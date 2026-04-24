package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.entity.User;
import com.helvino.ecommerce.enums.UserRole;
import com.helvino.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/admin/customers")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<User>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (q != null && !q.isBlank()) {
            return ResponseEntity.ok(userRepository.searchCustomers(q, pageable));
        }
        return ResponseEntity.ok(userRepository.findByRole(UserRole.CUSTOMER, pageable));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<User> toggle(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEnabled(!user.isEnabled());
        return ResponseEntity.ok(userRepository.save(user));
    }
}
