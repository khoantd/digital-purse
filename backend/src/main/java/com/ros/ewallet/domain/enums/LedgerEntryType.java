package com.ros.ewallet.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LedgerEntryType {
    DEBIT("DEBIT"),
    CREDIT("CREDIT");

    private final String code;
}
