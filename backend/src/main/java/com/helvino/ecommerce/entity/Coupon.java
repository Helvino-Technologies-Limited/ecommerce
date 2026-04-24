package com.helvino.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String code;

    private String description;

    @Column(nullable = false)
    private String discountType; // PERCENTAGE | FIXED

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(precision = 10, scale = 2)
    private BigDecimal minimumOrderAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal maximumDiscount;

    private int usageLimit = 0;  // 0 = unlimited

    private int usedCount = 0;

    private int userUsageLimit = 1;

    private boolean active = true;

    private LocalDateTime expiresAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public boolean isValid() {
        return active
               && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()))
               && (usageLimit == 0 || usedCount < usageLimit);
    }
}
