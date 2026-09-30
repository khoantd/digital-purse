package com.ros.ewallet.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LedgerAccountCode {
    WALLET("WALLET"),
    SYSTEM_FLOAT("SYSTEM_FLOAT");

    private final String code;
}
