package ph.darch.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        Map<String, String> details,
        String path
) {

    public static ApiError of(int status, String error, Map<String, String> details, String path) {
        return new ApiError(Instant.now(), status, error, details, path);
    }
}
