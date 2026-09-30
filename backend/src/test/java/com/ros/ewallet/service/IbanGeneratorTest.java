package com.ros.ewallet.service;

import com.ros.ewallet.validator.IbanValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IbanGeneratorTest {

    private final IbanGenerator ibanGenerator = new IbanGenerator();
    private final IbanValidator ibanValidator = new IbanValidator();

    @Test
    void generate_returnsVietnamIbanOfExpectedLength() {
        String iban = ibanGenerator.generate();

        assertNotNull(iban);
        assertEquals(28, iban.length());
        assertTrue(iban.startsWith("VN"));
        assertTrue(iban.chars().allMatch(Character::isLetterOrDigit));
    }

    @Test
    void generate_usesDemoBankBin() {
        String iban = ibanGenerator.generate();
        // VN + check(2) + bank BIN (970436)
        assertEquals("970436", iban.substring(4, 10));
    }

    @Test
    void generate_passesMod97Validation() {
        for (int i = 0; i < 20; i++) {
            String iban = ibanGenerator.generate();
            assertTrue(ibanValidator.isValid(iban, null), () -> "invalid IBAN: " + iban);
        }
    }

    @Test
    void generate_producesDistinctValues() {
        String first = ibanGenerator.generate();
        String second = ibanGenerator.generate();
        assertNotEquals(first, second);
    }

    @Test
    void accountNumber_extractsDigitsAfterBankBin() {
        String iban = ibanGenerator.generate();
        String account = ibanGenerator.accountNumber(iban);
        assertEquals(18, account.length());
        assertTrue(account.chars().allMatch(Character::isDigit));
        assertEquals(iban.substring(10), account);
    }

    @Test
    void validator_allowsNullOrBlankIban() {
        assertTrue(ibanValidator.isValid(null, null));
        assertTrue(ibanValidator.isValid("", null));
        assertTrue(ibanValidator.isValid("   ", null));
    }
}
