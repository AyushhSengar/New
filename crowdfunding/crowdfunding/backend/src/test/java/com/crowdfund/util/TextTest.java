package com.crowdfund.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.crowdfund.exception.AppException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextTest {

    @Test
    @DisplayName("require trims and accepts valid text within max length")
    void testRequireValid() {
        String result = Text.require("  Clean Title  ", "Title", 50);
        assertEquals("Clean Title", result);
    }

    @Test
    @DisplayName("require rejects null, empty and overly long strings")
    void testRequireInvalid() {
        AppException exNull = assertThrows(AppException.class,
                () -> Text.require(null, "Description", 100));
        assertEquals(HttpStatus.BAD_REQUEST, exNull.getStatus());
        assertEquals("Description is required.", exNull.getMessage());

        AppException exEmpty = assertThrows(AppException.class,
                () -> Text.require("   ", "Description", 100));
        assertEquals(HttpStatus.BAD_REQUEST, exEmpty.getStatus());

        AppException exLong = assertThrows(AppException.class,
                () -> Text.require("a".repeat(11), "Code", 10));
        assertEquals(HttpStatus.BAD_REQUEST, exLong.getStatus());
        assertEquals("Code must be at most 10 characters.", exLong.getMessage());
    }

    @Test
    @DisplayName("requireId validates alphanumeric Firestore IDs and blocks injection")
    void testRequireId() {
        assertEquals("abc_123-XYZ", Text.requireId("abc_123-XYZ"));

        assertThrows(AppException.class, () -> Text.requireId(null));
        assertThrows(AppException.class, () -> Text.requireId(""));
        assertThrows(AppException.class, () -> Text.requireId("invalid/id"));
        assertThrows(AppException.class, () -> Text.requireId("id with spaces"));
    }
}
