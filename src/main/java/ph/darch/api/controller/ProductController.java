package ph.darch.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ph.darch.api.dto.PagedResponse;
import ph.darch.api.dto.ProductResponse;
import ph.darch.api.service.ProductService;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Public storefront, no token required")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "List active products (paginated/searchable)")
    @GetMapping
    public PagedResponse<ProductResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean featured) {
        return productService.list(page, size, sort, search, featured);
    }

    @Operation(summary = "Get an active product by slug")
    @GetMapping("/{slug}")
    public ProductResponse getBySlug(@PathVariable String slug) {
        return productService.getBySlug(slug);
    }
}
