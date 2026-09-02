package ph.darch.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ph.darch.api.dto.PagedResponse;
import ph.darch.api.dto.ProductResponse;
import ph.darch.api.entity.Product;
import ph.darch.api.entity.ProductMedia;
import ph.darch.api.exception.NotFoundException;
import ph.darch.api.repository.ProductMediaRepository;
import ph.darch.api.repository.ProductRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final int MAX_SIZE = 100;
    private static final Set<String> SORTABLE = Set.of("createdAt", "name", "price");

    private final ProductRepository productRepository;
    private final ProductMediaRepository productMediaRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository,
                          ProductMediaRepository productMediaRepository,
                          ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMediaRepository = productMediaRepository;
        this.productMapper = productMapper;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> list(int page,
                                               int size,
                                               String sort,
                                               String search,
                                               Boolean featured) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, resolveSort(sort));

        String term = normalized(search);
        boolean onlyFeatured = Boolean.TRUE.equals(featured);

        Page<Product> products;
        if (term == null) {
            products = onlyFeatured
                    ? productRepository.findByIsActiveTrueAndFeaturedTrue(pageable)
                    : productRepository.findByIsActiveTrue(pageable);
        } else {
            products = onlyFeatured
                    ? productRepository.searchActiveFeaturedByTerm(term, pageable)
                    : productRepository.searchActiveByTerm(term, pageable);
        }

        Map<Long, List<ProductMedia>> mediaByProduct = fetchMediaByProduct(products.getContent());
        List<ProductResponse> content = products.getContent().stream()
                .map(p -> productMapper.toResponse(p,
                        mediaByProduct.getOrDefault(p.getId(), List.of())))
                .toList();

        return new PagedResponse<>(content, products.getNumber(), products.getSize(),
                products.getTotalElements(), products.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProductResponse getBySlug(String slug) {
        Product product = productRepository.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        List<ProductMedia> media = productMediaRepository
                .findByProductIdOrderByPositionAsc(product.getId());
        return productMapper.toResponse(product, media);
    }

    private Map<Long, List<ProductMedia>> fetchMediaByProduct(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = products.stream().map(Product::getId).toList();
        return productMediaRepository.findByProductIdInOrderByProductIdAscPositionAsc(ids)
                .stream()
                .collect(Collectors.groupingBy(pm -> pm.getProduct().getId()));
    }

    private String normalized(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim();
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sort.split(",");
        String property = parts[0].trim();
        if (!SORTABLE.contains(property)) {
            property = "createdAt";
        }
        Sort.Direction direction = parts.length > 1
                && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}
