package ph.darch.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @NotBlank(message = "name is required")
        @Size(max = 200, message = "name must be at most 200 characters")
        String name,

        @Size(max = 240, message = "slug must be at most 240 characters")
        @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*",
                message = "slug must contain only lowercase letters, digits and single dashes")
        String slug,

        @Size(max = 5000, message = "description must be at most 5000 characters")
        String description,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", message = "price must be zero or positive")
        @Digits(integer = 10, fraction = 2, message = "price is out of range")
        BigDecimal price,

        @Size(min = 3, max = 3, message = "currency must be a 3-letter code")
        String currency,

        @NotBlank(message = "buyUrl is required")
        @Size(max = 2048, message = "buyUrl is too long")
        @Pattern(regexp = "^https?://.+" , message = "buyUrl must be a valid http(s) URL")
        String buyUrl,

        @Size(max = 10, message = "at most 10 images are allowed")
        List<String> images,

        @Size(max = 2048, message = "videoUrl is too long")
        @Pattern(regexp = "^https?://.+", message = "videoUrl must be a valid http(s) URL")
        String videoUrl,

        Boolean isActive,
        Boolean featured) {
}
