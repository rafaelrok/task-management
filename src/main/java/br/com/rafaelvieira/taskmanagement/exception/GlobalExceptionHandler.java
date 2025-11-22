package br.com.rafaelvieira.taskmanagement.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
        log.warn("Forbidden: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        log.info("Not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        log.error("Internal error: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            // Se já existir, concatena mensagens
            Object existing = body.get(fe.getField());
            if (existing == null) {
                body.put(fe.getField(), fe.getDefaultMessage());
            } else {
                body.put(fe.getField(), existing + "; " + fe.getDefaultMessage());
            }
        }
        // Metadados opcionais
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.badRequest().body(body);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(
                        Map.of(
                                "timestamp", LocalDateTime.now().toString(),
                                "status", status.value(),
                                "error", status.getReasonPhrase(),
                                "message", message));
    }
}
