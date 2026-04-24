package com.helvino.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "delivery_zones",
        uniqueConstraints = @UniqueConstraint(columnNames = {"county", "town"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeliveryZone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String county;

    @Column(nullable = false)
    private String town;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryCost;

    private int estimatedDays = 1;

    private boolean active = true;
}
