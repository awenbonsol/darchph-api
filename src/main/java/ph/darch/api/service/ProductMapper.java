package ph.darch.api.service;

import org.springframework.stereotype.Component;
import ph.darch.api.dto.ProductResponse;
import ph.darch.api.entity.MediaType;
import ph.darch.api.entity.Product;
import ph.darch.api.entity.ProductMedia;

import java.util.List;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product, List<ProductMedia> media) {
        List<String> images = media.stream()
                .filter(m -> m.getMediaType() == MediaType.IMAGE)
                .sorted(java.util.Comparator.comparingInt(ProductMedia::getPosition))
                .map(m -> m.getMediaAsset().getPublicUrl())
                .toList();

        String videoUrl = media.stream()
                .filter(m -> m.getMediaType() == MediaType.VIDEO)
                .findFirst()
                .map(m -> m.getMediaAsset().getPublicUrl())
                .orElse(null);

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getPrice(),
                product.getCurrency(),
                images,
                videoUrl,
                product.getBuyUrl(),
                product.getIsActive(),
                product.isFeatured(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
