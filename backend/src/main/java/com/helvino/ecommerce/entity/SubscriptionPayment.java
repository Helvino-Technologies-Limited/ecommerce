package com.helvino.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscription_payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubscriptionPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private String paymentReference;

    private String paymentMethod;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
