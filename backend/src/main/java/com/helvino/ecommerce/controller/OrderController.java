package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.entity.*;
import com.helvino.ecommerce.enums.Currency;
import com.helvino.ecommerce.enums.OrderStatus;
import com.helvino.ecommerce.enums.PaymentMethod;
import com.helvino.ecommerce.enums.UserRole;
import com.helvino.ecommerce.repository.*;
import com.helvino.ecommerce.security.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid CreateOrderRequest req) {

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customer(user)
                .currency(Currency.valueOf(req.getCurrency() != null ? req.getCurrency() : "KES"))
                .status(OrderStatus.PENDING)
                .notes(req.getNotes())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (CreateOrderRequest.ItemRequest itemReq : req.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemReq.getProductId()));

            if (product.getStockQuantity() < itemReq.getQuantity()) {
                throw new RuntimeException("Insufficient stock for: " + product.getName());
            }

            BigDecimal unitPrice = product.getDiscountedPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .productImage(product.getImages().isEmpty() ? null : product.getImages().get(0))
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice)
                    .totalPrice(lineTotal)
                    .build();
            items.add(item);

            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
            product.setSalesCount(product.getSalesCount() + itemReq.getQuantity());
            productRepository.save(product);
        }

        BigDecimal deliveryFee = subtotal.compareTo(new BigDecimal("2000")) >= 0
                ? BigDecimal.ZERO
                : new BigDecimal("200");

        order.setItems(items);
        order.setSubtotal(subtotal);
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(subtotal.add(deliveryFee));

        if (req.getPaymentMethod() != null) {
            order.setPaymentMethod(PaymentMethod.valueOf(req.getPaymentMethod()));
        }

        // Delivery address
        if (req.getDeliveryAddress() != null) {
            Address address = Address.builder()
                    .user(user)
                    .streetAddress(req.getDeliveryAddress().getStreetAddress())
                    .city(req.getDeliveryAddress().getCity())
                    .county(req.getDeliveryAddress().getCounty())
                    .country("Kenya")
                    .build();
            order.setDeliveryAddress(address);
        }

        // Loyalty points (1 point per 100 KES spent)
        int pointsEarned = subtotal.divide(new BigDecimal("100"), 0, java.math.RoundingMode.DOWN).intValue();
        order.setLoyaltyPointsEarned(pointsEarned);
        user.setLoyaltyPoints(user.getLoyaltyPoints() + pointsEarned);
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(orderRepository.save(order));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/orders")
    public ResponseEntity<Page<Order>> getMyOrders(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderRepository.findByCustomerId(
                user.getId(), PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrder(@AuthenticationPrincipal User user, @PathVariable UUID id) {
        return orderRepository.findById(id)
                .filter(o -> o.getCustomer().getId().equals(user.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Page<Order>> adminGetOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User caller,
            HttpServletRequest request) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        if (caller.getRole() == UserRole.SUPER_ADMIN) {
            if (status != null) return ResponseEntity.ok(orderRepository.findByStatus(status, pageable));
            return ResponseEntity.ok(orderRepository.findAll(pageable));
        }

        // ADMIN: only orders containing their products
        UUID tenantId = TenantContext.getTenantId(request);
        if (tenantId == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        if (status != null) {
            return ResponseEntity.ok(orderRepository.findByTenantIdAndStatus(tenantId, status, pageable));
        }
        return ResponseEntity.ok(orderRepository.findByTenantId(tenantId, pageable));
    }

    @PatchMapping("/admin/orders/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Order> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User caller,
            HttpServletRequest request) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (caller.getRole() == UserRole.ADMIN) {
            UUID tenantId = TenantContext.getTenantId(request);
            if (tenantId == null || !orderRepository.existsItemByOrderIdAndTenantId(id, tenantId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(null);
            }
        }

        order.setStatus(OrderStatus.valueOf(body.get("status")));
        if (OrderStatus.DELIVERED.name().equals(body.get("status"))) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        OrderTracking tracking = OrderTracking.builder()
                .order(order)
                .status(order.getStatus())
                .message("Order status updated to " + order.getStatus())
                .build();
        order.getTrackingHistory().add(tracking);

        return ResponseEntity.ok(orderRepository.save(order));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/orders/{id}/confirm")
    public ResponseEntity<?> confirmReceipt(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ConfirmReceiptRequest req) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getCustomer().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Order must be marked as delivered before you can confirm receipt"));
        }
        if (order.getCustomerConfirmedAt() != null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "You have already confirmed receipt for this order"));
        }

        order.setCustomerConfirmedAt(LocalDateTime.now());
        order.setCustomerPaymentRef(req.getPaymentReference());
        order.setCustomerNote(req.getNote());

        return ResponseEntity.ok(orderRepository.save(order));
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    @Data
    public static class ConfirmReceiptRequest {
        @NotBlank(message = "Payment reference is required")
        private String paymentReference;
        private String note;
    }

    @Data
    public static class CreateOrderRequest {
        private List<ItemRequest> items;
        private AddressRequest deliveryAddress;
        private String paymentMethod;
        private String couponCode;
        private String currency;
        private String notes;

        @Data
        public static class ItemRequest {
            private UUID productId;
            private int quantity;
        }

        @Data
        public static class AddressRequest {
            private String streetAddress;
            private String city;
            private String county;
        }
    }
}
