package com.crowdfund.exception;

import org.springframework.http.HttpStatus;

/** An error that is safe to show to the user, with the HTTP status to return. */
public class AppException extends RuntimeException {

    private final HttpStatus status;

    public AppException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
