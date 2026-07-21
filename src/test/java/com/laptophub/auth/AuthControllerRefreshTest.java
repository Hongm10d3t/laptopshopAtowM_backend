package com.laptophub.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.auth.dto.LoginRequest;
import com.laptophub.auth.entity.RefreshToken;
import com.laptophub.auth.entity.RevokeReason;
import com.laptophub.auth.repository.RefreshTokenRepository;
import com.laptophub.auth.token.RefreshTokenGenerator;
import com.laptophub.auth.token.RefreshTokenHasher;
import com.laptophub.auth.token.RefreshTokenProperties;
import com.laptophub.user.entity.User;
import com.laptophub.user.repository.UserRepository;
import com.laptophub.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerRefreshTest {

    private static final Pattern REFRESH_COOKIE_VALUE = Pattern.compile("refreshToken=([^;]+)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenProperties refreshTokenProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Kết thúc transaction hiện tại bằng COMMIT thật rồi mở transaction mới —
    // mô phỏng đúng 2 request HTTP độc lập (mỗi request tự commit/rollback
    // transaction của riêng nó), thay vì để @Transactional ở class mặc định
    // gộp toàn bộ test method vào 1 transaction duy nhất chỉ rollback ở cuối.
    // Cách gộp mặc định từng che giấu mất 1 bug thật: revokeActiveByFamilyId()
    // chạy xong nhưng bị rollback theo AppException ném ra ngay sau đó
    // (RefreshService trước khi có noRollbackFor=AppException.class) — chỉ
    // lộ ra khi test qua request HTTP thật, không lộ qua MockMvc dùng chung
    // transaction. Vì test này commit thật, phải tự dọn dữ liệu ở cuối.
    private void commitAndStartNewTransaction() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
    }

    private String extractRefreshCookie(String setCookieHeader) {
        Matcher matcher = REFRESH_COOKIE_VALUE.matcher(setCookieHeader);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String loginAndExtractCookie(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        return extractRefreshCookie(result.getResponse().getHeader("Set-Cookie"));
    }

    @Test
    void refresh_validCookie_returnsNewAccessTokenAndRotatesRefreshToken() throws Exception {
        userRepository.saveAndFlush(
                User.create("refresh-ok@example.com", passwordEncoder.encode("Sup3rSecret!"), "Name", null,
                        UserRole.CUSTOMER));
        String oldRawToken = loginAndExtractCookie("refresh-ok@example.com", "Sup3rSecret!");

        MvcResult result = mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", oldRawToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        Matcher matcher = REFRESH_COOKIE_VALUE.matcher(setCookie);
        assertThat(matcher.find()).isTrue();
        String newRawToken = matcher.group(1);
        assertThat(newRawToken).isNotEqualTo(oldRawToken);

        User user = userRepository.findByEmail("refresh-ok@example.com").orElseThrow();
        List<RefreshToken> active = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId());
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getTokenHash()).isEqualTo(RefreshTokenHasher.hash(newRawToken));

        RefreshToken oldTokenRow = refreshTokenRepository.findByTokenHash(RefreshTokenHasher.hash(oldRawToken))
                .orElseThrow();
        assertThat(oldTokenRow.getRevokedAt()).isNotNull();
        assertThat(oldTokenRow.getRevokeReason()).isEqualTo(RevokeReason.ROTATED);
        assertThat(oldTokenRow.getReplacedByTokenId()).isEqualTo(active.get(0).getId());
    }

    @Test
    void refresh_noCookie_returns401() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void refresh_garbageToken_returns401() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "not-a-real-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void refresh_reusedOldToken_returns401_andActuallyCommitsFamilyRevocation() throws Exception {
        String email = "refresh-reuse@example.com";
        try {
            userRepository.saveAndFlush(
                    User.create(email, passwordEncoder.encode("Sup3rSecret!"), "Name", null, UserRole.CUSTOMER));
            commitAndStartNewTransaction();

            String oldRawToken = loginAndExtractCookie(email, "Sup3rSecret!");
            commitAndStartNewTransaction();

            // Lần 1: hợp lệ, rotate bình thường -> commit thật, đúng như 1
            // request HTTP thật sự kết thúc transaction của nó.
            MvcResult firstRefresh = mockMvc.perform(post("/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie("refreshToken", oldRawToken)))
                    .andExpect(status().isOk())
                    .andReturn();
            String rotatedRawToken = extractRefreshCookie(firstRefresh.getResponse().getHeader("Set-Cookie"));
            commitAndStartNewTransaction();

            // Lần 2: dùng lại đúng token cũ (đã bị rotate ở lần 1) -> reuse
            // detection, phải revoke CẢ family và commit thật.
            mockMvc.perform(post("/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie("refreshToken", oldRawToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
            commitAndStartNewTransaction();

            // Lần 3: token vừa rotate ra ở lần 1 (chưa từng bị lộ, tự nó vẫn
            // "hợp lệ") cũng phải bị từ chối — chứng minh việc revoke cả
            // family ở lần 2 đã thực sự commit xuống DB, không bị rollback
            // ngược theo AppException của chính lần 2.
            mockMvc.perform(post("/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie("refreshToken", rotatedRawToken)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
        } finally {
            // Đã commit thật nhiều lần ở trên -> rollback tự động cuối test
            // (từ @Transactional ở class) không dọn được nữa, phải tự xóa.
            if (!TestTransaction.isActive()) {
                TestTransaction.start();
            }
            jdbcTemplate.update(
                    "DELETE FROM refresh_tokens WHERE user_id = (SELECT id FROM users WHERE email = ?)", email);
            jdbcTemplate.update("DELETE FROM users WHERE email = ?", email);
            TestTransaction.flagForCommit();
            TestTransaction.end();
        }
    }

    @Test
    void refresh_expiredToken_returns401() throws Exception {
        User user = userRepository.saveAndFlush(
                User.create("refresh-expired@example.com", passwordEncoder.encode("Sup3rSecret!"), "Name", null,
                        UserRole.CUSTOMER));

        String rawToken = RefreshTokenGenerator.generate();
        Clock pastClock = Clock.fixed(Instant.now().minus(Duration.ofDays(40)), ZoneOffset.UTC);
        Instant issuedAt = pastClock.instant();
        refreshTokenRepository.saveAndFlush(RefreshToken.create(
                user.getId(), RefreshTokenHasher.hash(rawToken), "family-expired",
                issuedAt.plus(refreshTokenProperties.ttl()), issuedAt));

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", rawToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }
}
