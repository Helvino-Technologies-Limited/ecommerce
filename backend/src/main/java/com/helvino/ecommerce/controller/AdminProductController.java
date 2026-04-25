package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.entity.Category;
import com.helvino.ecommerce.entity.Product;
import com.helvino.ecommerce.entity.Tenant;
import com.helvino.ecommerce.entity.User;
import com.helvino.ecommerce.enums.Currency;
import com.helvino.ecommerce.enums.UserRole;
import com.helvino.ecommerce.repository.CategoryRepository;
import com.helvino.ecommerce.repository.ProductRepository;
import com.helvino.ecommerce.repository.TenantRepository;
import com.helvino.ecommerce.security.TenantContext;
import com.helvino.ecommerce.service.TenantSubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/products")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final TenantRepository tenantRepository;
    private final TenantSubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<Page<Product>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User caller,
            HttpServletRequest request) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (caller.getRole() == UserRole.SUPER_ADMIN) {
            return ResponseEntity.ok(productRepository.findAll(pageable));
        }
        // ADMIN: only their tenant's products
        UUID tenantId = TenantContext.getTenantId(request);
        if (tenantId == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(productRepository.findByTenantId(tenantId, pageable));
    }

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody ProductRequest req,
            @AuthenticationPrincipal User caller,
            HttpServletRequest request) {

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Tenant tenant = null;
        if (caller.getRole() == UserRole.ADMIN) {
            UUID tenantId = TenantContext.getTenantId(request);
            if (tenantId == null) return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tenant associated with your account"));
            tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
            if (!subscriptionService.hasSellerAccess(tenant)) {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                        .body(Map.of(
                                "message", "Your subscription has expired. Pay KSh 500 via Paybill 522533, Account 8071524 to continue selling.",
                                "paybill", TenantSubscriptionService.PAYBILL,
                                "account", TenantSubscriptionService.ACCOUNT
                        ));
            }
        }
        // SUPER_ADMIN: tenant = null (Helvino's own product)

        Product product = Product.builder()
                .name(req.getName())
                .slug(generateSlug(req.getName()))
                .description(req.getDescription())
                .shortDescription(req.getShortDescription())
                .price(req.getPrice())
                .compareAtPrice(req.getCompareAtPrice())
                .currency(req.getCurrency() != null ? req.getCurrency() : Currency.KES)
                .stockQuantity(req.getStockQuantity() != null ? req.getStockQuantity() : 0)
                .sku(req.getSku())
                .category(category)
                .tenant(tenant)
                .images(req.getImages() != null ? req.getImages() : List.of())
                .featured(req.isFeatured())
                .flashSale(req.isFlashSale())
                .flashSaleDiscount(req.getFlashSaleDiscount())
                .flashSaleEndsAt(req.getFlashSaleEndsAt())
                .tags(req.getTags() != null ? req.getTags() : List.of())
                .active(true)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(productRepository.save(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable UUID id,
            @RequestBody ProductRequest req,
            @AuthenticationPrincipal User caller,
            HttpServletRequest request) {

        Product product = findOwnedProduct(id, caller, request);
        if (product == null) return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Product not found in your store"));

        if (req.getName() != null) {
            product.setName(req.getName());
            product.setSlug(generateSlug(req.getName()));
        }
        if (req.getDescription() != null) product.setDescription(req.getDescription());
        if (req.getShortDescription() != null) product.setShortDescription(req.getShortDescription());
        if (req.getPrice() != null) product.setPrice(req.getPrice());
        if (req.getCompareAtPrice() != null) product.setCompareAtPrice(req.getCompareAtPrice());
        if (req.getCurrency() != null) product.setCurrency(req.getCurrency());
        if (req.getStockQuantity() != null) product.setStockQuantity(req.getStockQuantity());
        if (req.getSku() != null) product.setSku(req.getSku());
        if (req.getImages() != null) product.setImages(req.getImages());
        if (req.getTags() != null) product.setTags(req.getTags());
        if (req.getCategoryId() != null) {
            categoryRepository.findById(req.getCategoryId()).ifPresent(product::setCategory);
        }
        product.setFeatured(req.isFeatured());
        product.setFlashSale(req.isFlashSale());
        if (req.getFlashSaleDiscount() != null) product.setFlashSaleDiscount(req.getFlashSaleDiscount());
        if (req.getFlashSaleEndsAt() != null) product.setFlashSaleEndsAt(req.getFlashSaleEndsAt());

        return ResponseEntity.ok(productRepository.save(product));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(
            @PathVariable UUID id,
            @AuthenticationPrincipal User caller,
            HttpServletRequest request) {
        Product product = findOwnedProduct(id, caller, request);
        if (product == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        product.setActive(!product.isActive());
        return ResponseEntity.ok(productRepository.save(product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User caller,
            HttpServletRequest request) {
        Product product = findOwnedProduct(id, caller, request);
        if (product == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        productRepository.delete(product);
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Product findOwnedProduct(UUID id, User caller, HttpServletRequest request) {
        if (caller.getRole() == UserRole.SUPER_ADMIN) {
            return productRepository.findById(id).orElse(null);
        }
        UUID tenantId = TenantContext.getTenantId(request);
        if (tenantId == null) return null;
        return productRepository.findByIdAndTenantId(id, tenantId).orElse(null);
    }

    private String generateSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        String slug = base + "-" + UUID.randomUUID().toString().substring(0, 8);
        return slug;
    }

    @Data
    public static class ProductRequest {
        private String name;
        private String description;
        private String shortDescription;
        private BigDecimal price;
        private BigDecimal compareAtPrice;
        private Currency currency;
        private Integer stockQuantity;
        private String sku;
        private UUID categoryId;
        private List<String> images;
        private boolean featured;
        private boolean flashSale;
        private BigDecimal flashSaleDiscount;
        private LocalDateTime flashSaleEndsAt;
        private List<String> tags;
    }
}
