package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.entity.Product;
import com.helvino.ecommerce.entity.ProductVariant;
import com.helvino.ecommerce.repository.ProductRepository;
import com.helvino.ecommerce.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantRepository variantRepo;
    private final ProductRepository productRepo;

    /** Public: get all available variants for a product */
    @GetMapping("/products/{productId}/variants")
    public ResponseEntity<List<ProductVariant>> getVariants(@PathVariable UUID productId) {
        return ResponseEntity.ok(variantRepo.findByProductIdAndAvailableTrue(productId));
    }

    /** Admin: create a variant */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping("/admin/products/{productId}/variants")
    public ResponseEntity<ProductVariant> create(@PathVariable UUID productId,
                                                  @RequestBody Map<String, Object> body) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .variantType(str(body, "variantType"))
                .variantValue(str(body, "variantValue"))
                .stockQuantity(intVal(body, "stockQuantity"))
                .priceAdjustment(decimal(body, "priceAdjustment"))
                .available(true)
                .build();

        return ResponseEntity.ok(variantRepo.save(variant));
    }

    /** Admin: update a variant */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PutMapping("/admin/products/variants/{id}")
    public ResponseEntity<ProductVariant> update(@PathVariable UUID id,
                                                  @RequestBody Map<String, Object> body) {
        ProductVariant variant = variantRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        if (body.containsKey("variantType")) variant.setVariantType(str(body, "variantType"));
        if (body.containsKey("variantValue")) variant.setVariantValue(str(body, "variantValue"));
        if (body.containsKey("stockQuantity")) variant.setStockQuantity(intVal(body, "stockQuantity"));
        if (body.containsKey("priceAdjustment")) variant.setPriceAdjustment(decimal(body, "priceAdjustment"));
        if (body.containsKey("available")) variant.setAvailable((Boolean) body.get("available"));

        return ResponseEntity.ok(variantRepo.save(variant));
    }

    /** Admin: delete a variant */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @DeleteMapping("/admin/products/variants/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        variantRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Admin: toggle availability */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PatchMapping("/admin/products/variants/{id}/toggle")
    public ResponseEntity<ProductVariant> toggle(@PathVariable UUID id) {
        ProductVariant variant = variantRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Variant not found"));
        variant.setAvailable(!variant.isAvailable());
        return ResponseEntity.ok(variantRepo.save(variant));
    }

    // --- helpers ---
    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : v.toString();
    }

    private int intVal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        return Integer.parseInt(v.toString());
    }

    private BigDecimal decimal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof Number) return new BigDecimal(v.toString());
        return new BigDecimal(v.toString());
    }
}
