package com.helvino.ecommerce.repository;

import com.helvino.ecommerce.entity.Order;
import com.helvino.ecommerce.enums.OrderStatus;
import com.helvino.ecommerce.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);
    Page<Order> findByRiderId(UUID riderId, Pageable pageable);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    List<Order> findByRiderIdAndStatus(UUID riderId, OrderStatus status);

    // Tenant-scoped: orders that contain at least one product from this tenant
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i JOIN i.product p WHERE p.tenant.id = :tenantId")
    Page<Order> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i JOIN i.product p WHERE p.tenant.id = :tenantId AND o.status = :status")
    Page<Order> findByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") OrderStatus status, Pageable pageable);

    // Ownership check before status update
    @Query("SELECT COUNT(i) > 0 FROM OrderItem i WHERE i.order.id = :orderId AND i.product.tenant.id = :tenantId")
    boolean existsItemByOrderIdAndTenantId(@Param("orderId") UUID orderId, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.paymentStatus = 'COMPLETED' " +
           "AND o.createdAt BETWEEN :from AND :to")
    BigDecimal sumRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :from AND :to ORDER BY o.createdAt DESC")
    List<Order> findByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
