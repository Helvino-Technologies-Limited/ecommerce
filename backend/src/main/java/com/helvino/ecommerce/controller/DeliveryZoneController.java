package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.entity.DeliveryZone;
import com.helvino.ecommerce.repository.DeliveryZoneRepository;
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
public class DeliveryZoneController {

    private final DeliveryZoneRepository zoneRepo;

    /** Public: all active delivery zones */
    @GetMapping("/delivery/zones")
    public ResponseEntity<List<DeliveryZone>> getAllZones() {
        return ResponseEntity.ok(zoneRepo.findAllByActiveTrue());
    }

    /** Public: zones for a county */
    @GetMapping("/delivery/zones/county/{county}")
    public ResponseEntity<List<DeliveryZone>> getByCounty(@PathVariable String county) {
        return ResponseEntity.ok(zoneRepo.findByCountyIgnoreCaseAndActiveTrue(county));
    }

    /** Admin: create zone */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping("/admin/delivery/zones")
    public ResponseEntity<DeliveryZone> create(@RequestBody Map<String, Object> body) {
        DeliveryZone zone = DeliveryZone.builder()
                .county(str(body, "county"))
                .town(str(body, "town"))
                .deliveryCost(decimal(body, "deliveryCost"))
                .estimatedDays(intVal(body, "estimatedDays", 2))
                .active(true)
                .build();
        return ResponseEntity.ok(zoneRepo.save(zone));
    }

    /** Admin: update zone */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PutMapping("/admin/delivery/zones/{id}")
    public ResponseEntity<DeliveryZone> update(@PathVariable UUID id,
                                                @RequestBody Map<String, Object> body) {
        DeliveryZone zone = zoneRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Zone not found"));

        if (body.containsKey("county")) zone.setCounty(str(body, "county"));
        if (body.containsKey("town")) zone.setTown(str(body, "town"));
        if (body.containsKey("deliveryCost")) zone.setDeliveryCost(decimal(body, "deliveryCost"));
        if (body.containsKey("estimatedDays")) zone.setEstimatedDays(intVal(body, "estimatedDays", zone.getEstimatedDays()));
        if (body.containsKey("active")) zone.setActive((Boolean) body.get("active"));

        return ResponseEntity.ok(zoneRepo.save(zone));
    }

    /** Admin: delete zone */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @DeleteMapping("/admin/delivery/zones/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        zoneRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- helpers ---
    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? "" : v.toString();
    }

    private int intVal(Map<String, Object> m, String key, int def) {
        Object v = m.get(key);
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
    }

    private BigDecimal decimal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return BigDecimal.ZERO;
        return new BigDecimal(v.toString());
    }
}
