package com.laptophub.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.auth.dto.ChangePasswordRequest;
import com.laptophub.auth.dto.LoginRequest;
import com.laptophub.auth.dto.RegisterRequest;
import com.laptophub.user.User;
import com.laptophub.user.UserRepository;
import com.laptophub.user.UserRole;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Ma trận regression capstone cho TOÀN BỘ luồng Auth (login/refresh/logout/
// đổi mật khẩu) đi qua nhiều endpoint trong 1 kịch bản — cùng tinh thần
// SecurityCoreIntegrationMatrixTest (ASU-15) nhưng ở tầng HTTP thật cho
// Auth. Không lặp lại các test đã có sẵn ở AuthControllerLoginTest/
// RefreshTest/LogoutTest/ChangePasswordTest (mỗi lỗi 400/401/403 riêng lẻ đã
// được test kỹ ở đó) — file này chỉ khóa các hành vi XUYÊN endpoint mà không
// test đơn lẻ nào chứng minh được một mình:
// 1) Luồng đầy đủ register->login->refresh->logout->refresh(fail).
// 2) Single-session: login lần 2 làm session lần 1 chết thật (không chỉ
//    đếm số dòng active trong DB như AuthControllerLoginTest đã làm, mà gọi
//    thật /auth/refresh bằng cookie phiên 1 để chứng minh nó bị từ chối).
// 3) Đổi mật khẩu: access token cũ vẫn dùng được ở endpoint khác (không bị
//    blacklist) trong khi refresh token cũ thì không — đúng quyết định ở
//    AUTH_SECURITY_USER_CONTRACT.md mục 6.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowRegressionMatrixTest {

    private static final Pattern REFRESH_COOKIE_VALUE = Pattern.compile("refreshToken=([^;]*)");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private record Session(String accessToken, String refreshCookie) {
    }

    private void commitAndStartNewTransaction() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
    }

    private Session login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
        Matcher matcher = REFRESH_COOKIE_VALUE.matcher(result.getResponse().getHeader("Set-Cookie"));
        matcher.find();
        return new Session(accessToken, matcher.group(1));
    }

    private void assertRefreshRejected(String refreshCookie) throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", refreshCookie)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void fullLifecycle_register_login_refresh_logout_thenRefreshRejected() throws Exception {
        String email = "flow-full-lifecycle@example.com";
        try {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new RegisterRequest(email, "Sup3rSecret!", "Flow Test", null))))
                    .andExpect(status().isCreated());
            commitAndStartNewTransaction();

            Session session = login(email, "Sup3rSecret!");
            commitAndStartNewTransaction();

            MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie("refreshToken", session.refreshCookie())))
                    .andExpect(status().isOk())
                    .andReturn();
            Matcher matcher = REFRESH_COOKIE_VALUE.matcher(refreshResult.getResponse().getHeader("Set-Cookie"));
            matcher.find();
            String rotatedRefreshCookie = matcher.group(1);
            String newAccessToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                    .path("data").path("accessToken").asText();
            commitAndStartNewTransaction();

            mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + newAccessToken))
                    .andExpect(status().isNoContent());
            commitAndStartNewTransaction();

            assertRefreshRejected(rotatedRefreshCookie);
            commitAndStartNewTransaction();
        } finally {
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
    void secondLogin_invalidatesFirstSession_firstSessionRefreshRejectedByRealRequest() throws Exception {
        String email = "flow-single-session@example.com";
        try {
            userRepository.saveAndFlush(
                    User.create(email, passwordEncoder.encode("Sup3rSecret!"), "Name", null, UserRole.CUSTOMER));
            commitAndStartNewTransaction();

            Session firstSession = login(email, "Sup3rSecret!");
            commitAndStartNewTransaction();

            Session secondSession = login(email, "Sup3rSecret!");
            commitAndStartNewTransaction();

            // Session 1 phải chết thật — không chỉ là DB có 1 dòng active
            // (đã kiểm ở AuthControllerLoginTest), mà cookie thật của session
            // 1 phải bị /auth/refresh từ chối.
            assertRefreshRejected(firstSession.refreshCookie());
            commitAndStartNewTransaction();

            // Session 2 vẫn hoạt động bình thường.
            mockMvc.perform(post("/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie("refreshToken", secondSession.refreshCookie())))
                    .andExpect(status().isOk());
            commitAndStartNewTransaction();
        } finally {
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
    void changePassword_revokesRefreshSession_butOldAccessTokenStillWorksOnOtherEndpoints() throws Exception {
        String email = "flow-change-pw-access-token@example.com";
        try {
            userRepository.saveAndFlush(
                    User.create(email, passwordEncoder.encode("OldSecret1!"), "Name", null, UserRole.CUSTOMER));
            commitAndStartNewTransaction();

            Session session = login(email, "OldSecret1!");
            commitAndStartNewTransaction();

            mockMvc.perform(post("/auth/change-password")
                            .header("Authorization", "Bearer " + session.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ChangePasswordRequest("OldSecret1!", "NewSecret2!"))))
                    .andExpect(status().isOk());
            commitAndStartNewTransaction();

            // Refresh token cũ chết theo (giống logout) — đúng
            // AUTH_SECURITY_USER_CONTRACT.md mục 6.
            assertRefreshRejected(session.refreshCookie());
            commitAndStartNewTransaction();

            // Access token cũ (phát hành TRƯỚC khi đổi mật khẩu) vẫn được
            // chấp nhận ở một endpoint xác thực khác — chứng minh access
            // token không bị blacklist, đúng quyết định "hết hạn tự nhiên
            // theo TTL 15 phút, không thu hồi ngay".
            mockMvc.perform(post("/auth/logout-all").header("Authorization", "Bearer " + session.accessToken()))
                    .andExpect(status().isNoContent());
            commitAndStartNewTransaction();
        } finally {
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
}
