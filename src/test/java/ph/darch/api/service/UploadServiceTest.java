package ph.darch.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import ph.darch.api.dto.UploadResponse;
import ph.darch.api.entity.MediaType;
import ph.darch.api.exception.BadRequestException;
import ph.darch.api.repository.MediaAssetRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UploadServiceTest {

    private final StorageService storageService = mock(StorageService.class);
    private final MediaAssetRepository mediaAssetRepository = mock(MediaAssetRepository.class);
    private final UploadService uploadService = new UploadService(storageService, mediaAssetRepository);

    @Test
    void imageUploadUsesProductImagesBucketAndImageKey() {
        UploadResponse resp = uploadService.upload(
                new MockMultipartFile("file", "photo.jpg", "image/jpeg", "data".getBytes()));
        assertThat(resp.mediaType()).isEqualTo(MediaType.IMAGE);

        org.mockito.ArgumentCaptor<String> bucket = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<String> key = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(storageService).upload(bucket.capture(), key.capture(),
                org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.eq("image/jpeg"));
        assertThat(bucket.getValue()).isEqualTo("product-images");
        assertThat(key.getValue()).endsWith(".jpg");
    }

    @Test
    void videoUploadUsesProductVideosBucket() {
        uploadService.upload(new MockMultipartFile("file", "clip.mp4", "video/mp4", "data".getBytes()));
        verify(storageService).upload(org.mockito.ArgumentMatchers.eq("product-videos"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(byte[].class),
                org.mockito.ArgumentMatchers.eq("video/mp4"));
    }

    @Test
    void disallowedTypeThrowsBadRequest() {
        assertThatThrownBy(() -> uploadService.upload(
                new MockMultipartFile("file", "evil.exe", "application/octet-stream", "x".getBytes())))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(storageService);
    }

    @Test
    void unknownContentTypeThrowsBadRequest() {
        assertThatThrownBy(() -> uploadService.upload(
                new MockMultipartFile("file", "a.mystery", null, "x".getBytes())))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(storageService);
    }

    @Test
    void emptyFileThrowsBadRequest() {
        assertThatThrownBy(() -> uploadService.upload(
                new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0])))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(storageService);
    }

    @Test
    void oversizeImageThrowsBadRequest() {
        assertThatThrownBy(() -> uploadService.upload(
                new MockMultipartFile("file", "big.jpg", "image/jpeg", new byte[(int) UploadService.IMAGE_MAX_BYTES + 1])))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(storageService);
    }

    @Test
    void oversizeVideoThrowsBadRequest() {
        assertThatThrownBy(() -> uploadService.upload(
                new MockMultipartFile("file", "big.mp4", "video/mp4", new byte[(int) UploadService.VIDEO_MAX_BYTES + 1])))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(storageService);
    }
}
