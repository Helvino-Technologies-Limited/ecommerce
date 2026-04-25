package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.dto.response.AuthResponse;
import com.helvino.ecommerce.entity.Tenant;
import com.helvino.ecommerce.entity.User;
import com.helvino.ecommerce.enums.SubscriptionStatus;
import com.helvino.ecommerce.enums.UserRole;
import com.helvino.ecommerce.repository.TenantRepository;
import com.helvino.ecommerce.repository.UserRepository;
import com.helvino.ecommerce.security.JwtUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class TenantRegistrationController {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register-business")
    @Transactional
    public ResponseEntity<?> registerBusiness(@Valid @RequestBody BusinessRegisterRequest req) {
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
                .emailVerified(false)
                .build();
        userRepository.save(owner);

        String slug = generateSlug(req.getBusinessName());

        Tenant tenant = Tenant.builder()
                .businessName(req.getBusinessName())
                .slug(slug)
                .businessDescription(req.getBusinessDescription())
                .contactPhone(req.getContactPhone() != null ? req.getContactPhone() : req.getPhone())
                .owner(owner)
                .subscriptionStatus(SubscriptionStatus.TRIAL)
                .trialEndsAt(LocalDate.now().plusDays(30))
                .active(true)
                .build();
        tenantRepository.save(tenant);

        String token = jwtUtil.generateToken(owner.getId(), owner.getEmail(), owner.getRole().name(), tenant.getId());
        String refresh = jwtUtil.generateRefreshToken(owner.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                AuthResponse.builder()
                        .accessToken(token)
                        .refreshToken(refresh)
                        .id(owner.getId())
                        .email(owner.getEmail())
                        .firstName(owner.getFirstName())
                        .lastName(owner.getLastName())
                        .role(owner.getRole().name())
                        .walletBalance(owner.getWalletBalance())
                        .loyaltyPoints(owner.getLoyaltyPoints())
                        .tenantId(tenant.getId())
                        .businessName(tenant.getBusinessName())
                        .build()
        );
    }

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

    @Data
    public static class BusinessRegisterRequest {
        @NotBlank private String firstName;
        @NotBlank private String lastName;
        @Email @NotBlank private String email;
        private String phone;
        @NotBlank @Size(min = 8) private String password;
        @NotBlank @Size(min = 2) private String businessName;
        private String businessDescription;
        private String contactPhone;
    }
}
