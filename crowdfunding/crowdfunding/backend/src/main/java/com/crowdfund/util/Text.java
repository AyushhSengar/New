package com.crowdfund.util;

import org.springframework.http.HttpStatus;

import com.crowdfund.exception.AppException;

public final class Text {

    private Text() {
    }

    /** Trims the value, rejects empty text and text longer than max. */
    public static String require(String value, String label, int max) {

        String text = value == null ? "" : value.trim();

        if (text.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, label + " is required.");
        }

        if (text.length() > max) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    label + " must be at most " + max + " characters.");
        }

        return text;
    }

    /** Firestore document ids must not contain '/' etc. */
    public static String requireId(String id) {

        if (id == null || !id.matches("^[A-Za-z0-9_-]{1,128}$")) {
            throw new AppException(HttpStatus.NOT_FOUND, "Record not found.");
        }

        return id;
    }
}
