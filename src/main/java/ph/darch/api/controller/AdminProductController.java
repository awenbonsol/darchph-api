package ph.darch.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ph.darch.api.dto.PagedResponse;
import ph.darch.api.dto.ProductRequest;
import ph.darch.api.dto.ProductResponse;
import ph.darch.api.service.AdminProductService;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@Tag(name = "Admin Products", description = "Admin CRUD for products (JWT required)")
@SecurityRequirement(name = "bearerAuth")
public class AdminProductController {

    private final AdminProductService adminProductService;

    public AdminProductController(AdminProductService adminProductService) {
        this.adminProductService = adminProductService;
    }

    @Operation(summary = "List all products (incl. inactive)")
    @GetMapping
    public PagedResponse<ProductResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean featured) {
        return adminProductService.list(page, size, sort, search, isActive, featured);
    }

    @Operation(summary = "Get a single product by id")
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return adminProductService.get(id);
    }

    @Operation(summary = "Create a product")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return adminProductService.create(request);
    }

    @Operation(summary = "Fully replace a product (media replaced in order)")
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return adminProductService.update(id, request);
    }

    @Operation(summary = "Partially update a product (e.g. isActive/featured/price)")
    @PatchMapping("/{id}")
    public ProductResponse patch(@PathVariable Long id, @RequestBody Map<String, Object> fields) {
        return adminProductService.patch(id, fields);
    }

    @Operation(summary = "Delete a product and its media")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        adminProductService.delete(id);
    }
}
