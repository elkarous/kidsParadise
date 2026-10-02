package com.kindererp.model;

import com.kindererp.model.converter.MoneyConverter;
import com.kindererp.util.Formats;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyConverterTest {

    private final MoneyConverter converter = new MoneyConverter();

    @Test
    void storesThousandthsAsAnExactInteger() {
        assertThat(converter.convertToDatabaseColumn(new BigDecimal("12.5"))).isEqualTo(12_500L);
        assertThat(converter.convertToDatabaseColumn(new BigDecimal("0.001"))).isEqualTo(1L);
        assertThat(converter.convertToEntityAttribute(195_000L)).isEqualByComparingTo("195");
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void parsesAmountsTypedWithCommaOrDot() {
        assertThat(Formats.parseAmount("1 250,5")).isEqualByComparingTo("1250.5");
        assertThat(Formats.parseAmount("1250.500")).isEqualByComparingTo("1250.5");
        assertThat(Formats.parseAmount("abc")).isNull();
        assertThat(Formats.parseAmount("1.2345")).isNull();
        assertThat(Formats.parseAmount("")).isNull();
    }
}
