package com.helvino.ecommerce.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String avatarUrl;
    private BigDecimal walletBalance;
    private int loyaltyPoints;
}
