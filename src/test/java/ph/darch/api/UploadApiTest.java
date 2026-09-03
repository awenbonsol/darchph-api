package ph.darch.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ph.darch.api.exception.UpstreamException;
import ph.darch.api.repository.MediaAssetRepository;
import ph.darch.api.service.StorageService;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UploadApiTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "this-is-a-test-admin-password-123456789";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    @MockBean
    private StorageService storageService;

    private String auth;

    @BeforeEach
    void setUp() throws Exception {
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + ADMIN_USERNAME
                                + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        auth = "Bearer " + objectMapper.readTree(body).get("token").asText();

        doNothing().when(storageService).upload(anyString(), anyString(), any(), anyString());
        doNothing().when(storageService).delete(anyString(), anyString());
        when(storageService.publicUrl(anyString(), anyString()))
                .thenAnswer(inv -> "https://cdn.supabase.co/public/"
                        + inv.getArgument(0) + "/" + inv.getArgument(1));
    }

    private MockMultipartFile file(String originalName, String contentType, byte[] content) {
        return new MockMultipartFile("file", originalName, contentType, content);
    }

    @Test
    void imageUploadReturns200WithImageTypeAndPersistsAsset() throws Exception {
        long before = mediaAssetRepository.count();
        mockMvc.perform(multipart("/api/uploads").file(file("photo.jpg", "image/jpeg", "fake-image".getBytes()))
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaType").value("IMAGE"))
                .andExpect(jsonPath("$.publicUrl")
                        .value(startsWith("https://cdn.supabase.co/public/product-images/")));

        Assertions.assertEquals(before + 1, mediaAssetRepository.count());
        Assertions.assertTrue(mediaAssetRepository.findAll().stream()
                .anyMatch(a -> "product-images".equals(a.getBucket())
                        && a.getMediaType() == ph.darch.api.entity.MediaType.IMAGE));
    }

    @Test
    void videoUploadReturns200WithVideoTypeAndPersistsAsset() throws Exception {
        mockMvc.perform(multipart("/api/uploads").file(file("clip.mp4", "video/mp4", "fake-video".getBytes()))
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaType").value("VIDEO"))
                .andExpect(jsonPath("$.publicUrl")
                        .value(startsWith("https://cdn.supabase.co/public/product-videos/")));

        Assertions.assertTrue(mediaAssetRepository.findAll().stream()
                .anyMatch(a -> "product-videos".equals(a.getBucket())
                        && a.getMediaType() == ph.darch.api.entity.MediaType.VIDEO));
    }

    @Test
    void pngWebpAndWebmAreAccepted() throws Exception {
        mockMvc.perform(multipart("/api/uploads").file(file("a.png", "image/png", "x".getBytes()))
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaType").value("IMAGE"));

        mockMvc.perform(multipart("/api/uploads").file(file("b.webp", "image/webp", "x".getBytes()))
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaType").value("IMAGE"));

        mockMvc.perform(multipart("/api/uploads").file(file("c.webm", "video/webm", "x".getBytes()))
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaType").value("VIDEO"));
    }

    @Test
    void disallowedTypeReturns400() throws Exception {
        mockMvc.perform(multipart("/api/uploads").file(file("evil.exe", "application/octet-stream", "x".getBytes()))
                        .header("Authorization", auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.file").exists());
    }

    @Test
    void emptyFileReturns400() throws Exception {
        mockMvc.perform(multipart("/api/uploads").file(new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]))
                        .header("Authorization", auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.file").exists());
    }

    @Test
    void oversizeImageReturns400() throws Exception {
        byte[] content = new byte[6 * 1024 * 1024];
        mockMvc.perform(multipart("/api/uploads").file(file("big.jpg", "image/jpeg", content))
                        .header("Authorization", auth))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.file").exists());
    }

    @Test
    void uploadRequiresAuth() throws Exception {
        mockMvc.perform(multipart("/api/uploads").file(file("a.jpg", "image/jpeg", "x".getBytes())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void storageUploadFailureReturns502() throws Exception {
        doThrow(new UpstreamException("boom"))
                .when(storageService).upload(anyString(), anyString(), any(), anyString());
        mockMvc.perform(multipart("/api/uploads").file(file("a.jpg", "image/jpeg", "x".getBytes()))
                        .header("Authorization", auth))
                .andExpect(status().isBadGateway());
    }
}
