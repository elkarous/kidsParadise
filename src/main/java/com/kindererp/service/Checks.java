package com.kindererp.service;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Input rules shared by the services (authoritative) and the forms (immediate feedback). */
public final class Checks {

    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()./-]{6,20}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private Checks() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Digits with optional leading +, spaces, dots, dashes or parentheses; at least 6 digits. */
    public static boolean isValidPhone(String value) {
        return value != null && PHONE.matcher(value.trim()).matches()
                && value.chars().filter(Character::isDigit).count() >= 6;
    }

    public static boolean isValidEmail(String value) {
        return value != null && EMAIL.matcher(value.trim()).matches();
    }

    public static boolean isNonNegative(BigDecimal value) {
        return value != null && value.signum() >= 0;
    }

    /** Throws a {@link BusinessException} with the given key when the condition is false. */
    public static void require(boolean condition, String messageKey, Object... args) {
        if (!condition) {
            throw new BusinessException(messageKey, args);
        }
    }

    public static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
