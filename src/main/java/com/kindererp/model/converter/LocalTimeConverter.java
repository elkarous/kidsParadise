package com.kindererp.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Stores times as "HH:mm" text. */
@Converter(autoApply = true)
public class LocalTimeConverter implements AttributeConverter<LocalTime, String> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public String convertToDatabaseColumn(LocalTime value) {
        return value == null ? null : value.format(FORMAT);
    }

    @Override
    public LocalTime convertToEntityAttribute(String text) {
        return text == null || text.isBlank() ? null : LocalTime.parse(text, FORMAT);
    }
}
