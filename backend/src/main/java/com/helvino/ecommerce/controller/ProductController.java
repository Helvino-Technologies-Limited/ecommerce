package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.entity.Product;
import com.helvino.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<Page<Product>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q) {

        Sort sortSpec = switch (sort != null ? sort : "newest") {
            case "price_asc"  -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "popular"    -> Sort.by("salesCount").descending();
            case "rating"     -> Sort.by("averageRating").descending();
            default           -> Sort.by("createdAt").descending();
        };
        PageRequest pageable = PageRequest.of(page, size, sortSpec);

        if (q != null && !q.isBlank() && categoryId != null) {
            return ResponseEntity.ok(productRepository.searchMarketplaceInCategory(q, categoryId, pageable));
        } else if (q != null && !q.isBlank() && category != null && !category.isBlank()) {
            return ResponseEntity.ok(productRepository.searchMarketplaceInCategorySlug(q, category, pageable));
        } else if (q != null && !q.isBlank()) {
            return ResponseEntity.ok(productRepository.searchMarketplace(q, pageable));
        } else if (categoryId != null) {
            return ResponseEntity.ok(productRepository.findMarketplaceByCategory(categoryId, pageable));
        } else if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(productRepository.findMarketplaceByCategorySlug(category, pageable));
        }
        return ResponseEntity.ok(productRepository.findMarketplaceProducts(pageable));
    }

    @GetMapping("/store/{slug}")
    public ResponseEntity<Page<Product>> getStorefront(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productRepository.findByTenantSlug(
                slug, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Product> getProduct(@PathVariable String slug) {
        return productRepository.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/featured")
    public ResponseEntity<List<Product>> getFeatured() {
        return ResponseEntity.ok(productRepository.findMarketplaceFeatured());
    }

    @GetMapping("/flash-sales")
    public ResponseEntity<List<Product>> getFlashSales() {
        return ResponseEntity.ok(productRepository.findMarketplaceFlashSales());
    }

    @GetMapping("/best-sellers")
    public ResponseEntity<List<Product>> getBestSellers() {
        return ResponseEntity.ok(productRepository.findMarketplaceBestSellers(PageRequest.of(0, 10)));
    }

    @GetMapping("/new-arrivals")
    public ResponseEntity<List<Product>> getNewArrivals() {
        return ResponseEntity.ok(productRepository.findMarketplaceNewArrivals(PageRequest.of(0, 10)));
    }
}
