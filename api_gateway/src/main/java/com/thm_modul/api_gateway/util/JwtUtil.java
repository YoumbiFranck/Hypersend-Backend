package com.thm_modul.api_gateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Key key;

    /**
     * Initialize the signing key after properties are loaded
     */
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        log.debug("JWT utility initialized for API Gateway");
    }

    /**
     * Extract username from JWT token
     */
    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            log.warn("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract user ID from JWT token
     * This is critical for API Gateway to identify users for downstream services
     */
    public Integer getUserIdFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object userIdObj = claims.get("userId");

            if (userIdObj != null) {
                if (userIdObj instanceof Integer) {
                    return (Integer) userIdObj;
                } else if (userIdObj instanceof String) {
                    return Integer.valueOf((String) userIdObj);
                } else if (userIdObj instanceof Number) {
                    return ((Number) userIdObj).intValue();
                }
            }

            log.warn("No valid userId found in token claims");
            return null;

        } catch (Exception e) {
            log.warn("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract expiration date from JWT token
     */
    public Date extractExpiration(String token) {
        try {
            return extractClaim(token, Claims::getExpiration);
        } catch (Exception e) {
            log.warn("Failed to extract expiration from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract a specific claim from JWT token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        } catch (Exception e) {
            log.warn("Failed to extract claim from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract all claims from JWT token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Check if JWT token is expired
     */
    public Boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            log.warn("Error checking token expiration: {}", e.getMessage());
            return true; // Consider expired if we can't determine
        }
    }

    /**
     * Validate JWT token - main method used by authentication filter
     */
    public Boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.debug("Token is null or empty");
            return false;
        }

        try {
            // Parse and validate the token
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            // Additional check for expiration
            if (isTokenExpired(token)) {
                log.debug("Token is expired");
                return false;
            }

            log.debug("Token validation successful");
            return true;

        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token format: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        } catch (io.jsonwebtoken.security.SecurityException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during JWT validation: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Get token type from claims (access vs refresh)
     */
    public String getTokenType(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object typeObj = claims.get("type");
            return typeObj != null ? typeObj.toString() : "access";
        } catch (Exception e) {
            log.warn("Failed to extract token type: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if token is a refresh token
     */
    public Boolean isRefreshToken(String token) {
        String tokenType = getTokenType(token);
        return "refresh".equals(tokenType);
    }

    /**
     * Get remaining time until token expiration in milliseconds
     */
    public Long getTimeUntilExpiration(String token) {
        try {
            Date expiration = extractExpiration(token);
            if (expiration != null) {
                return expiration.getTime() - System.currentTimeMillis();
            }
            return null;
        } catch (Exception e) {
            log.warn("Error calculating time until expiration: {}", e.getMessage());
            return null;
        }
    }
}
