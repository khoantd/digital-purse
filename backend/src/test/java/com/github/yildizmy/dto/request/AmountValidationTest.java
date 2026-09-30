package com.github.yildizmy.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SEC-05: amount/balance must be strictly positive.
 */
class AmountValidationTest {

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
    void transactionAmount_rejectsNegative() {
        var request = validTransactionRequest();
        request.setAmount(new BigDecimal("-10.00"));

        Set<ConstraintViolation<TransactionRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
    }

    @Test
    void transactionAmount_rejectsZero() {
        var request = validTransactionRequest();
        request.setAmount(BigDecimal.ZERO);

        Set<ConstraintViolation<TransactionRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
    }

    @Test
    void transactionAmount_acceptsPositive() {
        var request = validTransactionRequest();
        request.setAmount(new BigDecimal("0.01"));

        Set<ConstraintViolation<TransactionRequest>> violations = validator.validate(request);

        assertFalse(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
    }

    @Test
    void walletBalance_rejectsNegative() {
        var request = validWalletRequest();
        request.setBalance(new BigDecimal("-1.00"));

        Set<ConstraintViolation<WalletRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("balance")));
    }

    @Test
    void walletBalance_rejectsZero() {
        var request = validWalletRequest();
        request.setBalance(BigDecimal.ZERO);

        Set<ConstraintViolation<WalletRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("balance")));
    }

    @Test
    void walletBalance_acceptsPositive() {
        var request = validWalletRequest();
        request.setBalance(new BigDecimal("100.00"));

        Set<ConstraintViolation<WalletRequest>> violations = validator.validate(request);

        assertFalse(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("balance")));
    }

    private TransactionRequest validTransactionRequest() {
        var request = new TransactionRequest();
        request.setAmount(new BigDecimal("10.00"));
        request.setFromWalletIban("TR330006100519786457841326");
        request.setToWalletIban("TR320010009999901234567890");
        request.setTypeId(1L);
        return request;
    }

    private WalletRequest validWalletRequest() {
        var request = new WalletRequest();
        request.setIban("TR330006100519786457841326");
        request.setName("Primary");
        request.setBalance(new BigDecimal("100.00"));
        return request;
    }
}
