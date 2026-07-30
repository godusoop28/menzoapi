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
        Map<String, String> fieldErrors) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, null, error, message, path, null);
    }

    public static ErrorResponse of(int status, String code, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, code, error, message, path, null);
    }

    public static ErrorResponse of(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, null, error, message, path, fieldErrors);
    }
}
