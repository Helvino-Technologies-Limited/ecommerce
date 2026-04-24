package com.helvino.ecommerce.entity;

import com.helvino.ecommerce.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_tracking")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private String message;

    @Column(precision = 10, scale = 8)
    private BigDecimal riderLatitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal riderLongitude;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
