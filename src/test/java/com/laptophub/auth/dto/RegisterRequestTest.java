package com.laptophub.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestTest {

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

    private RegisterRequest validRequest() {
        return new RegisterRequest("user@example.com", "S3curePass!", "Nguyen Van A", "0901234567");
    }

    @Test
    void validRequest_hasNoViolations() {
        assertThat(validator.validate(validRequest())).isEmpty();
    }

    @Test
    void phoneIsOptional_nullPhoneHasNoViolation() {
        RegisterRequest request = new RegisterRequest("user@example.com", "S3curePass!", "Nguyen Van A", null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void blankEmail_isRejected() {
        RegisterRequest request = new RegisterRequest("", "S3curePass!", "Nguyen Van A", null);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void malformedEmail_isRejected() {
        RegisterRequest request = new RegisterRequest("not-an-email", "S3curePass!", "Nguyen Van A", null);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void blankPassword_isRejected() {
        RegisterRequest request = new RegisterRequest("user@example.com", "", "Nguyen Van A", null);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void tooShortPassword_isRejected() {
        RegisterRequest request = new RegisterRequest("user@example.com", "short1", "Nguyen Van A", null);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void tooLongPassword_isRejected() {
        String longPassword = "a".repeat(73);
        RegisterRequest request = new RegisterRequest("user@example.com", longPassword, "Nguyen Van A", null);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void passwordAtMaxLength_isAccepted() {
        String maxLengthPassword = "a".repeat(72);
        RegisterRequest request = new RegisterRequest("user@example.com", maxLengthPassword, "Nguyen Van A", null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void blankFullName_isRejected() {
        RegisterRequest request = new RegisterRequest("user@example.com", "S3curePass!", "", null);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void tooLongPhone_isRejected() {
        String longPhone = "1".repeat(21);
        RegisterRequest request = new RegisterRequest("user@example.com", "S3curePass!", "Nguyen Van A", longPhone);

        assertThat(validator.validate(request)).isNotEmpty();
    }

    @Test
    void validationMessages_areNotBlank_forEveryViolation() {
        RegisterRequest invalid = new RegisterRequest("", "", "", null);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(invalid);

        assertThat(violations).isNotEmpty();
        assertThat(violations).allSatisfy(v -> assertThat(v.getMessage()).isNotBlank());
    }

    @Test
    void registerResponse_hasNoPasswordOrHashComponent() {
        Stream<String> componentNames = Stream.of(RegisterResponse.class.getRecordComponents())
                .map(RecordComponent::getName);

        assertThat(componentNames).noneMatch(name ->
                name.toLowerCase().contains("password") || name.toLowerCase().contains("hash"));
    }
}
