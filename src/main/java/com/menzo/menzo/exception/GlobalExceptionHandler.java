package com.menzo.menzo.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import io.jsonwebtoken.JwtException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, WebRequest request) {
        ErrorResponse body = ErrorResponse.of(ex.getStatus().value(), ex.getCode(), ex.getStatus().getReasonPhrase(),
                ex.getMessage(), path(request), ex.getRetryAfterSeconds());
        ResponseEntity.BodyBuilder response = ResponseEntity.status(ex.getStatus());
        if (ex.getRetryAfterSeconds() != null) {
            response = response.header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        }
        return response.body(body);
    }

    /** Respaldo de MusicService.expectedVersion: si dos coanfitriones controlan Menzi DJ casi al
     * mismo tiempo, Hibernate rechaza el segundo UPDATE por versión desactualizada (@Version en
     * LiveMusicSession) — esto solo dispara si el chequeo explícito de expectedVersion no lo
     * atajó antes, así que igual se traduce a 409 en vez de un 500 genérico. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(
                HttpStatus.CONFLICT.value(), "Conflict",
                "Alguien más actualizó Menzi DJ justo ahora — volvé a intentar.", path(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(), "Bad Request", "Datos inválidos", path(request), fieldErrors));
    }

    @ExceptionHandler({AuthenticationException.class, JwtException.class})
    public ResponseEntity<ErrorResponse> handleAuth(Exception ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(), "Unauthorized", ex.getMessage(), path(request)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(), "Forbidden", "No tienes permiso para esta acción", path(request)));
    }

    /** Antes esta rama devolvía un 500 genérico sin dejar rastro — cualquier excepción no
     * anticipada (NPE, bug de parseo, lo que sea) quedaba completamente invisible en los logs de
     * Render. Ahora se loguea la clase y el stack trace completo antes de responder, así la
     * próxima falla real (por ejemplo, la de Menzi DJ/YouTube) sí se puede diagnosticar. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unhandled exception on {}: {}", path(request), ex.getClass().getSimpleName(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                "Ocurrió un error inesperado", path(request)));
    }

    private String path(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
