package com.laptophub.user.service;

import com.laptophub.common.ErrorCode;
import com.laptophub.common.exception.AppException;
import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import com.laptophub.user.entity.UserStatus;
import com.laptophub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceProfileTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    private User userWithId(long id) {
        User user = User.create("user" + id + "@example.com", "hash", "Name", null, UserRole.CUSTOMER);
        user.setId(id);
        return user;
    }

    @Test
    void updateProfile_updatesFullNameAndPhone_whenUserExists() {
        User user = userWithId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User updated = userService.updateProfile(1L, "New Name", "0911111111");

        assertThat(updated.getFullName()).isEqualTo("New Name");
        assertThat(updated.getPhone()).isEqualTo("0911111111");
    }

    @Test
    void updateProfile_throwsUnauthenticated_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(99L, "New Name", null))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    void search_delegatesToRepository() {
        Pageable pageable = Pageable.ofSize(20);
        Page<User> page = new PageImpl<>(java.util.List.of(userWithId(1L)));
        when(userRepository.search(UserRole.CUSTOMER, UserStatus.ACTIVE, "name", pageable)).thenReturn(page);

        Page<User> result = userService.search(UserRole.CUSTOMER, UserStatus.ACTIVE, "name", pageable);

        assertThat(result).isEqualTo(page);
    }

    @Test
    void blockUser_blocksTargetUser_whenActingAdminDiffersFromTarget() {
        User target = userWithId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        User blocked = userService.blockUser(1L, 2L);

        assertThat(blocked.getStatus()).isEqualTo(UserStatus.BLOCKED);
    }

    @Test
    void blockUser_throwsValidationError_whenAdminTargetsSelf() {
        assertThatThrownBy(() -> userService.blockUser(1L, 1L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

        verify(userRepository, org.mockito.Mockito.never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blockUser_throwsResourceNotFound_whenTargetMissing() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.blockUser(1L, 2L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void activateUser_activatesTargetUser() {
        User target = userWithId(3L);
        target.block();
        when(userRepository.findById(3L)).thenReturn(Optional.of(target));

        User activated = userService.activateUser(3L);

        assertThat(activated.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void activateUser_throwsResourceNotFound_whenTargetMissing() {
        when(userRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.activateUser(3L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }
}
