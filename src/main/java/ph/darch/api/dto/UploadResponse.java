package ph.darch.api.dto;

import ph.darch.api.entity.MediaType;

public record UploadResponse(
        String publicUrl,
        MediaType mediaType
) {
}
