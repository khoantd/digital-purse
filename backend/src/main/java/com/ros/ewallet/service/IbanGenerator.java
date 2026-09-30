package com.ros.ewallet.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

import static com.ros.ewallet.common.Constants.IBAN_MAX;
import static com.ros.ewallet.common.Constants.IBAN_MODULUS;

/**
 * Generates ISO 13616-compliant Vietnam-shaped IBANs for new wallets (demo).
 * Format: VN + 2 check digits + 6 bank BIN + 18 account (28 chars).
 * Bank BIN matches the demo VietQR NAPAS BIN ({@value #BANK_BIN}).
 */
@Component
public class IbanGenerator {

    public static final String COUNTRY_CODE = "VN";
    /** Demo NAPAS BIN (Vietcombank) used for app-generated wallets / VietQR. */
    public static final String BANK_BIN = "970436";
    private static final int ACCOUNT_DIGITS = 18;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        String accountNumber = randomDigits(ACCOUNT_DIGITS);
        String bban = BANK_BIN + accountNumber;
        String checkDigits = calculateCheckDigits(COUNTRY_CODE, bban);
        return COUNTRY_CODE + checkDigits + bban;
    }

    /**
     * Account digits embedded in a generated VN IBAN (after country + check + bank BIN).
     */
    public String accountNumber(String iban) {
        if (iban == null || iban.length() < 10) {
            return "";
        }
        return iban.substring(10);
    }

    private String calculateCheckDigits(String countryCode, String bban) {
        String rearranged = bban + countryCode + "00";
        long total = 0;
        for (int i = 0; i < rearranged.length(); i++) {
            int charValue = Character.getNumericValue(rearranged.charAt(i));
            total = (charValue > 9 ? total * 100 : total * 10) + charValue;
            if (total > IBAN_MAX) {
                total = total % IBAN_MODULUS;
            }
        }
        int check = (int) (98 - (total % IBAN_MODULUS));
        return String.format("%02d", check);
    }

    private String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
