package ph.darch.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ph.darch.api.dto.UploadResponse;
import ph.darch.api.service.UploadService;

@RestController
@RequestMapping("/api/uploads")
@Tag(name = "Uploads", description = "Media uploads to Supabase Storage (JWT required)")
@SecurityRequirement(name = "bearerAuth")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @Operation(summary = "Upload an image or video")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(uploadService.upload(file));
    }
}
