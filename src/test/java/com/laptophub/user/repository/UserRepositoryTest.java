package com.laptophub.user.repository;

import com.laptophub.config.JpaAuditingConfig;
import com.laptophub.user.EmailNormalizer;
import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Không dùng profile "test": JpaAuditingConfig bị tắt ở profile đó
// (@Profile("!test")) nên created_at/updated_at sẽ null và vi phạm NOT NULL.
// @DataJpaTest chỉ quét @Entity/@Repository, không tự nạp @Configuration
// thường — phải @Import JpaAuditingConfig để @CreatedDate/@LastModifiedDate
// thật sự chạy khi saveAndFlush.
// Chạy trên MySQL thật (Replace.NONE) vì project chưa có embedded test DB và
// migration dùng cú pháp riêng của MySQL (CHECK constraint, datetime(6)).
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_returnsUser_whenQueriedWithNormalizedEmail() {
        String email = EmailNormalizer.normalize(" Find@Example.COM ");
        userRepository.saveAndFlush(User.create(email, "hash", "Find Me", null, UserRole.CUSTOMER));

        Optional<User> found = userRepository.findByEmail(email);

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("find@example.com");
    }

    @Test
    void findByEmail_returnsEmpty_whenNoMatch() {
        assertThat(userRepository.findByEmail("missing@example.com")).isEmpty();
    }

    @Test
    void existsByEmail_reflectsNormalizedEmail() {
        String email = EmailNormalizer.normalize("Exists@Example.COM");
        userRepository.saveAndFlush(User.create(email, "hash", "Exists", null, UserRole.CUSTOMER));

        assertThat(userRepository.existsByEmail(email)).isTrue();
        assertThat(userRepository.existsByEmail("someone-else@example.com")).isFalse();
    }

    @Test
    void findById_returnsUser_afterSave() {
        User saved = userRepository.saveAndFlush(
                User.create(EmailNormalizer.normalize("byid@example.com"), "hash", "By Id", null, UserRole.ADMIN));

        Optional<User> found = userRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void savingDuplicateNormalizedEmail_violatesUniqueConstraint() {
        String email = EmailNormalizer.normalize("Dup@Example.COM");
        userRepository.saveAndFlush(User.create(email, "hash", "First", null, UserRole.CUSTOMER));

        User duplicate = User.create(email, "hash2", "Second", null, UserRole.CUSTOMER);

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
