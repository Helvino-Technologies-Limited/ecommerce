package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.entity.Coupon;
import com.helvino.ecommerce.repository.CouponRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/coupons")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponRepository couponRepository;

    @GetMapping
    public ResponseEntity<List<Coupon>> list() {
        return ResponseEntity.ok(couponRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Coupon> create(@RequestBody CouponRequest req) {
        Coupon coupon = Coupon.builder()
                .code(req.getCode().toUpperCase().trim())
                .description(req.getDescription())
                .discountType(req.getDiscountType())
                .discountValue(BigDecimal.valueOf(req.getDiscountValue()))
                .minimumOrderAmount(req.getMinimumOrderAmount() != null
                        ? BigDecimal.valueOf(req.getMinimumOrderAmount()) : null)
                .usageLimit(req.getUsageLimit() != null ? req.getUsageLimit() : 0)
                .userUsageLimit(1)
                .active(true)
                .expiresAt(req.getExpiresAt())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(couponRepository.save(coupon));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Coupon> toggle(@PathVariable UUID id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        coupon.setActive(!coupon.isActive());
        return ResponseEntity.ok(couponRepository.save(coupon));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        couponRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class CouponRequest {
        private String code;
        private String description;
        private String discountType;
        private double discountValue;
        private Double minimumOrderAmount;
        private Integer usageLimit;
        private LocalDateTime expiresAt;
    }
}
