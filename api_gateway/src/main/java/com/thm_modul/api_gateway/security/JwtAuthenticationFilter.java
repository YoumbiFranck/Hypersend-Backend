package com.thm_modul.api_gateway.security;

import com.thm_modul.api_gateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /**
     * Main JWT authentication filter for API Gateway
     * Validates JWT tokens and sets up Spring Security context
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Extract Authorization header
        String authorizationHeader = request.getHeader("Authorization");

        // Check if request contains Bearer token
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                // Extract token (remove "Bearer " prefix)
                String token = authorizationHeader.substring(7);

                // Validate token
                if (jwtUtil.validateToken(token)) {
                    // Extract user information from token
                    String username = jwtUtil.extractUsername(token);
                    Integer userId = jwtUtil.getUserIdFromToken(token);

                    if (username != null && userId != null) {
                        // Create authentication object
                        UsernamePasswordAuthenticationToken authenticationToken =
                                new UsernamePasswordAuthenticationToken(
                                        userId, // Principal = user ID for easy access
                                        username, // Credentials = username
                                        Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
                                );

                        // Set authentication in security context
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                        log.debug("JWT authentication successful for user: {} (ID: {})", username, userId);
                    } else {
                        log.warn("JWT token missing required claims (username or userId)");
                    }
                } else {
                    log.debug("Invalid JWT token provided");
                }
            } catch (Exception e) {
                log.warn("JWT authentication failed: {}", e.getMessage());
                // Continue without authentication - let Spring Security handle authorization
            }
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Determine if this filter should be applied to the request
     * Skip filtering for public endpoints
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip authentication for public endpoints
        return path.equals("/api/v1/auth/login") ||
                path.equals("/api/v1/auth/refresh") ||
                path.equals("/api/v1/users/register") ||
                path.startsWith("/actuator/") ||
                path.equals("/api/v1/health");
    }
}
