package com.helvino.ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_variants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** e.g. "SIZE", "COLOR", "MATERIAL", "TYPE" */
    @Column(nullable = false)
    private String variantType;

    /** e.g. "XL", "Red", "Cotton" */
    @Column(nullable = false)
    private String variantValue;

    private int stockQuantity = 0;

    @Column(precision = 12, scale = 2)
    private BigDecimal priceAdjustment = BigDecimal.ZERO;

    private boolean available = true;
}
