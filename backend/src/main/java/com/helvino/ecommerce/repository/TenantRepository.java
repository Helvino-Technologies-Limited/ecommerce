package com.helvino.ecommerce.repository;

import com.helvino.ecommerce.entity.Tenant;
import com.helvino.ecommerce.entity.User;
import com.helvino.ecommerce.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByOwner(User owner);
    Optional<Tenant> findByOwnerId(UUID ownerId);
    Optional<Tenant> findBySlug(String slug);
    boolean existsByBusinessName(String businessName);
    boolean existsBySlug(String slug);

    List<Tenant> findBySubscriptionStatusIn(List<SubscriptionStatus> statuses);

    @Query("SELECT t FROM Tenant t WHERE " +
           "LOWER(t.businessName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(t.owner.email) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(t.owner.firstName) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Tenant> search(@Param("q") String q, Pageable pageable);

    long countBySubscriptionStatus(SubscriptionStatus status);
}
