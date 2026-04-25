package com.helvino.ecommerce.enums;

public enum SubscriptionStatus {
    TRIAL,      // 30-day free trial after registration
    ACTIVE,     // Paid and current
    INACTIVE,   // Payment lapsed — login works, products hidden from storefront
    SUSPENDED   // Manually suspended by SUPER_ADMIN — login blocked
}
