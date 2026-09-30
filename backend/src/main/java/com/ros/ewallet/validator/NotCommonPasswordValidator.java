package com.ros.ewallet.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;

/**
 * Lightweight breach-list check against well-known weak passwords (SEC-13).
 * Does not call external APIs; keeps signup offline and deterministic for tests.
 */
public class NotCommonPasswordValidator implements ConstraintValidator<NotCommonPassword, String> {

    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password",
            "password123",
            "password1234",
            "123456789012",
            "qwertyuiopas",
            "letmein12345",
            "adminadmin12",
            "welcome12345",
            "iloveyou1234",
            "changeme1234",
            "abc123456789",
            "monkey123456"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return !COMMON_PASSWORDS.contains(value.toLowerCase(Locale.ROOT));
    }
}
