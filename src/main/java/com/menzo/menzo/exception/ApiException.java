package com.menzo.menzo.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String message) {
        this(status, null, message);
    }

    /** code es un identificador estable para el cliente (p. ej. "YOUTUBE_QUOTA_EXCEEDED") —
     * opcional, null para los errores genéricos que ya se distinguían bien por HTTP status solo. */
    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
