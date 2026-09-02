package ph.darch.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String slug,
        String description,
        BigDecimal price,
        String currency,
        List<String> images,
        String videoUrl,
        String buyUrl,
        boolean isActive,
        boolean featured,
        Instant createdAt,
        Instant updatedAt) {
}
