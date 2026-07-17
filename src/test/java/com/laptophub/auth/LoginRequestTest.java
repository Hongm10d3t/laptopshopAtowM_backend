package com.laptophub.auth;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void validRequest_hasNoViolations() {
        assertThat(validator.validate(new LoginRequest("user@example.com", "any-password"))).isEmpty();
    }

    @Test
    void blankEmail_isRejected() {
        assertThat(validator.validate(new LoginRequest("", "any-password"))).isNotEmpty();
    }

    @Test
    void malformedEmail_isRejected() {
        assertThat(validator.validate(new LoginRequest("not-an-email", "any-password"))).isNotEmpty();
    }

    @Test
    void blankPassword_isRejected() {
        assertThat(validator.validate(new LoginRequest("user@example.com", ""))).isNotEmpty();
    }

    @Test
    void shortPassword_isNotRejected_loginDoesNotEnforcePasswordPolicy() {
        // Login chỉ kiểm tra khớp mật khẩu đã lưu, không ép policy hiện tại
        // (mật khẩu tạo từ trước vẫn phải đăng nhập được dù policy đổi sau).
        assertThat(validator.validate(new LoginRequest("user@example.com", "a"))).isEmpty();
    }
}
