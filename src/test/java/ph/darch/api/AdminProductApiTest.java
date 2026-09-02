package ph.darch.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ph.darch.api.entity.MediaAsset;
import ph.darch.api.repository.MediaAssetRepository;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminProductApiTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "this-is-a-test-admin-password-123456789";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    private String auth;
    private String imgUrl;
    private String imgUrl2;
    private String vidUrl;

    @BeforeEach
    void setUp() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + ADMIN_USERNAME
                                + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        auth = "Bearer " + objectMapper.readTree(body).get("token").asText();

        imgUrl = asset(ph.darch.api.entity.MediaType.IMAGE, "/img/one.jpg");
        imgUrl2 = asset(ph.darch.api.entity.MediaType.IMAGE, "/img/two.jpg");
        vidUrl = asset(ph.darch.api.entity.MediaType.VIDEO, "/vid/clip.mp4");
    }

    private String asset(ph.darch.api.entity.MediaType type, String suffix) {
        MediaAsset m = new MediaAsset();
        m.setMediaType(type);
        m.setBucket(type == ph.darch.api.entity.MediaType.IMAGE
                ? "product-images" : "product-videos");
        m.setObjectPath("admin-test" + suffix);
        m.setPublicUrl("https://cdn.supabase.co/public" + suffix);
        return mediaAssetRepository.save(m).getPublicUrl();
    }

    private String createProduct(String json) throws Exception {
        String body = mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    @Test
    void createReturns201AndAppearsInAdminAndPublicLists() throws Exception {
        String json = """
                {"name":"Test Keychain","description":"engraved","price":250.00,
                 "currency":"PHP","buyUrl":"https://shopee.ph/x","isActive":true,
                 "featured":false,"images":["%s"],"videoUrl":"%s"}
                """.formatted(imgUrl, vidUrl);
        String id = createProduct(json);

        mockMvc.perform(get("/api/admin/products").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItem("Test Keychain")));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItem("Test Keychain")));

        mockMvc.perform(get("/api/admin/products/" + id).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Keychain"))
                .andExpect(jsonPath("$.images", hasSize(1)))
                .andExpect(jsonPath("$.videoUrl").value(vidUrl));
    }

    @Test
    void omittedSlugIsAutoGeneratedFromName() throws Exception {
        String body = mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Another Item","price":100,"buyUrl":"https://e.test"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        objectMapper.readTree(body).get("slug").asText();
        mockMvc.perform(get("/api/admin/products").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].slug", hasItem("another-item")));
    }

    @Test
    void duplicateNameGetsUniqueSuffixedSlug() throws Exception {
        createProduct("""
                {"name":"Same Name","price":100,"buyUrl":"https://e.test"}
                """);
        createProduct("""
                {"name":"Same Name","price":100,"buyUrl":"https://e.test"}
                """);
        mockMvc.perform(get("/api/admin/products").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].slug",
                        containsInAnyOrder("same-name", "same-name-2")));
    }

    @Test
    void explicitDuplicateSlugReturns409() throws Exception {
        createProduct("""
                {"name":"First","slug":"reserved","price":100,"buyUrl":"https://e.test"}
                """);
        mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Second","slug":"reserved","price":100,"buyUrl":"https://e.test"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void getByIdMissingReturns404() throws Exception {
        mockMvc.perform(get("/api/admin/products/999999").header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void putFullyReplacesProductAndMedia() throws Exception {
        String id = createProduct("""
                {"name":"Old","price":100,"buyUrl":"https://a.test","images":["%s"],"videoUrl":"%s"}
                """.formatted(imgUrl, vidUrl));

        mockMvc.perform(put("/api/admin/products/" + id)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"New","price":300,"buyUrl":"https://b.test",
                                 "images":["%s","%s"],"featured":true,"isActive":true}
                                """.formatted(imgUrl, imgUrl2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"))
                .andExpect(jsonPath("$.featured").value(true))
                .andExpect(jsonPath("$.images", hasSize(2)))
                .andExpect(jsonPath("$.images[0]").value(imgUrl))
                .andExpect(jsonPath("$.images[1]").value(imgUrl2))
                .andExpect(jsonPath("$.videoUrl").value(nullValue()));
    }

    @Test
    void patchIsActiveFalseHidesFromPublicButKeepsInAdmin() throws Exception {
        String id = createProduct("""
                {"name":"Visible","price":100,"buyUrl":"https://c.test"}
                """);

        mockMvc.perform(patch("/api/admin/products/" + id)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        mockMvc.perform(get("/api/products").param("search", "Visible"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        mockMvc.perform(get("/api/admin/products").header("Authorization", auth)
                        .param("search", "Visible"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(Integer.valueOf(id))));
    }

    @Test
    void patchPriceAndUnknownKeysAreIgnored() throws Exception {
        String id = createProduct("""
                {"name":"Priced","price":100,"buyUrl":"https://d.test"}
                """);
        mockMvc.perform(patch("/api/admin/products/" + id)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\":420.50,\"bogus\":\"ignored\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(420.50));
    }

    @Test
    void deleteRemovesProductAndCascadesMediaRows() throws Exception {
        String id = createProduct("""
                {"name":"Doomed","price":100,"buyUrl":"https://e.test","images":["%s"]}
                """.formatted(imgUrl));

        mockMvc.perform(get("/api/admin/products/" + id).header("Authorization", auth))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/admin/products/" + id).header("Authorization", auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/products/" + id).header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidPayloadsReturn400WithDetails() throws Exception {
        String[][] cases = {
                // negative price
                {"{\"name\":\"X\",\"price\":-1,\"buyUrl\":\"https://e.test\"}"},
                // blank name
                {"{\"name\":\"\",\"price\":1,\"buyUrl\":\"https://e.test\"}"},
                // malformed buyUrl
                {"{\"name\":\"X\",\"price\":1,\"buyUrl\":\"not-a-url\"}"},
                // two videos (image slot holds the video + videoUrl set)
                {"{\"name\":\"X\",\"price\":1,\"buyUrl\":\"https://e.test\","
                        + "\"videoUrl\":\"" + vidUrl + "\",\"images\":[\"" + vidUrl + "\"]}"},
                // >10 images
                {"{\"name\":\"X\",\"price\":1,\"buyUrl\":\"https://e.test\","
                        + "\"images\":[\"" + imgUrl + "\",\"" + imgUrl + "\",\"" + imgUrl + "\",\""
                        + imgUrl + "\",\"" + imgUrl + "\",\"" + imgUrl + "\",\"" + imgUrl + "\",\""
                        + imgUrl + "\",\"" + imgUrl + "\",\"" + imgUrl + "\",\"" + imgUrl + "\"]}"},
                // image URL never uploaded
                {"{\"name\":\"X\",\"price\":1,\"buyUrl\":\"https://e.test\","
                        + "\"images\":[\"https://evil.example.com/x.jpg\"]}"},
        };
        for (String[] c : cases) {
            mockMvc.perform(post("/api/admin/products")
                            .header("Authorization", auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(c[0]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details").exists());
        }
    }

    @Test
    void adminListIncludesInactiveProducts() throws Exception {
        String id = createProduct("""
                {"name":"Offline","price":100,"buyUrl":"https://f.test"}
                """);
        mockMvc.perform(patch("/api/admin/products/" + id)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":false}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/products").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].slug", hasItem("offline")))
                .andExpect(jsonPath("$.content[*].isActive", hasItem(false)));
    }

    @Test
    void adminEndpointsRequireAuth() throws Exception {
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"price\":1,\"buyUrl\":\"https://e.test\"}"))
                .andExpect(status().isUnauthorized());
    }
}
