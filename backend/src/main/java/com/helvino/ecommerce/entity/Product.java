package com.helvino.ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.helvino.ecommerce.enums.Currency;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String shortDescription;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(precision = 12, scale = 2)
    private BigDecimal compareAtPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency = Currency.KES;

    @Column(nullable = false)
    private int stockQuantity = 0;

    private String sku;

    private String barcode;

    @Column(precision = 8, scale = 2)
    private BigDecimal weight;

    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    private boolean active = true;

    private boolean featured = false;

    private boolean flashSale = false;

    @Column(precision = 5, scale = 2)
    private BigDecimal flashSaleDiscount;

    private LocalDateTime flashSaleEndsAt;

    private Double averageRating = 0.0;

    private int reviewCount = 0;

    private int salesCount = 0;

    @ElementCollection
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public BigDecimal getDiscountedPrice() {
        if (flashSale && flashSaleDiscount != null) {
            BigDecimal discount = price.multiply(flashSaleDiscount.divide(new BigDecimal("100")));
            return price.subtract(discount);
        }
        return price;
    }
}
