package org.vornex.app.Exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("audit");

    private ErrorResponse buildErrorResponse(HttpStatus status, String message, String path, List<String> errors) {
        return ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .traceId(MDC.get("traceId"))
                .errors(errors)
                .build();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                buildErrorResponse(HttpStatus.FORBIDDEN, "Доступ запрещён", request.getRequestURI(), null)
        );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNameNotFound(UsernameNotFoundException ex, HttpServletRequest request) {
        auditLogger.info("User not found at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                buildErrorResponse(HttpStatus.UNAUTHORIZED, "Пользователь не найден", request.getRequestURI(), null)
        );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex, HttpServletRequest request) {
        auditLogger.warn("Disabled account at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                buildErrorResponse(HttpStatus.UNAUTHORIZED, "Аккаунт удалён", request.getRequestURI(), null)
        );
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleBanned(LockedException ex, HttpServletRequest request) {
        auditLogger.warn("Banned user at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                buildErrorResponse(HttpStatus.FORBIDDEN, "Вы забанены", request.getRequestURI(), null)
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();

        log.warn("Validation failed at [{}]: {}", request.getRequestURI(), errors);

        return ResponseEntity.badRequest().body(
                buildErrorResponse(HttpStatus.BAD_REQUEST, "Ошибка валидации", request.getRequestURI(), errors)
        );
    }
//    @ExceptionHandler(NoResourceFoundException.class)
//    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
//        log.warn("Resource not found at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
//        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
//                buildErrorResponse(HttpStatus.NOT_FOUND, "Ресурс не найден", request.getRequestURI(), null)
//        );
//    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            log.error("Invalid status code in ResponseStatusException at [{}]: {}",
                    request.getRequestURI(), ex.getMessage(), ex);
        } else {
            log.warn("ResponseStatusException at [{}] with status [{}]: {}",
                    request.getRequestURI(), status, ex.getReason(), ex);
        }

        String reason = ex.getReason() != null ? ex.getReason() : "Произошла ошибка";

        return ResponseEntity.status(status).body(
                buildErrorResponse(status, reason, request.getRequestURI(), null)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Что-то пошло не так. Мы уже работаем над этим.", request.getRequestURI(), null)
        );
    }

}

// Spring обрабатывает исключения в порядке определения методов
