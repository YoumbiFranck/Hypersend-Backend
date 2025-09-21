package com.thm_modul.message_service.util;

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
     * Uses HMAC-SHA256 algorithm for token signing
     */
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        log.debug("JWT utility initialized with secret key");
    }

    /**
     * Extract username from JWT token
     * Returns the subject claim which contains the username
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
     * Returns the custom userId claim added during token generation
     */
    public Integer getUserIdFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object userIdObj = claims.get("userId");

            if (userIdObj != null) {
                // Handle both Integer and String representations
                if (userIdObj instanceof Integer) {
                    return (Integer) userIdObj;
                } else if (userIdObj instanceof String) {
                    return Integer.valueOf((String) userIdObj);
                } else if (userIdObj instanceof Number) {
                    return ((Number) userIdObj).intValue();
                }
            }

            log.warn("No userId found in token claims");
            return null;

        } catch (Exception e) {
            log.warn("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract expiration date from JWT token
     * Used for token validation
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
     * Extract a specific claim from JWT token using a claims resolver function
     * Generic method for extracting any claim type
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
     * Private method used by other extraction methods
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
     * Compares token expiration with current time
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
     * Validate JWT token
     * Checks signature validity and expiration
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
     * Validate JWT token with additional username verification
     * Useful when you want to ensure token belongs to specific user
     */
    public Boolean validateToken(String token, String expectedUsername) {
        if (!validateToken(token)) {
            return false;
        }

        try {
            String tokenUsername = extractUsername(token);
            boolean usernameMatches = expectedUsername.equals(tokenUsername);

            if (!usernameMatches) {
                log.warn("Token username '{}' does not match expected username '{}'",
                        tokenUsername, expectedUsername);
            }

            return usernameMatches;

        } catch (Exception e) {
            log.warn("Error validating token with username: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate JWT token with additional user ID verification
     * Useful when you want to ensure token belongs to specific user ID
     */
    public Boolean validateToken(String token, Integer expectedUserId) {
        if (!validateToken(token)) {
            return false;
        }

        try {
            Integer tokenUserId = getUserIdFromToken(token);
            boolean userIdMatches = expectedUserId.equals(tokenUserId);

            if (!userIdMatches) {
                log.warn("Token user ID '{}' does not match expected user ID '{}'",
                        tokenUserId, expectedUserId);
            }

            return userIdMatches;

        } catch (Exception e) {
            log.warn("Error validating token with user ID: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get token type from claims
     * Useful to distinguish between access and refresh tokens
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
     * Returns true if token has type "refresh"
     */
    public Boolean isRefreshToken(String token) {
        String tokenType = getTokenType(token);
        return "refresh".equals(tokenType);
    }

    /**
     * Get remaining time until token expiration in milliseconds
     * Returns negative value if token is expired
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

    /**
     * Extract issued at time from token
     * Useful for token age calculations
     */
    public Date extractIssuedAt(String token) {
        try {
            return extractClaim(token, Claims::getIssuedAt);
        } catch (Exception e) {
            log.warn("Failed to extract issued at time: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Calculate token age in milliseconds
     * Returns time since token was issued
     */
    public Long getTokenAge(String token) {
        try {
            Date issuedAt = extractIssuedAt(token);
            if (issuedAt != null) {
                return System.currentTimeMillis() - issuedAt.getTime();
            }
            return null;
        } catch (Exception e) {
            log.warn("Error calculating token age: {}", e.getMessage());
            return null;
        }
    }
}
