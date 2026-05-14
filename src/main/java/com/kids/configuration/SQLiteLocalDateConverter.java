package com.kids.configuration;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Converter(autoApply = true)
public class SQLiteLocalDateConverter implements AttributeConverter<LocalDate, String> {

    // This handles both YYYY-MM-DD and YYYY-MM-DD HH:mm:ss.SSS formats
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("[yyyy-MM-dd][yyyy-MM-dd HH:mm:ss.SSS]");

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        return (attribute == null) ? null : attribute.toString();
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        // If SQLite gives us the long timestamp, we take just the date part
        if (dbData.length() > 10) {
            dbData = dbData.substring(0, 10);
        }
        return LocalDate.parse(dbData);
    }
}