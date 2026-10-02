package com.kindererp.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Stores money as an INTEGER number of thousandths (12.5 is stored as 12500), never as a
 * floating-point number. Three decimals cover every target currency (TND has 3, DZD/MAD/EUR have 2).
 * Every BigDecimal attribute in the model is a money amount.
 */
@Converter(autoApply = true)
public class MoneyConverter implements AttributeConverter<BigDecimal, Long> {

    public static final int SCALE = 3;

    @Override
    public Long convertToDatabaseColumn(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP).unscaledValue().longValueExact();
    }

    @Override
    public BigDecimal convertToEntityAttribute(Long value) {
        return value == null ? null : BigDecimal.valueOf(value, SCALE);
    }
}
