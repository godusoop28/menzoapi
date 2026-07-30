package com.menzo.menzo.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Integer retryAfterSeconds;

    public ApiException(HttpStatus status, String message) {
        this(status, null, message);
    }

    /** code es un identificador estable para el cliente (p. ej. "YOUTUBE_QUOTA_EXCEEDED") —
     * opcional, null para los errores genéricos que ya se distinguían bien por HTTP status solo. */
    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    /** retryAfterSeconds es opcional (null salvo 429) — le dice al cliente cuánto esperar antes de
     * reintentar en vez de que adivine con un backoff fijo (ver YoutubeRateLimiter). */
    public ApiException(HttpStatus status, String code, String message, Integer retryAfterSeconds) {
        super(message);
        this.status = status;
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
