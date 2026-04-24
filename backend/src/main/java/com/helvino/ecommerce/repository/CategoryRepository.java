package com.helvino.ecommerce.repository;

import com.helvino.ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);
    List<Category> findByParentIsNullAndActiveTrue();
    List<Category> findByParentIdAndActiveTrue(UUID parentId);
}
