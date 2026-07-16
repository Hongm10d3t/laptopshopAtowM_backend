package com.laptophub.user;

import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import com.laptophub.config.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 2 test đầu chạy trên MySQL thật (Replace.NONE) để chứng minh persistence
// và pre-check hoạt động đúng với ràng buộc DB thật. Test race condition
// dùng UserRepository mock riêng (không phải bean được @Autowired) vì tái
// hiện race thật bằng thread/DB thật sẽ phức tạp và không deterministic —
// mock cho phép ép chính xác kịch bản "existsByEmail nói chưa trùng nhưng
// saveAndFlush vẫn vi phạm UNIQUE constraint" mà không cần race thật.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class UserServiceCreateCustomerTest {

    @Autowired
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void createsActiveCustomer_withPassedThroughPasswordHash() {
        User created = userService.createCustomer(
                "new-customer@example.com", "already-hashed-value", "Nguyen Van A", "0900000000");

        assertThat(created.getId()).isNotNull();
        assertThat(created.getEmail()).isEqualTo("new-customer@example.com");
        assertThat(created.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(created.getPasswordHash()).isEqualTo("already-hashed-value");
    }

    @Test
    void duplicateEmail_preCheck_throwsEmailAlreadyExists_notRawDbException() {
        userService.createCustomer("dup-customer@example.com", "hash1", "Name", null);

        assertThatThrownBy(() ->
                userService.createCustomer("dup-customer@example.com", "hash2", "Name 2", null))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
    }

    @Test
    void raceOnUniqueConstraint_isMappedToEmailAlreadyExists_notLeakedAsDataIntegrityViolation() {
        UserRepository racyRepository = mock(UserRepository.class);
        when(racyRepository.existsByEmail("race@example.com")).thenReturn(false);
        when(racyRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key: uk_users_email"));
        UserService racyService = new UserService(racyRepository);

        assertThatThrownBy(() -> racyService.createCustomer("race@example.com", "hash", "Name", null))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
    }

    @Test
    void preCheckDuplicate_neverAttemptsInsert() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.existsByEmail("known@example.com")).thenReturn(true);
        UserService service = new UserService(repository);

        assertThatThrownBy(() -> service.createCustomer("known@example.com", "hash", "Name", null))
                .isInstanceOf(AppException.class);

        verify(repository, never()).saveAndFlush(any(User.class));
    }
}
