package com.ros.ewallet.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VietQrGeneratorTest {

    private final VietQrGenerator vietQrGenerator = new VietQrGenerator();

    @Test
    void buildStatic_containsNapasGuidAndVndCurrency() {
        String payload = vietQrGenerator.buildStatic("970436", "012345678901234567", "My Wallet");

        assertTrue(payload.startsWith("000201"));
        assertTrue(payload.contains("A000000727"));
        assertTrue(payload.contains("970436"));
        assertTrue(payload.contains("012345678901234567"));
        assertTrue(payload.contains("5303704")); // VND
        assertTrue(payload.contains("5802VN"));
        assertTrue(payload.contains("6304"));
        assertTrue(payload.length() > 50);
    }

    @Test
    void buildStatic_endsWithFourCharCrc() {
        String payload = vietQrGenerator.buildStatic("970436", "012345678901234567", null);
        String crc = payload.substring(payload.length() - 4);
        assertEquals(4, crc.length());
        assertTrue(crc.matches("[0-9A-F]{4}"));
    }

    @Test
    void buildStatic_isDeterministicForSameInputs() {
        String a = vietQrGenerator.buildStatic("970436", "111", "A");
        String b = vietQrGenerator.buildStatic("970436", "111", "A");
        assertEquals(a, b);
    }
}
