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

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.paymentStatus = 'COMPLETED' " +
           "AND o.createdAt BETWEEN :from AND :to")
    BigDecimal sumRevenue(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :from AND :to ORDER BY o.createdAt DESC")
    List<Order> findByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
