package ph.darch.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ph.darch.api.entity.MediaAsset;
import ph.darch.api.entity.MediaType;
import ph.darch.api.entity.Product;
import ph.darch.api.entity.ProductMedia;
import ph.darch.api.repository.MediaAssetRepository;
import ph.darch.api.repository.ProductMediaRepository;
import ph.darch.api.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProductApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMediaRepository productMediaRepository;

    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    private final AtomicInteger counter = new AtomicInteger();
    private final Map<String, Product> saved = new ConcurrentHashMap<>();

    @BeforeEach
    void seed() {
        Product a = product("personalized-wooden-keychain", "Personalized Wooden Keychain",
                "Engraved maple keychain for gifting", "250.00", true, true);
        attachMedia(a, List.of(
                newMedia(MediaType.IMAGE, "/img/a0.jpg"),
                newMedia(MediaType.IMAGE, "/img/a1.jpg"),
                newMedia(MediaType.IMAGE, "/img/a2.jpg"),
                newMedia(MediaType.VIDEO, "/vid/a.mp4")));

        sleep(10);

        Product b = product("laser-cut-coaster-set", "Laser Cut Coaster Set",
                "Set of four coasters with family names", "800.00", true, false);

        sleep(10);

        product("hidden-inactive-item", "Hidden Inactive Item",
                "Not on the storefront", "999.00", false, false);

        saved.put("A", a);
        saved.put("B", b);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Product product(String slug, String name, String description,
                            String price, boolean isActive, boolean featured) {
        Product p = new Product();
        p.setName(name);
        p.setSlug(slug);
        p.setDescription(description);
        p.setPrice(new BigDecimal(price));
        p.setCurrency("PHP");
        p.setBuyUrl("https://shopee.ph/" + slug);
        p.setIsActive(isActive);
        p.setFeatured(featured);
        return productRepository.save(p);
    }

    private MediaAsset newMedia(MediaType type, String url) {
        MediaAsset m = new MediaAsset();
        m.setMediaType(type);
        m.setBucket(type == MediaType.IMAGE ? "product-images" : "product-videos");
        m.setObjectPath("products/" + counter.incrementAndGet() + url);
        m.setPublicUrl("https://cdn.supabase.co/public" + url);
        return mediaAssetRepository.save(m);
    }

    private void attachMedia(Product product, List<MediaAsset> assets) {
        for (int i = 0; i < assets.size(); i++) {
            MediaAsset asset = assets.get(i);
            ProductMedia pm = new ProductMedia();
            pm.setProduct(product);
            pm.setMediaAsset(asset);
            pm.setMediaType(asset.getMediaType());
            pm.setPosition(i);
            productMediaRepository.save(pm);
        }
    }

    @Test
    void listingReturnsOnlyActiveProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].slug",
                        contains("laser-cut-coaster-set", "personalized-wooden-keychain")));
    }

    @Test
    void imagesAreOrderedByPositionAndVideoUrlPopulated() throws Exception {
        mockMvc.perform(get("/api/products/personalized-wooden-keychain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images", hasSize(3)))
                .andExpect(jsonPath("$.images[0]").value("https://cdn.supabase.co/public/img/a0.jpg"))
                .andExpect(jsonPath("$.images[1]").value("https://cdn.supabase.co/public/img/a1.jpg"))
                .andExpect(jsonPath("$.images[2]").value("https://cdn.supabase.co/public/img/a2.jpg"))
                .andExpect(jsonPath("$.videoUrl").value("https://cdn.supabase.co/public/vid/a.mp4"));
    }

    @Test
    void detailWithoutVideoHasNullVideoUrlAndEmptyImages() throws Exception {
        mockMvc.perform(get("/api/products/laser-cut-coaster-set"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images", hasSize(0)))
                .andExpect(jsonPath("$.videoUrl").value(nullValue()));
    }

    @Test
    void searchFiltersByNameOrDescriptionCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/products").param("search", "KEYCHAIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].slug").value("personalized-wooden-keychain"));

        mockMvc.perform(get("/api/products").param("search", "family names"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].slug").value("laser-cut-coaster-set"));
    }

    @Test
    void featuredFilterReturnsOnlyFeaturedActiveProducts() throws Exception {
        mockMvc.perform(get("/api/products").param("featured", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].slug").value("personalized-wooden-keychain"));
    }

    @Test
    void paginationReturnsCorrectPageAndMetadata() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].slug").value("personalized-wooden-keychain"));
    }

    @Test
    void detailBySlugReturns200ForActiveProduct() throws Exception {
        mockMvc.perform(get("/api/products/personalized-wooden-keychain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Personalized Wooden Keychain"))
                .andExpect(jsonPath("$.price").value(250.00));
    }

    @Test
    void detailByInactiveOrMissingSlugReturns404() throws Exception {
        mockMvc.perform(get("/api/products/hidden-inactive-item"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/products/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listingRequiresNoAuthToken() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void dtoShapeDoesNotLeakEntities() throws Exception {
        mockMvc.perform(get("/api/products/personalized-wooden-keychain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.objectPath").doesNotExist())
                .andExpect(jsonPath("$.mediaType").doesNotExist())
                .andExpect(jsonPath("$.position").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.bucket").doesNotExist());
    }
}
