package dev.hippodid.client;

/**
 * Thrown when the HippoDid API returns an error response (4xx, 5xx).
 *
 * <p>Wraps the error details from the API's standard error envelope:
 * <pre>{@code
 * {
 *   "error": {
 *     "type": "CharacterNotFound",
 *     "message": "Character abc123 not found",
 *     "status": 404
 *   }
 * }
 * }</pre>
 */
public class HippoDidException extends RuntimeException {

    private final int statusCode;
    private final String errorType;

    public HippoDidException(int statusCode, String errorType, String message) {
        super("[" + statusCode + "] " + errorType + ": " + message);
        this.statusCode = statusCode;
        this.errorType = errorType;
    }

    public HippoDidException(int statusCode, String message) {
        this(statusCode, "ApiError", message);
    }

    /** HTTP status code returned by the API. */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Error type returned by the API.
     *
     * <p>Common values: {@code CharacterNotFound}, {@code MemoryNotFound},
     * {@code CharacterLimitExceeded}, {@code TierLimitExceeded}, {@code Unauthorized}.
     */
    public String errorType() {
        return errorType;
    }
}
