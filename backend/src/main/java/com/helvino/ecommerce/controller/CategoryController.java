package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.entity.Category;
import com.helvino.ecommerce.repository.CategoryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAll() {
        return ResponseEntity.ok(categoryRepository.findByParentIsNullAndActiveTrue());
    }

    @GetMapping("/categories/{id}/children")
    public ResponseEntity<List<Category>> getChildren(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryRepository.findByParentIdAndActiveTrue(id));
    }

    @PostMapping("/admin/categories")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Category> create(@RequestBody CategoryRequest req) {
        Category category = Category.builder()
                .name(req.getName())
                .slug(req.getName().toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-"))
                .description(req.getDescription())
                .imageUrl(req.getImageUrl())
                .active(true)
                .build();

        if (req.getParentId() != null) {
            categoryRepository.findById(req.getParentId()).ifPresent(category::setParent);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(categoryRepository.save(category));
    }

    @PutMapping("/admin/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Category> update(@PathVariable UUID id, @RequestBody CategoryRequest req) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        if (req.getName() != null) category.setName(req.getName());
        if (req.getDescription() != null) category.setDescription(req.getDescription());
        if (req.getImageUrl() != null) category.setImageUrl(req.getImageUrl());
        return ResponseEntity.ok(categoryRepository.save(category));
    }

    @Data
    public static class CategoryRequest {
        private String name;
        private String description;
        private String imageUrl;
        private UUID parentId;
    }
}
