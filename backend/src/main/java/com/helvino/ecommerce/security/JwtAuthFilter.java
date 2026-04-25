package com.helvino.ecommerce.security;

import com.helvino.ecommerce.entity.User;
import com.helvino.ecommerce.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);
        // Only attempt DB lookup when a token is actually present.
        // Public-endpoint requests (via publicApi) send no token, so this block
        // is skipped entirely — no unnecessary DB connections for anonymous traffic.
        if (token != null && jwtUtil.isTokenValid(token)) {
            try {
                Claims claims = jwtUtil.parseToken(token);
                UUID userId = UUID.fromString(claims.getSubject());
                String role = (String) claims.get("role");

                User user = userRepository.findById(userId).orElse(null);
                if (user != null && user.isEnabled()) {
                    var auth = new UsernamePasswordAuthenticationToken(
                            user, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    // Stash tenantId and impersonation flag for controllers to use
                    UUID tenantId = jwtUtil.getTenantId(token);
                    if (tenantId != null) request.setAttribute("tenantId", tenantId);
                    UUID impersonatedBy = jwtUtil.getImpersonatedBy(token);
                    if (impersonatedBy != null) request.setAttribute("impersonatedBy", impersonatedBy);
                }
            } catch (Exception ignored) {}
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
