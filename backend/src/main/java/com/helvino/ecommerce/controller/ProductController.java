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
import java.util.Map;
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
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "popular" -> Sort.by("salesCount").descending();
            case "rating" -> Sort.by("averageRating").descending();
            default -> Sort.by("createdAt").descending();
        };

        PageRequest pageable = PageRequest.of(page, size, sortSpec);

        if (q != null && !q.isBlank() && categoryId != null) {
            return ResponseEntity.ok(productRepository.searchInCategory(q, categoryId, pageable));
        } else if (q != null && !q.isBlank() && category != null && !category.isBlank()) {
            return ResponseEntity.ok(productRepository.searchInCategoryBySlug(q, category, pageable));
        } else if (q != null && !q.isBlank()) {
            return ResponseEntity.ok(productRepository.search(q, pageable));
        } else if (categoryId != null) {
            return ResponseEntity.ok(productRepository.findByCategoryId(categoryId, pageable));
        } else if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(productRepository.findByCategorySlug(category, pageable));
        }
        return ResponseEntity.ok(productRepository.findByActiveTrue(pageable));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Product> getProduct(@PathVariable String slug) {
        return productRepository.findBySlug(slug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/featured")
    public ResponseEntity<List<Product>> getFeatured() {
        return ResponseEntity.ok(productRepository.findByFeaturedTrueAndActiveTrue());
    }

    @GetMapping("/flash-sales")
    public ResponseEntity<List<Product>> getFlashSales() {
        return ResponseEntity.ok(productRepository.findByFlashSaleTrueAndActiveTrueOrderByFlashSaleEndsAtAsc());
    }

    @GetMapping("/best-sellers")
    public ResponseEntity<List<Product>> getBestSellers() {
        return ResponseEntity.ok(productRepository.findBestSellers(PageRequest.of(0, 10)));
    }

    @GetMapping("/new-arrivals")
    public ResponseEntity<List<Product>> getNewArrivals() {
        return ResponseEntity.ok(productRepository.findNewArrivals(PageRequest.of(0, 10)));
    }
}
