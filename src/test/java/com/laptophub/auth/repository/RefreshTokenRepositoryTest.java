package com.laptophub.auth.repository;

import com.laptophub.auth.entity.RefreshToken;
import com.laptophub.auth.entity.RevokeReason;
import com.laptophub.config.JpaAuditingConfig;
import com.laptophub.user.entity.User;
import com.laptophub.user.repository.UserRepository;
import com.laptophub.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// Chạy trên MySQL thật (Replace.NONE), giống UserRepositoryTest — cần @Import
// JpaAuditingConfig vì test này phải tạo User thật (FK bắt buộc của
// refresh_tokens) và User dùng @CreatedDate/@LastModifiedDate qua BaseEntity.
// RefreshToken tự nó không dùng auditing nên không ảnh hưởng.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String email) {
        return userRepository.saveAndFlush(User.create(email, "hash", "Name", null, UserRole.CUSTOMER));
    }

    private Instant future() {
        return Instant.now().plus(Duration.ofDays(30));
    }

    @Test
    void findByTokenHash_returnsToken_whenExists() {
        User user = persistUser("find-hash@example.com");
        refreshTokenRepository.saveAndFlush(
                RefreshToken.create(user.getId(), "hash-find-1", "family-a", future(), Instant.now()));

        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash("hash-find-1");

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(user.getId());
    }

    @Test
    void findByTokenHash_returnsEmpty_whenNotExists() {
        assertThat(refreshTokenRepository.findByTokenHash("does-not-exist")).isEmpty();
    }

    @Test
    void findByUserIdAndRevokedAtIsNull_returnsOnlyActiveTokensForThatUser() {
        User user = persistUser("find-active@example.com");
        User otherUser = persistUser("other-user@example.com");

        refreshTokenRepository.saveAndFlush(
                RefreshToken.create(user.getId(), "hash-active-1", "family-b", future(), Instant.now()));
        refreshTokenRepository.saveAndFlush(
                RefreshToken.create(otherUser.getId(), "hash-other-user", "family-c", future(), Instant.now()));

        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId());

        assertThat(activeTokens).extracting(RefreshToken::getTokenHash).containsExactly("hash-active-1");
    }

    @Test
    void revokeActiveByUserId_revokesOnlyActiveTokensForThatUser_withAccurateRowCount() {
        User user = persistUser("revoke-user@example.com");
        User otherUser = persistUser("revoke-other@example.com");

        refreshTokenRepository.saveAndFlush(
                RefreshToken.create(user.getId(), "hash-ru-1", "family-1", future(), Instant.now()));
        refreshTokenRepository.saveAndFlush(
                RefreshToken.create(user.getId(), "hash-ru-2", "family-2", future(), Instant.now()));
        refreshTokenRepository.saveAndFlush(
                RefreshToken.create(otherUser.getId(), "hash-ru-other", "family-3", future(), Instant.now()));

        int updated = refreshTokenRepository.revokeActiveByUserId(user.getId(), Instant.now(), RevokeReason.LOGOUT);

        assertThat(updated).isEqualTo(2);
        assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId())).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash("hash-ru-other").orElseThrow().getRevokedAt()).isNull();
    }

    @Test
    void revokeActiveByUserId_excludesAlreadyRevokedTokens_secondCallReturnsZero() {
        User user = persistUser("revoke-twice@example.com");
        refreshTokenRepository.saveAndFlush(
                RefreshToken.create(user.getId(), "hash-rt-1", "family-1", future(), Instant.now()));

        int firstRevoke = refreshTokenRepository.revokeActiveByUserId(user.getId(), Instant.now(), RevokeReason.LOGOUT);
        assertThat(firstRevoke).isEqualTo(1);

        int secondRevoke = refreshTokenRepository.revokeActiveByUserId(user.getId(), Instant.now(), RevokeReason.LOGOUT);
        assertThat(secondRevoke).isEqualTo(0);
    }

    @Test
    void revokeActiveByFamilyId_revokesOnlyThatFamily_withAccurateRowCount() {
        User user = persistUser("revoke-family@example.com");

        refreshTokenRepository.saveAndFlush(
                RefreshToken.create(user.getId(), "hash-fam-1", "family-x", future(), Instant.now()));
        refreshTokenRepository.saveAndFlush(
                RefreshToken.create(user.getId(), "hash-fam-2", "family-x", future(), Instant.now()));
        refreshTokenRepository.saveAndFlush(
                RefreshToken.create(user.getId(), "hash-fam-other", "family-y", future(), Instant.now()));

        int updated = refreshTokenRepository.revokeActiveByFamilyId(
                "family-x", Instant.now(), RevokeReason.REUSE_DETECTED);

        assertThat(updated).isEqualTo(2);
        assertThat(refreshTokenRepository.findByTokenHash("hash-fam-other").orElseThrow().getRevokedAt()).isNull();
    }

    @Test
    void revokeActiveByUserId_clearsPersistenceContext_soSubsequentReadsSeeFreshData() {
        User user = persistUser("clear-test@example.com");
        refreshTokenRepository.saveAndFlush(
                RefreshToken.create(user.getId(), "hash-clear-test", "family-1", future(), Instant.now()));

        // Load vào persistence context TRƯỚC bulk update.
        RefreshToken loadedBeforeRevoke = refreshTokenRepository.findByTokenHash("hash-clear-test").orElseThrow();
        assertThat(loadedBeforeRevoke.getRevokedAt()).isNull();

        int updated = refreshTokenRepository.revokeActiveByUserId(user.getId(), Instant.now(), RevokeReason.LOGOUT);
        assertThat(updated).isEqualTo(1);

        // Nếu thiếu clearAutomatically, persistence context sẽ trả lại đúng
        // object cũ (revokedAt null) từ lần load trước thay vì đọc lại DB.
        RefreshToken loadedAfterRevoke = refreshTokenRepository.findByTokenHash("hash-clear-test").orElseThrow();
        assertThat(loadedAfterRevoke.getRevokedAt()).isNotNull();
        assertThat(loadedAfterRevoke.getRevokeReason()).isEqualTo(RevokeReason.LOGOUT);
    }
}
