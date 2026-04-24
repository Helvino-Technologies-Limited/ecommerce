package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.dto.request.LoginRequest;
import com.helvino.ecommerce.dto.request.RegisterRequest;
import com.helvino.ecommerce.dto.response.AuthResponse;
import com.helvino.ecommerce.entity.User;
import com.helvino.ecommerce.enums.UserRole;
import com.helvino.ecommerce.repository.UserRepository;
import com.helvino.ecommerce.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email already registered"));
        }
        if (req.getPhone() != null && userRepository.existsByPhone(req.getPhone())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Phone already registered"));
        }

        User user = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(UserRole.CUSTOMER)
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String refresh = jwtUtil.generateRefreshToken(user.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(buildAuthResponse(user, token, refresh));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        }
        if (!user.isEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Account is disabled"));
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String refresh = jwtUtil.generateRefreshToken(user.getId());

        return ResponseEntity.ok(buildAuthResponse(user, token, refresh));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || !jwtUtil.isTokenValid(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired refresh token"));
        }

        var userId = jwtUtil.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return ResponseEntity.ok(Map.of("accessToken", newToken));
    }

    private AuthResponse buildAuthResponse(User user, String token, String refresh) {
        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken(refresh)
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .walletBalance(user.getWalletBalance())
                .loyaltyPoints(user.getLoyaltyPoints())
                .build();
    }
}
