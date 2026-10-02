package com.kindererp.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;

/** Stores dates as ISO text (yyyy-MM-dd), which SQLite date functions understand. */
@Converter(autoApply = true)
public class LocalDateConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate value) {
        return value == null ? null : value.toString();
    }

    @Override
    public LocalDate convertToEntityAttribute(String text) {
        return text == null || text.isBlank() ? null : LocalDate.parse(text.substring(0, 10));
    }
}
