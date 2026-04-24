package com.helvino.ecommerce.config;

import com.helvino.ecommerce.entity.User;
import com.helvino.ecommerce.enums.UserRole;
import com.helvino.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        ensureAdminExists();
    }

    private void ensureAdminExists() {
        String email    = env("ADMIN_EMAIL",    "info@helvino.org");
        String password = env("ADMIN_PASSWORD", "Helvino@2026");
        String phone    = env("ADMIN_PHONE",    "0110421320");

        userRepository.findByEmail(email).ifPresentOrElse(
            existing -> {
                boolean dirty = false;
                if (!existing.isEnabled())                         { existing.setEnabled(true);                       dirty = true; }
                if (!existing.isEmailVerified())                   { existing.setEmailVerified(true);                 dirty = true; }
                if (existing.getRole() != UserRole.SUPER_ADMIN)   { existing.setRole(UserRole.SUPER_ADMIN);           dirty = true; }
                // Always re-hash with Java BCrypt so format is guaranteed compatible
                existing.setPassword(passwordEncoder.encode(password));
                dirty = true;
                if (dirty) {
                    userRepository.save(existing);
                    log.info("Admin user updated and enabled: {}", email);
                }
            },
            () -> {
                User admin = User.builder()
                        .firstName("Helvino")
                        .lastName("Admin")
                        .email(email)
                        .phone(phone)
                        .password(passwordEncoder.encode(password))
                        .role(UserRole.SUPER_ADMIN)
                        .enabled(true)
                        .emailVerified(true)
                        .phoneVerified(true)
                        .walletBalance(BigDecimal.ZERO)
                        .loyaltyPoints(0)
                        .build();
                userRepository.save(admin);
                log.info("Admin user created: {}", email);
            }
        );
    }

    private String env(String key, String fallback) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : fallback;
    }
}
