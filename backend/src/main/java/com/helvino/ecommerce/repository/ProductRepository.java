package com.helvino.ecommerce.repository;

import com.helvino.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySlug(String slug);
    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);
    Page<Product> findByActiveTrue(Pageable pageable);
    List<Product> findByFeaturedTrueAndActiveTrue();
    List<Product> findByFlashSaleTrueAndActiveTrueOrderByFlashSaleEndsAtAsc();

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Product> search(@Param("q") String q, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category.id = :categoryId AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Product> searchInCategory(@Param("q") String q, @Param("categoryId") UUID categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category.slug = :slug")
    Page<Product> findByCategorySlug(@Param("slug") String slug, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category.slug = :slug AND " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Product> searchInCategoryBySlug(@Param("q") String q, @Param("slug") String slug, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.salesCount DESC")
    List<Product> findBestSellers(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.createdAt DESC")
    List<Product> findNewArrivals(Pageable pageable);
}
