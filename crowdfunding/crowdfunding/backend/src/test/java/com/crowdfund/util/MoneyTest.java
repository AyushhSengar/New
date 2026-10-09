package com.crowdfund.util;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.crowdfund.exception.AppException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    @DisplayName("parsePositive accepts valid positive amounts with up to 2 decimal places")
    void testParsePositiveValid() {
        BigDecimal result = Money.parsePositive(new BigDecimal("100.50"), "Amount");
        assertEquals(new BigDecimal("100.50"), result);

        BigDecimal whole = Money.parsePositive(new BigDecimal("500"), "Amount");
        assertEquals(new BigDecimal("500.00"), whole);
    }

    @Test
    @DisplayName("parsePositive rejects null, zero, negative and values with >2 decimal places")
    void testParsePositiveInvalid() {
        AppException exNull = assertThrows(AppException.class,
                () -> Money.parsePositive(null, "Target"));
        assertEquals(HttpStatus.BAD_REQUEST, exNull.getStatus());
        assertEquals("Target is required.", exNull.getMessage());

        AppException exZero = assertThrows(AppException.class,
                () -> Money.parsePositive(BigDecimal.ZERO, "Target"));
        assertEquals(HttpStatus.BAD_REQUEST, exZero.getStatus());
        assertEquals("Target must be greater than zero.", exZero.getMessage());

        AppException exNeg = assertThrows(AppException.class,
                () -> Money.parsePositive(new BigDecimal("-10.00"), "Target"));
        assertEquals(HttpStatus.BAD_REQUEST, exNeg.getStatus());

        AppException exScale = assertThrows(AppException.class,
                () -> Money.parsePositive(new BigDecimal("10.555"), "Target"));
        assertEquals(HttpStatus.BAD_REQUEST, exScale.getStatus());
        assertEquals("Target can have at most 2 decimal places.", exScale.getMessage());
    }

    @Test
    @DisplayName("fromFirestore safely converts Numbers into 2-decimal BigDecimals")
    void testFromFirestore() {
        assertEquals(new BigDecimal("250.00"), Money.fromFirestore(250.0));
        assertEquals(new BigDecimal("100.00"), Money.fromFirestore(100L));
        assertEquals(new BigDecimal("0.00"), Money.fromFirestore(null));
        assertEquals(new BigDecimal("0.00"), Money.fromFirestore("not-a-number"));
    }
}
