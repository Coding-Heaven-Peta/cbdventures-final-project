package za.ac.richfield.cbdventures.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts exceptions into a single, predictable JSON error shape so the
 * front-end can always show a friendly message (NFR: Reliability).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::describe)
                .collect(Collectors.joining(" "));
        if (message.isBlank()) {
            message = "Please check the information you entered.";
        }
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /** Missing static assets or endpoints return a clean 404 instead of a 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMissing(NoResourceFoundException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "That page could not be found.", request);
    }

    /** A path variable that is not the expected type is a client mistake, not a 500. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                "That address is not valid. Check the link and try again.", request);
    }

    /**
     * A body that cannot be read (malformed JSON, or a value that is not one of
     * the allowed options) is the caller's mistake, so it must answer 400 — not
     * the generic 500. The front-end shows this message verbatim.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex,
                                                                HttpServletRequest request) {
        Throwable cause = ex.getMostSpecificCause();
        String detail = cause == null ? "" : String.valueOf(cause.getMessage());
        String message = detail.contains("Enum class")
                ? "That option is not available. Choose one of the listed values and try again."
                : "We could not read that request. Check the details you sent and try again.";
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex,
                                                                     HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Log the cause: without it a 500 in a demo is impossible to diagnose.
        LOG.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong on our side. Please try again in a moment.", request);
    }

    private String describe(FieldError error) {
        String field = error.getField();
        String readable = field.replaceAll("([A-Z])", " $1").toLowerCase();
        return Character.toUpperCase(readable.charAt(0)) + readable.substring(1) + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message,
                                                      HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message == null || message.isBlank() ? status.getReasonPhrase() : message);
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
