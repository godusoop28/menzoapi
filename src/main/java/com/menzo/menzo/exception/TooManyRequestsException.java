package com.menzo.menzo.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends ApiException {
    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }

    public TooManyRequestsException(String code, String message, int retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, code, message, retryAfterSeconds);
    }
}
