package com.helvino.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String type; // CREDIT | DEBIT

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    private String description;

    private String reference;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
