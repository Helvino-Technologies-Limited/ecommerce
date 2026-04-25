package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.entity.SubscriptionPayment;
import com.helvino.ecommerce.entity.Tenant;
import com.helvino.ecommerce.entity.User;
import com.helvino.ecommerce.enums.SubscriptionStatus;
import com.helvino.ecommerce.enums.UserRole;
import com.helvino.ecommerce.repository.ProductRepository;
import com.helvino.ecommerce.repository.SubscriptionPaymentRepository;
import com.helvino.ecommerce.repository.TenantRepository;
import com.helvino.ecommerce.repository.UserRepository;
import com.helvino.ecommerce.security.JwtUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/super-admin/tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class SuperAdminTenantController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // ── List tenants ──────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<Tenant>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (q != null && !q.isBlank()) {
            return ResponseEntity.ok(tenantRepository.search(q, pageable));
        }
        return ResponseEntity.ok(tenantRepository.findAll(pageable));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", tenantRepository.count());
        result.put("trial", tenantRepository.countBySubscriptionStatus(SubscriptionStatus.TRIAL));
        result.put("active", tenantRepository.countBySubscriptionStatus(SubscriptionStatus.ACTIVE));
        result.put("inactive", tenantRepository.countBySubscriptionStatus(SubscriptionStatus.INACTIVE));
        result.put("suspended", tenantRepository.countBySubscriptionStatus(SubscriptionStatus.SUSPENDED));
        return ResponseEntity.ok(result);
    }

    // ── Single tenant ─────────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<Tenant> get(@PathVariable UUID id) {
        return tenantRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/products")
    public ResponseEntity<?> getProducts(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!tenantRepository.existsById(id)) return ResponseEntity.notFound().build();
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(productRepository.findByTenantId(id, pageable));
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<SubscriptionPayment>> getPayments(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentRepository.findByTenantIdOrderByCreatedAtDesc(id));
    }

    // ── Create tenant (admin-initiated registration) ──────────────────────

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody CreateTenantRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already registered"));
        }
        if (tenantRepository.existsByBusinessName(req.getBusinessName())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Business name already taken"));
        }

        User owner = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(UserRole.ADMIN)
                .enabled(true)
                .emailVerified(true)
                .build();
        userRepository.save(owner);

        String slug = generateSlug(req.getBusinessName());
        SubscriptionStatus status = req.isActivateImmediately()
                ? SubscriptionStatus.ACTIVE : SubscriptionStatus.TRIAL;
        LocalDate renewsAt = req.isActivateImmediately() ? LocalDate.now().plusMonths(1) : null;

        Tenant tenant = Tenant.builder()
                .businessName(req.getBusinessName())
                .slug(slug)
                .businessDescription(req.getBusinessDescription())
                .contactPhone(req.getContactPhone())
                .owner(owner)
                .subscriptionStatus(status)
                .trialEndsAt(LocalDate.now().plusDays(30))
                .subscriptionRenewsAt(renewsAt)
                .active(true)
                .build();
        tenantRepository.save(tenant);
        return ResponseEntity.status(HttpStatus.CREATED).body(tenant);
    }

    // ── Update tenant ─────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ResponseEntity<Tenant> update(@PathVariable UUID id,
                                          @RequestBody UpdateTenantRequest req) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        if (req.getBusinessDescription() != null) tenant.setBusinessDescription(req.getBusinessDescription());
        if (req.getContactPhone() != null) tenant.setContactPhone(req.getContactPhone());
        if (req.getLogoUrl() != null) tenant.setLogoUrl(req.getLogoUrl());
        if (req.getCounty() != null) tenant.setCounty(req.getCounty());
        return ResponseEntity.ok(tenantRepository.save(tenant));
    }

    // ── Status changes ────────────────────────────────────────────────────

    @PatchMapping("/{id}/activate")
    @Transactional
    public ResponseEntity<Tenant> activate(@PathVariable UUID id,
                                            @RequestParam(required = false) String paymentReference) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        LocalDate renewsAt = tenant.getSubscriptionRenewsAt() != null &&
                             tenant.getSubscriptionRenewsAt().isAfter(LocalDate.now())
                ? tenant.getSubscriptionRenewsAt().plusMonths(1)
                : LocalDate.now().plusMonths(1);

        tenant.setSubscriptionStatus(SubscriptionStatus.ACTIVE);
        tenant.setSubscriptionRenewsAt(renewsAt);
        tenant.setLastPaymentAt(LocalDate.now());
        tenant.setTotalPaymentsMade(tenant.getTotalPaymentsMade() + 1);
        tenant.setActive(true);
        tenant.getOwner().setEnabled(true);
        tenantRepository.save(tenant);

        // Record payment
        if (paymentReference != null || true) {
            SubscriptionPayment payment = SubscriptionPayment.builder()
                    .tenant(tenant)
                    .amount(tenant.getMonthlyFee())
                    .paymentReference(paymentReference != null ? paymentReference : "MANUAL")
                    .paymentMethod("MANUAL")
                    .periodStart(LocalDate.now())
                    .periodEnd(renewsAt)
                    .build();
            paymentRepository.save(payment);
        }

        return ResponseEntity.ok(tenant);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Tenant> deactivate(@PathVariable UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        tenant.setSubscriptionStatus(SubscriptionStatus.INACTIVE);
        // User can still log in but products are hidden from public storefront
        tenant.getOwner().setEnabled(true);
        return ResponseEntity.ok(tenantRepository.save(tenant));
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<Tenant> suspend(@PathVariable UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        tenant.setSubscriptionStatus(SubscriptionStatus.SUSPENDED);
        tenant.setActive(false);
        // Block login
        tenant.getOwner().setEnabled(false);
        userRepository.save(tenant.getOwner());
        return ResponseEntity.ok(tenantRepository.save(tenant));
    }

    @PatchMapping("/{id}/reinstate")
    public ResponseEntity<Tenant> reinstate(@PathVariable UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        tenant.setSubscriptionStatus(SubscriptionStatus.TRIAL);
        tenant.setTrialEndsAt(LocalDate.now().plusDays(30));
        tenant.setActive(true);
        tenant.getOwner().setEnabled(true);
        userRepository.save(tenant.getOwner());
        return ResponseEntity.ok(tenantRepository.save(tenant));
    }

    // ── Impersonation ─────────────────────────────────────────────────────

    @PostMapping("/{id}/impersonate")
    public ResponseEntity<Map<String, Object>> impersonate(
            @PathVariable UUID id,
            @AuthenticationPrincipal User superAdmin) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        User owner = tenant.getOwner();

        String impersonationToken = jwtUtil.generateImpersonationToken(
                owner.getId(), owner.getEmail(), "ADMIN",
                tenant.getId(), superAdmin.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", impersonationToken);
        response.put("impersonating", true);
        response.put("businessName", tenant.getBusinessName());
        response.put("tenantId", tenant.getId());
        response.put("id", owner.getId());
        response.put("email", owner.getEmail());
        response.put("firstName", owner.getFirstName());
        response.put("lastName", owner.getLastName());
        response.put("role", "ADMIN");
        response.put("walletBalance", owner.getWalletBalance());
        response.put("loyaltyPoints", owner.getLoyaltyPoints());
        return ResponseEntity.ok(response);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String generateSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        if (!tenantRepository.existsBySlug(base)) return base;
        int i = 2;
        while (tenantRepository.existsBySlug(base + "-" + i)) i++;
        return base + "-" + i;
    }

    // ── Request DTOs ──────────────────────────────────────────────────────

    @Data
    public static class CreateTenantRequest {
        @NotBlank private String firstName;
        @NotBlank private String lastName;
        @Email @NotBlank private String email;
        private String phone;
        @NotBlank @Size(min = 8) private String password;
        @NotBlank private String businessName;
        private String businessDescription;
        private String contactPhone;
        private boolean activateImmediately = false;
    }

    @Data
    public static class UpdateTenantRequest {
        private String businessDescription;
        private String contactPhone;
        private String logoUrl;
        private String county;
    }
}
