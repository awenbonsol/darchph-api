package ph.darch.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ph.darch.api.dto.PagedResponse;
import ph.darch.api.dto.ProductRequest;
import ph.darch.api.dto.ProductResponse;
import ph.darch.api.entity.MediaAsset;
import ph.darch.api.entity.MediaType;
import ph.darch.api.entity.Product;
import ph.darch.api.entity.ProductMedia;
import ph.darch.api.exception.BadRequestException;
import ph.darch.api.exception.ConflictException;
import ph.darch.api.exception.NotFoundException;
import ph.darch.api.repository.MediaAssetRepository;
import ph.darch.api.repository.ProductMediaRepository;
import ph.darch.api.repository.ProductRepository;
import ph.darch.api.util.SlugGenerator;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminProductService {

    private static final int MAX_SIZE = 100;
    private static final Set<String> SORTABLE = Set.of("createdAt", "name", "price");
    private static final Set<String> PATCHABLE = Set.of(
            "name", "slug", "description", "price", "currency",
            "buyUrl", "isActive", "featured");

    private final ProductRepository productRepository;
    private final ProductMediaRepository productMediaRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final ProductMapper productMapper;
    private final SlugGenerator slugGenerator;
    private final MediaCleanupService mediaCleanupService;

    public AdminProductService(ProductRepository productRepository,
                               ProductMediaRepository productMediaRepository,
                               MediaAssetRepository mediaAssetRepository,
                               ProductMapper productMapper,
                               SlugGenerator slugGenerator,
                               MediaCleanupService mediaCleanupService) {
        this.productRepository = productRepository;
        this.productMediaRepository = productMediaRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.productMapper = productMapper;
        this.slugGenerator = slugGenerator;
        this.mediaCleanupService = mediaCleanupService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> list(int page, int size, String sort,
                                               String search, Boolean isActive, Boolean featured) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, resolveSort(sort));

        String term = normalized(search);
        Page<Product> products = productRepository.findAll(
                productSpec(term, isActive, featured), pageable);

        List<Long> ids = products.getContent().stream().map(Product::getId).toList();
        Map<Long, List<ProductMedia>> mediaByProduct = fetchMediaByProduct(ids);
        List<ProductResponse> content = products.getContent().stream()
                .map(p -> productMapper.toResponse(p,
                        mediaByProduct.getOrDefault(p.getId(), List.of())))
                .toList();

        return new PagedResponse<>(content, products.getNumber(), products.getSize(),
                products.getTotalElements(), products.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        List<ProductMedia> media = productMediaRepository
                .findByProductIdOrderByPositionAsc(product.getId());
        return productMapper.toResponse(product, media);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        product.setName(request.name().trim());
        product.setDescription(request.description() == null ? "" : request.description());
        product.setPrice(request.price());
        product.setCurrency(request.currency() == null || request.currency().isBlank()
                ? "PHP" : request.currency());
        product.setBuyUrl(request.buyUrl());
        product.setIsActive(request.isActive() == null || request.isActive());
        product.setFeatured(request.featured() != null && request.featured());

        product.setSlug(resolveSlug(request, null));

        Product saved = productRepository.save(product);
        rebuildMedia(saved, request);
        return productMapper.toResponse(saved,
                productMediaRepository.findByProductIdOrderByPositionAsc(saved.getId()));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        product.setName(request.name().trim());
        product.setDescription(request.description() == null ? "" : request.description());
        product.setPrice(request.price());
        product.setCurrency(request.currency() == null || request.currency().isBlank()
                ? "PHP" : request.currency());
        product.setBuyUrl(request.buyUrl());
        product.setIsActive(request.isActive() == null || request.isActive());
        product.setFeatured(request.featured() != null && request.featured());

        product.setSlug(resolveSlug(request, product.getId()));

        Product saved = productRepository.save(product);
        rebuildMedia(saved, request);
        return productMapper.toResponse(saved,
                productMediaRepository.findByProductIdOrderByPositionAsc(saved.getId()));
    }

    @Transactional
    public ProductResponse patch(Long id, Map<String, Object> fields) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        applyPatch(product, fields);
        product.setSlug(resolvePatchedSlug(product));
        productRepository.save(product);
        return productMapper.toResponse(product,
                productMediaRepository.findByProductIdOrderByPositionAsc(product.getId()));
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        List<ProductMedia> media = productMediaRepository
                .findByProductIdOrderByPositionAsc(product.getId());
        List<MediaAsset> assets = media.stream().map(ProductMedia::getMediaAsset).toList();
        productMediaRepository.deleteAll(media);
        productMediaRepository.flush();
        productRepository.delete(product);
        mediaCleanupService.deleteAssets(assets);
    }

    // ----- helpers -----

    private Specification<Product> productSpec(String term, Boolean isActive, Boolean featured) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (term != null) {
                String like = "%" + term.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }
            if (featured != null) {
                predicates.add(cb.equal(root.get("featured"), featured));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void applyPatch(Product product, Map<String, Object> fields) {
        if (fields == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String key = entry.getKey();
            if (!PATCHABLE.contains(key) || entry.getValue() == null) {
                continue;
            }
            switch (key) {
                case "name" -> product.setName(requireText(key, entry.getValue(), 200));
                case "description" -> product.setDescription(entry.getValue().toString());
                case "price" -> product.setPrice(requirePrice(entry.getValue()));
                case "currency" -> product.setCurrency(requireCurrency(entry.getValue()));
                case "buyUrl" -> product.setBuyUrl(requireUrl(key, entry.getValue()));
                case "isActive" -> product.setIsActive(requireBoolean(key, entry.getValue()));
                case "featured" -> product.setFeatured(requireBoolean(key, entry.getValue()));
                default -> throw new BadRequestException("Unsupported field: " + key);
            }
        }
    }

    private String resolveSlug(ProductRequest request, Long currentId) {
        if (request.slug() != null && !request.slug().isBlank()) {
            String slug = request.slug().trim();
            if (isTaken(slug, currentId)) {
                throw new ConflictException("Slug already in use: " + slug);
            }
            return slug;
        }
        return generateUniqueSlug(request.name(), currentId);
    }

    private String resolvePatchedSlug(Product product) {
        return generateUniqueSlug(product.getName(), product.getId());
    }

    private String generateUniqueSlug(String name, Long currentId) {
        String base = slugGenerator.slugify(name);
        if (base.isEmpty()) {
            base = "item";
        }
        String candidate = base;
        int n = 1;
        while (isTaken(candidate, currentId)) {
            n += 1;
            candidate = base + "-" + n;
        }
        return candidate;
    }

    private boolean isTaken(String slug, Long currentId) {
        return productRepository.findBySlug(slug)
                .map(existing -> currentId == null || !existing.getId().equals(currentId))
                .orElse(false);
    }

    private void rebuildMedia(Product product, ProductRequest request) {
        List<ProductMedia> existing = productMediaRepository
                .findByProductIdOrderByPositionAsc(product.getId());
        List<MediaAsset> removedAssets = new ArrayList<>();

        List<MediaAsset> imageAssets = resolveAll(request.images());
        MediaAsset videoAsset = request.videoUrl() == null || request.videoUrl().isBlank()
                ? null
                : resolveOne(request.videoUrl());

        long videoCount = imageAssets.stream()
                .filter(a -> a.getMediaType() == MediaType.VIDEO)
                .count()
                + (videoAsset != null && videoAsset.getMediaType() == MediaType.VIDEO ? 1 : 0);
        if (videoCount > 1) {
            Map<String, String> details = new LinkedHashMap<>();
            details.put("media", "At most one video is allowed per product");
            throw new BadRequestException(details);
        }

        if (existing.isEmpty()) {
            int position = 0;
            for (MediaAsset asset : imageAssets) {
                productMediaRepository.save(mediaRow(product, asset, position++));
            }
            if (videoAsset != null) {
                productMediaRepository.save(mediaRow(product, videoAsset, position));
            }
            return;
        }

        productMediaRepository.deleteAll(existing);
        productMediaRepository.flush();

        for (ProductMedia removed : existing) {
            MediaAsset removedAsset = removed.getMediaAsset();
            boolean reused = imageAssets.contains(removedAsset)
                    || (videoAsset != null && videoAsset.getId().equals(removedAsset.getId()));
            if (!reused) {
                removedAssets.add(removedAsset);
            }
        }

        int position = 0;
        for (MediaAsset asset : imageAssets) {
            productMediaRepository.save(mediaRow(product, asset, position++));
        }
        if (videoAsset != null) {
            productMediaRepository.save(mediaRow(product, videoAsset, position));
        }

        mediaCleanupService.deleteAssets(removedAssets);
    }

    private ProductMedia mediaRow(Product product, MediaAsset asset, int position) {
        ProductMedia row = new ProductMedia();
        row.setProduct(product);
        row.setMediaAsset(asset);
        row.setMediaType(asset.getMediaType());
        row.setPosition(position);
        return row;
    }

    private List<MediaAsset> resolveAll(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return List.of();
        }
        List<MediaAsset> assets = new ArrayList<>();
        for (String url : urls) {
            assets.add(resolveOne(url));
        }
        return assets;
    }

    private MediaAsset resolveOne(String url) {
        return mediaAssetRepository.findByPublicUrl(url)
                .orElseThrow(() -> badMedia(url));
    }

    private BadRequestException badMedia(String url) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("media", "URL was not uploaded through the API: " + url);
        return new BadRequestException(details);
    }

    private String requireText(String field, Object value, int max) {
        String s = value.toString().trim();
        if (s.isEmpty()) {
            throw disallow(field, field + " cannot be blank");
        }
        if (s.length() > max) {
            throw disallow(field, field + " must be at most " + max + " characters");
        }
        return s;
    }

    private BigDecimal requirePrice(Object value) {
        try {
            BigDecimal price = new BigDecimal(value.toString());
            if (price.signum() < 0) {
                throw disallow("price", "price must be zero or positive");
            }
            return price;
        } catch (NumberFormatException e) {
            throw disallow("price", "price must be a valid number");
        }
    }

    private String requireCurrency(Object value) {
        String s = value.toString();
        if (s.length() != 3) {
            throw disallow("currency", "currency must be a 3-letter code");
        }
        return s;
    }

    private String requireUrl(String field, Object value) {
        String s = value.toString().trim();
        if (!s.matches("^https?://.+")) {
            throw disallow(field, field + " must be a valid http(s) URL");
        }
        return s;
    }

    private boolean requireBoolean(String field, Object value) {
        Object v = value;
        if (v instanceof String str) {
            v = Boolean.parseBoolean(str.trim());
        }
        if (!(v instanceof Boolean b)) {
            throw disallow(field, field + " must be a boolean");
        }
        return b;
    }

    private BadRequestException disallow(String field, String message) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put(field, message);
        return new BadRequestException(details);
    }

    private Map<Long, List<ProductMedia>> fetchMediaByProduct(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return productMediaRepository
                .findByProductIdInOrderByProductIdAscPositionAsc(ids)
                .stream()
                .collect(java.util.stream.Collectors.groupingBy(pm -> pm.getProduct().getId()));
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
