package com.ros.ewallet.service;

import org.springframework.stereotype.Component;

/**
 * Builds static VietQR (EMVCo / NAPAS) payloads for demo receive flows.
 * Not a licensed payment processor — payload is for in-app QR display only.
 */
@Component
public class VietQrGenerator {

    private static final String NAPAS_GUID = "A000000727";
    private static final String SERVICE_CODE = "QRIBFTTA";
    private static final String CURRENCY_VND = "704";
    private static final String COUNTRY_VN = "VN";

    public String buildStatic(String bankBin, String accountNumber, String consumerName) {
        String beneficiary = tlv("00", bankBin) + tlv("01", accountNumber);
        String merchantAccount = tlv("00", NAPAS_GUID)
                + tlv("01", beneficiary)
                + tlv("02", SERVICE_CODE);

        StringBuilder payload = new StringBuilder();
        payload.append(tlv("00", "01"));
        payload.append(tlv("01", "11"));
        payload.append(tlv("38", merchantAccount));
        payload.append(tlv("53", CURRENCY_VND));
        payload.append(tlv("58", COUNTRY_VN));
        if (consumerName != null && !consumerName.isBlank()) {
            String name = consumerName.length() > 25 ? consumerName.substring(0, 25) : consumerName;
            payload.append(tlv("59", name));
        }
        payload.append("6304");
        String crc = crc16(payload.toString());
        payload.append(crc);
        return payload.toString();
    }

    private static String tlv(String id, String value) {
        return id + String.format("%02d", value.length()) + value;
    }

    /**
     * CRC-16/CCITT-FALSE (poly 0x1021, init 0xFFFF) as used by EMVCo QR.
     */
    static String crc16(String data) {
        int crc = 0xFFFF;
        for (int i = 0; i < data.length(); i++) {
            crc ^= (data.charAt(i) << 8);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc <<= 1;
                }
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}
