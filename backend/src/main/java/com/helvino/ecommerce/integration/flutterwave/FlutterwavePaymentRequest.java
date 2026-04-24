package com.helvino.ecommerce.integration.flutterwave;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FlutterwavePaymentRequest {
    private String txRef;
    private double amount;
    private String currency;
    private String customerEmail;
    private String customerName;
    private String customerPhone;
    private String description;
}
