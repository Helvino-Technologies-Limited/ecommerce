package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.entity.Product;
import com.helvino.ecommerce.entity.Category;
import com.helvino.ecommerce.enums.Currency;
import com.helvino.ecommerce.repository.CategoryRepository;
import com.helvino.ecommerce.repository.ProductRepository;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/products")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<Page<Product>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productRepository.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest req) {
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Product product = Product.builder()
                .name(req.getName())
                .slug(generateSlug(req.getName()))
                .description(req.getDescription())
                .shortDescription(req.getShortDescription())
                .price(req.getPrice())
                .compareAtPrice(req.getCompareAtPrice())
                .currency(req.getCurrency() != null ? req.getCurrency() : Currency.KES)
                .stockQuantity(req.getStockQuantity())
                .sku(req.getSku())
                .category(category)
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
    public ResponseEntity<Product> update(@PathVariable UUID id, @RequestBody ProductRequest req) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (req.getName() != null) { product.setName(req.getName()); }
        if (req.getDescription() != null) { product.setDescription(req.getDescription()); }
        if (req.getPrice() != null) { product.setPrice(req.getPrice()); }
        if (req.getCurrency() != null) { product.setCurrency(req.getCurrency()); }
        if (req.getStockQuantity() != null) { product.setStockQuantity(req.getStockQuantity()); }
        if (req.getImages() != null) { product.setImages(req.getImages()); }
        product.setFeatured(req.isFeatured());
        product.setFlashSale(req.isFlashSale());
        if (req.getFlashSaleDiscount() != null) { product.setFlashSaleDiscount(req.getFlashSaleDiscount()); }
        if (req.getFlashSaleEndsAt() != null) { product.setFlashSaleEndsAt(req.getFlashSaleEndsAt()); }

        return ResponseEntity.ok(productRepository.save(product));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Product> toggle(@PathVariable UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setActive(!product.isActive());
        return ResponseEntity.ok(productRepository.save(product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String generateSlug(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                + "-" + UUID.randomUUID().toString().substring(0, 8);
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
