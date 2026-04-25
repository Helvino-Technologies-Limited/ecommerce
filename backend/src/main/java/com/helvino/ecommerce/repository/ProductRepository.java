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

    // ── Single product lookup ──────────────────────────────────────────────
    Optional<Product> findBySlug(String slug);
    Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);

    // ── Marketplace (public storefront) ───────────────────────────────────
    // Only products from Helvino (tenant IS NULL) or active/trial tenants
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(p.tenant IS NULL OR p.tenant.subscriptionStatus IN ('TRIAL','ACTIVE'))")
    Page<Product> findMarketplaceProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category.id = :categoryId AND " +
           "(p.tenant IS NULL OR p.tenant.subscriptionStatus IN ('TRIAL','ACTIVE'))")
    Page<Product> findMarketplaceByCategory(@Param("categoryId") UUID categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category.slug = :slug AND " +
           "(p.tenant IS NULL OR p.tenant.subscriptionStatus IN ('TRIAL','ACTIVE'))")
    Page<Product> findMarketplaceByCategorySlug(@Param("slug") String slug, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(p.tenant IS NULL OR p.tenant.subscriptionStatus IN ('TRIAL','ACTIVE')) AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Product> searchMarketplace(@Param("q") String q, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category.id = :categoryId AND " +
           "(p.tenant IS NULL OR p.tenant.subscriptionStatus IN ('TRIAL','ACTIVE')) AND " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Product> searchMarketplaceInCategory(@Param("q") String q,
                                               @Param("categoryId") UUID categoryId,
                                               Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category.slug = :slug AND " +
           "(p.tenant IS NULL OR p.tenant.subscriptionStatus IN ('TRIAL','ACTIVE')) AND " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Product> searchMarketplaceInCategorySlug(@Param("q") String q,
                                                   @Param("slug") String slug,
                                                   Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.featured = true AND " +
           "(p.tenant IS NULL OR p.tenant.subscriptionStatus IN ('TRIAL','ACTIVE'))")
    List<Product> findMarketplaceFeatured();

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.flashSale = true AND " +
           "(p.tenant IS NULL OR p.tenant.subscriptionStatus IN ('TRIAL','ACTIVE')) " +
           "ORDER BY p.flashSaleEndsAt ASC")
    List<Product> findMarketplaceFlashSales();

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(p.tenant IS NULL OR p.tenant.subscriptionStatus IN ('TRIAL','ACTIVE')) " +
           "ORDER BY p.salesCount DESC")
    List<Product> findMarketplaceBestSellers(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(p.tenant IS NULL OR p.tenant.subscriptionStatus IN ('TRIAL','ACTIVE')) " +
           "ORDER BY p.createdAt DESC")
    List<Product> findMarketplaceNewArrivals(Pageable pageable);

    // Per-tenant storefront (public — only for active/trial tenant)
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.tenant.slug = :slug AND " +
           "p.tenant.subscriptionStatus IN ('TRIAL','ACTIVE')")
    Page<Product> findByTenantSlug(@Param("slug") String slug, Pageable pageable);

    // ── Admin — tenant-scoped ─────────────────────────────────────────────
    Page<Product> findByTenantId(UUID tenantId, Pageable pageable);
    long countByTenantId(UUID tenantId);

    @Query("SELECT p FROM Product p WHERE p.tenant.id = :tenantId AND p.active = true AND " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Product> searchInTenant(@Param("q") String q, @Param("tenantId") UUID tenantId, Pageable pageable);
}
