package com.menzo.menzo.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        /** Código estable para que el cliente distinga causas sin parsear el mensaje humano (p. ej.
         * "YOUTUBE_QUOTA_EXCEEDED", "LIVE_NOT_ACTIVE") — null para errores genéricos que no lo
         * necesitan (validación de campos, 500 inesperado, etc.). */
        String code,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors,
        /** Solo para 429 (ver TooManyRequestsException/YoutubeRateLimiter) — null en cualquier otro
         * caso, así el cliente sabe cuándo tiene sentido reintentar en vez de adivinar. */
        Integer retryAfterSeconds) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, null, error, message, path, null, null);
    }

    public static ErrorResponse of(int status, String code, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, code, error, message, path, null, null);
    }

    public static ErrorResponse of(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, null, error, message, path, fieldErrors, null);
    }

    public static ErrorResponse of(int status, String code, String error, String message, String path, Integer retryAfterSeconds) {
        return new ErrorResponse(Instant.now(), status, code, error, message, path, null, retryAfterSeconds);
    }
}
