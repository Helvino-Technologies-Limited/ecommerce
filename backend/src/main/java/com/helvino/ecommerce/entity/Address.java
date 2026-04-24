package com.helvino.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "addresses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String label;  // Home, Work, Other

    @Column(nullable = false)
    private String streetAddress;

    private String apartment;

    private String city;

    private String county;

    private String country = "Kenya";

    private String postalCode;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    private boolean isDefault = false;

    private boolean active = true;
}
