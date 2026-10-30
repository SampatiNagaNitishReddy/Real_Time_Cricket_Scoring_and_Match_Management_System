package com.nitish.cricket.cricket_platform.ExceptionHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Matches messages like "NEXT_BATSMAN_REQUIRED: Both striker and ..."
    private static final Pattern CODE_PREFIX =
            Pattern.compile("^([A-Z][A-Z_]{2,}):\\s*(.*)$", Pattern.DOTALL);

    public record ApiError(String error, String code) {}

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(message, code));
    }

    // Business rule: bad input / something doesn't exist
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "That request wasn't valid.";
        boolean notFound = msg.toLowerCase().contains("not found");

        return build(
                notFound ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST,
                notFound ? "NOT_FOUND" : "BAD_REQUEST",
                msg);
    }

    // Business rule: the action isn't allowed in the current state
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> conflict(IllegalStateException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "That action isn't allowed right now.";

        Matcher m = CODE_PREFIX.matcher(msg);
        if (m.matches()) {
            return build(HttpStatus.CONFLICT, m.group(1), m.group(2));
        }
        return build(HttpStatus.CONFLICT, "CONFLICT", msg);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden(AccessDeniedException e) {
        String msg = e.getMessage() != null ? e.getMessage() : "You don't have permission to do this.";
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", msg);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> unauthorized(AuthenticationException e) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Please log in to continue.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse("Some details are missing or invalid.");
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", msg);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ApiError> malformed(Exception e) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "The request was incomplete or malformed.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> tooLarge(MaxUploadSizeExceededException e) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "That file is too large.");
    }

    // Anything unexpected: log it, never send the raw message to the client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e) {

        // Spring's own HTTP errors (405, 404, ...) keep their real status
        if (e instanceof ErrorResponse er) {
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());
            return build(status, status.name(), status.getReasonPhrase());
        }

        log.error("Unhandled exception", e);

        return build(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR",
                "Something went wrong on our side. Please try again.");
    }
}
