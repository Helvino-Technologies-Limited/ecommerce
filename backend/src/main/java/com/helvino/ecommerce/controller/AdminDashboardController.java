package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.enums.OrderStatus;
import com.helvino.ecommerce.enums.UserRole;
import com.helvino.ecommerce.repository.OrderRepository;
import com.helvino.ecommerce.repository.ProductRepository;
import com.helvino.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        if (from == null) from = LocalDateTime.now().minusDays(30);
        if (to == null) to = LocalDateTime.now();

        Map<String, Object> stats = new HashMap<>();

        // Revenue
        BigDecimal revenue = orderRepository.sumRevenue(from, to);
        stats.put("revenue", revenue != null ? revenue : BigDecimal.ZERO);

        // Orders breakdown
        stats.put("totalOrders", orderRepository.count());
        stats.put("pendingOrders", orderRepository.countByStatus(OrderStatus.PENDING));
        stats.put("processingOrders", orderRepository.countByStatus(OrderStatus.PROCESSING));
        stats.put("deliveredOrders", orderRepository.countByStatus(OrderStatus.DELIVERED));
        stats.put("cancelledOrders", orderRepository.countByStatus(OrderStatus.CANCELLED));

        // Products
        stats.put("totalProducts", productRepository.count());

        // Customers
        stats.put("totalCustomers", userRepository.findByRole(
                UserRole.CUSTOMER, org.springframework.data.domain.Pageable.unpaged()).getTotalElements());

        // Recent orders
        stats.put("recentOrders", orderRepository.findByDateRange(from, to));

        return ResponseEntity.ok(stats);
    }
}
