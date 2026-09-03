package ph.darch.api.service;

import org.junit.jupiter.api.Test;
import ph.darch.api.dto.ProductRequest;
import ph.darch.api.entity.MediaAsset;
import ph.darch.api.entity.MediaType;
import ph.darch.api.entity.Product;
import ph.darch.api.entity.ProductMedia;
import ph.darch.api.exception.BadRequestException;
import ph.darch.api.repository.MediaAssetRepository;
import ph.darch.api.repository.ProductMediaRepository;
import ph.darch.api.repository.ProductRepository;
import ph.darch.api.util.SlugGenerator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminProductServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductMediaRepository productMediaRepository = mock(ProductMediaRepository.class);
    private final MediaAssetRepository mediaAssetRepository = mock(MediaAssetRepository.class);
    private final ProductMapper productMapper = new ProductMapper();
    private final SlugGenerator slugGenerator = new SlugGenerator();
    private final MediaCleanupService mediaCleanupService = mock(MediaCleanupService.class);

    private final AdminProductService service = new AdminProductService(
            productRepository, productMediaRepository, mediaAssetRepository,
            productMapper, slugGenerator, mediaCleanupService);

    private MediaAsset asset(MediaType type, String url) {
        MediaAsset a = new MediaAsset();
        a.setId(10L + (long) url.hashCode() % 100);
        a.setMediaType(type);
        a.setBucket(type == MediaType.IMAGE ? "product-images" : "product-videos");
        a.setObjectPath(url + "-obj");
        a.setPublicUrl(url);
        return a;
    }

    @Test
    void createPersistsOrderedMediaForImageAndVideo() {
        MediaAsset img = asset(MediaType.IMAGE, "https://cdn/x/a.jpg");
        MediaAsset vid = asset(MediaType.VIDEO, "https://cdn/x/b.mp4");
        when(mediaAssetRepository.findByPublicUrl("https://cdn/x/a.jpg")).thenReturn(Optional.of(img));
        when(mediaAssetRepository.findByPublicUrl("https://cdn/x/b.mp4")).thenReturn(Optional.of(vid));
        when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());

        Product saved = new Product();
        saved.setId(1L);
        saved.setName("Test");
        saved.setPrice(BigDecimal.ONE);
        saved.setBuyUrl("https://e.test");
        when(productRepository.save(any(Product.class))).thenReturn(saved);
        when(productMediaRepository.findByProductIdOrderByPositionAsc(1L)).thenReturn(List.of());

        ProductRequest req = new ProductRequest(
                "Test", null, "", BigDecimal.ONE, "PHP", "https://e.test",
                List.of("https://cdn/x/a.jpg"), "https://cdn/x/b.mp4", true, false);

        service.create(req);

        org.mockito.ArgumentCaptor<ProductMedia> captor =
                org.mockito.ArgumentCaptor.forClass(ProductMedia.class);
        org.mockito.Mockito.verify(productMediaRepository, org.mockito.Mockito.times(2))
                .save(captor.capture());

        List<ProductMedia> rows = captor.getAllValues();
        assertThat(rows).extracting(ProductMedia::getMediaType)
                .containsExactly(MediaType.IMAGE, MediaType.VIDEO);
        assertThat(rows).extracting(ProductMedia::getPosition)
                .containsExactly(0, 1);
    }

    @Test
    void secondVideoThrowsBadRequest() {
        MediaAsset vid1 = asset(MediaType.VIDEO, "https://cdn/x/v1.mp4");
        MediaAsset vid2 = asset(MediaType.VIDEO, "https://cdn/x/v2.mp4");
        when(mediaAssetRepository.findByPublicUrl("https://cdn/x/v1.mp4")).thenReturn(Optional.of(vid1));
        when(mediaAssetRepository.findByPublicUrl("https://cdn/x/v2.mp4")).thenReturn(Optional.of(vid2));
        when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        Product saved = savedProduct();
        when(productRepository.save(any(Product.class))).thenReturn(saved);
        when(productMediaRepository.findByProductIdOrderByPositionAsc(saved.getId())).thenReturn(List.of());

        ProductRequest req = new ProductRequest(
                "Test", null, "", BigDecimal.ONE, "PHP", "https://e.test",
                List.of("https://cdn/x/v1.mp4"), "https://cdn/x/v2.mp4", true, false);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .satisfies(ex -> assertThat(((BadRequestException) ex).getDetails())
                        .containsKey("media"));
    }

    @Test
    void unuploadedImageUrlThrowsBadRequest() {
        when(mediaAssetRepository.findByPublicUrl("https://evil/x.jpg")).thenReturn(Optional.empty());
        when(productRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        Product saved = savedProduct();
        when(productRepository.save(any(Product.class))).thenReturn(saved);
        when(productMediaRepository.findByProductIdOrderByPositionAsc(saved.getId())).thenReturn(List.of());

        ProductRequest req = new ProductRequest(
                "Test", null, "", BigDecimal.ONE, "PHP", "https://e.test",
                List.of("https://evil/x.jpg"), null, true, false);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class);
    }

    private Product savedProduct() {
        Product p = new Product();
        p.setId(1L);
        p.setName("Test");
        p.setPrice(BigDecimal.ONE);
        p.setBuyUrl("https://e.test");
        return p;
    }
}
