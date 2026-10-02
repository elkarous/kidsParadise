package com.kindererp.util;

import com.kindererp.model.WorkingMonth;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;

/** Locale-aware display of money, dates and months; tolerant parsing of user-typed amounts. */
public final class Formats {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private Formats() {
    }

    /** Amount with the school currency, e.g. "1 250,500 DT" in French. */
    public static String money(BigDecimal amount) {
        return amount(amount) + " " + AppState.settings().getCurrencySymbol();
    }

    /** Amount without currency, with the school's number of decimals. */
    public static String amount(BigDecimal amount) {
        int decimals = AppState.settings().getCurrencyDecimals();
        NumberFormat format = NumberFormat.getNumberInstance(I18n.locale());
        format.setMinimumFractionDigits(decimals);
        format.setMaximumFractionDigits(decimals);
        return format.format(amount == null ? BigDecimal.ZERO : amount.setScale(decimals, RoundingMode.HALF_UP));
    }

    /** Plain editable form of an amount ("1250.5"), for text fields. */
    public static String editableAmount(BigDecimal amount) {
        return amount == null ? "" : amount.stripTrailingZeros().toPlainString();
    }

    /** Parses "1250", "1 250,5" or "1250.500"; returns null when the text is not a valid amount. */
    public static BigDecimal parseAmount(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim().replaceAll("[\\s\\u00A0\\u202F]", "").replace(',', '.');
        if (!normalized.matches("-?\\d+(\\.\\d{1,3})?")) {
            return null;
        }
        return new BigDecimal(normalized);
    }

    public static String date(LocalDate date) {
        return date == null ? "" : date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(I18n.locale()));
    }

    public static String time(LocalTime time) {
        return time == null ? "" : time.format(TIME);
    }

    public static LocalTime parseTime(String text) {
        if (text == null || !text.trim().matches("\\d{1,2}:\\d{2}")) {
            return null;
        }
        String[] parts = text.trim().split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return hour < 24 && minute < 60 ? LocalTime.of(hour, minute) : null;
    }

    public static String monthName(int month) {
        return Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, I18n.locale());
    }

    /** e.g. "septembre 2026" / "سبتمبر 2026", with a "(closed)" mark when the month is closed. */
    public static String month(WorkingMonth month) {
        if (month == null) {
            return "";
        }
        String label = monthName(month.getCalendarMonth()) + " " + month.getCalendarYear();
        return month.isClosed() ? label + " " + I18n.get("month.closedMark") : label;
    }
}
