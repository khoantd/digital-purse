package com.ros.ewallet.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rejects passwords that appear on a common/breached password denylist (SEC-13).
 */
@Documented
@Constraint(validatedBy = NotCommonPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotCommonPassword {

    String message() default "{validation.user.password.common}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
