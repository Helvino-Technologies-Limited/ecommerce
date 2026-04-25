package com.helvino.ecommerce.repository;

import com.helvino.ecommerce.entity.SubscriptionPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPayment, UUID> {
    List<SubscriptionPayment> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
