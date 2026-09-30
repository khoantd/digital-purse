package com.ros.ewallet.common;

import lombok.experimental.UtilityClass;

/**
 * Utility class to hold constant values used throughout the application.
 */
@UtilityClass
public class Constants {

    public static final String TRACE = "trace";
    public static final String DATE_FORMAT = "dd.MM.yyyy";
    public static final String DATE_TIME_FORMAT = "dd.MM.yyyy HH:mm:ss";
    public static final int IBAN_MIN_SIZE = 15;
    public static final int IBAN_MAX_SIZE = 34;
    public static final long IBAN_MAX = 999999999;
    public static final long IBAN_MODULUS = 97;
    /** Minimum password length for signup (SEC-13). */
    public static final int PASSWORD_MIN_LENGTH = 12;
    public static final int PASSWORD_MAX_LENGTH = 100;
    /** Default (and currently only) wallet currency. */
    public static final String CURRENCY_VND = "VND";

    /** Transaction type catalog (see Flyway V5 / V12 / V15). */
    public static final long TYPE_TRANSFER = 1L;
    public static final long TYPE_WITHDRAW = 2L;
    public static final long TYPE_TOP_UP = 3L;
    public static final long TYPE_REVERSE = 4L;
}
