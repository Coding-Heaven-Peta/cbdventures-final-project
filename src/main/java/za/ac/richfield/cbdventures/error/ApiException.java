package za.ac.richfield.cbdventures.error;

import org.springframework.http.HttpStatus;

/**
 * Exception carrying an HTTP status plus a plain-English message suitable for
 * display directly in the UI (NFR: Usability - "plain-English labels and error
 * messages throughout").
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
