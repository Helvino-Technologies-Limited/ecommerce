package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.entity.Order;
import com.helvino.ecommerce.entity.OrderTracking;
import com.helvino.ecommerce.entity.User;
import com.helvino.ecommerce.enums.OrderStatus;
import com.helvino.ecommerce.repository.OrderRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/rider")
@PreAuthorize("hasRole('RIDER')")
@RequiredArgsConstructor
public class RiderController {

    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getOrders(
            @AuthenticationPrincipal User rider,
            @RequestParam(defaultValue = "pending") String status) {

        return switch (status) {
            case "active" -> ResponseEntity.ok(orderRepository.findByRiderIdAndStatus(
                    rider.getId(), OrderStatus.OUT_FOR_DELIVERY));
            case "pending" -> ResponseEntity.ok(orderRepository
                    .findByStatus(OrderStatus.CONFIRMED, org.springframework.data.domain.Pageable.unpaged())
                    .getContent());
            default -> ResponseEntity.ok(orderRepository.findByRiderId(
                    rider.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent());
        };
    }

    @PostMapping("/orders/{id}/accept")
    public ResponseEntity<Order> acceptOrder(@AuthenticationPrincipal User rider, @PathVariable UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setRider(rider);
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setEstimatedDelivery(LocalDateTime.now().plusHours(2));

        addTracking(order, OrderStatus.OUT_FOR_DELIVERY, "Rider " + rider.getFullName() + " has picked up your order");
        notifyCustomer(order.getCustomer().getId().toString(), "Your order is out for delivery!");

        return ResponseEntity.ok(orderRepository.save(order));
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<Order> updateStatus(
            @AuthenticationPrincipal User rider,
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!rider.getId().equals(order.getRider().getId())) {
            return ResponseEntity.status(403).build();
        }

        OrderStatus newStatus = OrderStatus.valueOf(body.get("status"));
        order.setStatus(newStatus);
        addTracking(order, newStatus, body.getOrDefault("message", "Status updated to " + newStatus));
        notifyCustomer(order.getCustomer().getId().toString(), "Order update: " + newStatus);

        return ResponseEntity.ok(orderRepository.save(order));
    }

    @PostMapping("/orders/{id}/location")
    public ResponseEntity<Void> updateLocation(
            @AuthenticationPrincipal User rider,
            @PathVariable UUID id,
            @RequestBody LocationUpdate loc) {

        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) return ResponseEntity.notFound().build();

        // Broadcast rider location via WebSocket
        messagingTemplate.convertAndSend(
                "/topic/orders/" + id + "/location",
                Map.of("latitude", loc.getLatitude(), "longitude", loc.getLongitude(),
                       "riderId", rider.getId().toString()));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/orders/{id}/confirm-delivery")
    public ResponseEntity<Order> confirmDelivery(@AuthenticationPrincipal User rider, @PathVariable UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!rider.getId().equals(order.getRider().getId())) {
            return ResponseEntity.status(403).build();
        }

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        addTracking(order, OrderStatus.DELIVERED, "Order delivered successfully");
        notifyCustomer(order.getCustomer().getId().toString(), "Your order has been delivered!");

        return ResponseEntity.ok(orderRepository.save(order));
    }

    private void addTracking(Order order, OrderStatus status, String message) {
        OrderTracking tracking = OrderTracking.builder()
                .order(order)
                .status(status)
                .message(message)
                .build();
        order.getTrackingHistory().add(tracking);
    }

    private void notifyCustomer(String customerId, String message) {
        messagingTemplate.convertAndSendToUser(customerId, "/queue/notifications",
                Map.of("message", message, "timestamp", LocalDateTime.now().toString()));
    }

    @Data
    public static class LocationUpdate {
        private BigDecimal latitude;
        private BigDecimal longitude;
    }
}
