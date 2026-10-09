package com.crowdfund.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.http.HttpStatus;

import com.crowdfund.exception.AppException;

/** Money is handled with BigDecimal (2 decimals) so totals never drift. */
public final class Money {

    private static final BigDecimal MAX = new BigDecimal("1000000000");

    private Money() {
    }

    public static BigDecimal parsePositive(BigDecimal value, String label) {

        if (value == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, label + " is required.");
        }

        if (value.signum() <= 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, label + " must be greater than zero.");
        }

        if (value.stripTrailingZeros().scale() > 2) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    label + " can have at most 2 decimal places.");
        }

        if (value.compareTo(MAX) > 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, label + " is too large.");
        }

        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    /** Reads a number stored in Firestore (Long or Double) as a 2-decimal BigDecimal. */
    public static BigDecimal fromFirestore(Object raw) {

        if (raw instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO.setScale(2);
    }
}
