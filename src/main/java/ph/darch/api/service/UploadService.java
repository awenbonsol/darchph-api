package ph.darch.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ph.darch.api.dto.UploadResponse;
import ph.darch.api.entity.MediaAsset;
import ph.darch.api.entity.MediaType;
import ph.darch.api.exception.BadRequestException;
import ph.darch.api.repository.MediaAssetRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Validates an uploaded file, stores it in Supabase Storage via
 * {@link StorageService}, and records the resulting {@link MediaAsset}.
 */
@Service
public class UploadService {

    public static final long IMAGE_MAX_BYTES = 5L * 1024 * 1024;   // 5 MB
    public static final long VIDEO_MAX_BYTES = 100L * 1024 * 1024; // 100 MB

    private static final Map<String, UploadSpec> SPECS = Map.of(
            "image/jpeg", new UploadSpec(MediaType.IMAGE, "product-images", "jpg", IMAGE_MAX_BYTES),
            "image/png", new UploadSpec(MediaType.IMAGE, "product-images", "png", IMAGE_MAX_BYTES),
            "image/webp", new UploadSpec(MediaType.IMAGE, "product-images", "webp", IMAGE_MAX_BYTES),
            "video/mp4", new UploadSpec(MediaType.VIDEO, "product-videos", "mp4", VIDEO_MAX_BYTES),
            "video/webm", new UploadSpec(MediaType.VIDEO, "product-videos", "webm", VIDEO_MAX_BYTES)
    );

    private final StorageService storageService;
    private final MediaAssetRepository mediaAssetRepository;

    public UploadService(StorageService storageService,
                         MediaAssetRepository mediaAssetRepository) {
        this.storageService = storageService;
        this.mediaAssetRepository = mediaAssetRepository;
    }

    @Transactional
    public UploadResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("file", "file is required and must be non-empty");
        }

        String contentType = file.getContentType();
        UploadSpec spec = contentType == null ? null : SPECS.get(contentType.toLowerCase());
        if (spec == null) {
            throw badRequest("file", "Unsupported content type: " + (contentType == null ? "unknown" : contentType));
        }

        if (file.getSize() > spec.maxBytes) {
            throw badRequest("file",
                    "File exceeds the " + (spec.mediaType == MediaType.IMAGE ? "5 MB" : "100 MB")
                            + " limit for " + spec.mediaType.name().toLowerCase() + "s");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }

        String key = UUID.randomUUID() + "." + spec.extension;
        storageService.upload(spec.bucket, key, bytes, contentType);

        MediaAsset asset = new MediaAsset();
        asset.setMediaType(spec.mediaType);
        asset.setBucket(spec.bucket);
        asset.setObjectPath(key);
        asset.setPublicUrl(storageService.publicUrl(spec.bucket, key));
        mediaAssetRepository.save(asset);

        return new UploadResponse(asset.getPublicUrl(), asset.getMediaType());
    }

    private BadRequestException badRequest(String field, String message) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put(field, message);
        return new BadRequestException(details);
    }

    private record UploadSpec(MediaType mediaType, String bucket, String extension, long maxBytes) {
    }
}
