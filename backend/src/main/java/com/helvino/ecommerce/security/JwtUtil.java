package com.helvino.ecommerce.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UUID userId, String email, String role) {
        return generateToken(userId, email, role, null);
    }

    public String generateToken(UUID userId, String email, String role, UUID tenantId) {
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey());
        if (tenantId != null) builder.claim("tenantId", tenantId.toString());
        return builder.compact();
    }

    public String generateImpersonationToken(UUID targetUserId, String targetEmail,
                                              String targetRole, UUID tenantId,
                                              UUID impersonatorId) {
        var builder = Jwts.builder()
                .subject(targetUserId.toString())
                .claim("email", targetEmail)
                .claim("role", targetRole)
                .claim("impersonatedBy", impersonatorId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000)) // 1 hour max
                .signWith(getSigningKey());
        if (tenantId != null) builder.claim("tenantId", tenantId.toString());
        return builder.compact();
    }

    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserId(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    public UUID getTenantId(String token) {
        try {
            String raw = (String) parseToken(token).get("tenantId");
            return raw != null ? UUID.fromString(raw) : null;
        } catch (Exception e) { return null; }
    }

    public UUID getImpersonatedBy(String token) {
        try {
            String raw = (String) parseToken(token).get("impersonatedBy");
            return raw != null ? UUID.fromString(raw) : null;
        } catch (Exception e) { return null; }
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
