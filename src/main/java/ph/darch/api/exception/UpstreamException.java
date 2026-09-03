package ph.darch.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an upstream dependency (e.g. Supabase Storage) fails while serving
 * the request. Surfaced to clients as HTTP 502 Bad Gateway.
 */
public class UpstreamException extends ApiException {

    public UpstreamException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }
}
