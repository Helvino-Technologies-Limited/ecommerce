package com.helvino.ecommerce.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public final class TenantContext {

    private TenantContext() {}

    public static UUID getTenantId(HttpServletRequest request) {
        return (UUID) request.getAttribute("tenantId");
    }

    public static boolean isImpersonating(HttpServletRequest request) {
        return request.getAttribute("impersonatedBy") != null;
    }

    public static UUID getImpersonatedBy(HttpServletRequest request) {
        return (UUID) request.getAttribute("impersonatedBy");
    }
}
