package com.ros.ewallet.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Status {

    PENDING("Pending"),
    SUCCESS("Success"),
    ERROR("Error");

    private String label;
}
