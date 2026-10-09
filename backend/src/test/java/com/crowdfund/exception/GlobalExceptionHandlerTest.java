package com.crowdfund.exception;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleApp maps AppException to corresponding HTTP status and error body")
    void testHandleApp() {
        AppException ex = new AppException(HttpStatus.CONFLICT, "Campaign not pending.");
        ResponseEntity<Map<String, String>> response = handler.handleApp(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Campaign not pending.", response.getBody().get("error"));
    }

    @Test
    @DisplayName("handleBadBody returns 400 Bad Request with generic error message")
    @SuppressWarnings("deprecation")
    void testHandleBadBody() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Invalid JSON");
        ResponseEntity<Map<String, String>> response = handler.handleBadBody(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid request body.", response.getBody().get("error"));
    }

    @Test
    @DisplayName("handleOther returns 500 Internal Server Error without leaking internal details")
    void testHandleOther() {
        Exception ex = new RuntimeException("Sensitive database stacktrace");
        ResponseEntity<Map<String, String>> response = handler.handleOther(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Unexpected server error. Please try again.", response.getBody().get("error"));
    }
}
