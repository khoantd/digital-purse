package com.ros.ewallet.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SEC-13: signup passwords must be at least 12 characters and not on the common-password list.
 */
class PasswordPolicyTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        factory.close();
    }

    @Test
    void signup_rejectsShortPassword() {
        SignupRequest request = validSignup();
        request.setPassword("short1");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void signup_rejectsCommonPassword() {
        SignupRequest request = validSignup();
        request.setPassword("password1234");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("password")
                        && v.getMessage().toLowerCase().contains("common")));
    }

    @Test
    void signup_acceptsStrongPasswordWithSpaces() {
        SignupRequest request = validSignup();
        request.setPassword(" my Strong!Pass ");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertFalse(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    private SignupRequest validSignup() {
        return new SignupRequest(
                null,
                "Alice",
                "Smith",
                "alicesmith",
                "alice@example.com",
                "my Strong!Pass"
        );
    }
}
