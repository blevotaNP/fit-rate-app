package com.outfitrating.exception;

import org.springframework.http.HttpStatus;

/** Base for the handful of expected, client-visible failures the API reports. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
