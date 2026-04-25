package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.entity.Tenant;
import com.helvino.ecommerce.repository.TenantRepository;
import com.helvino.ecommerce.security.TenantContext;
import com.helvino.ecommerce.service.TenantSubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/subscription")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TenantSubscriptionController {

    private final TenantRepository tenantRepository;
    private final TenantSubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<TenantSubscriptionService.SubscriptionInfo> getMySubscription(
            HttpServletRequest request) {
        UUID tenantId = TenantContext.getTenantId(request);
        if (tenantId == null) return ResponseEntity.notFound().build();
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(subscriptionService.getInfo(tenant));
    }
}
