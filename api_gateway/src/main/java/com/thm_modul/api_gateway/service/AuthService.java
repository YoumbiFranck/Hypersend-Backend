package com.thm_modul.api_gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RestTemplate restTemplate;

    @Value("${app.login-service.url}")
    private String loginServiceUrl;

    @Value("${app.gateway.secret}")
    private String gatewaySecret;

    /**
     * Authenticate user via login service
     * Forwards login request to internal login service
     */
    public Object authenticateUser(Map<String, String> loginRequest) {
        try {
            HttpHeaders headers = createInternalHeaders();
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(loginRequest, headers);

            String url = loginServiceUrl + "/internal/v1/auth/login";

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    log.debug("Login successful via login service");
                    return responseBody.get("data");
                } else {
                    String error = (String) responseBody.get("error");
                    throw new IllegalArgumentException(error != null ? error : "Authentication failed");
                }
            }

            throw new RuntimeException("Invalid response from login service");

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error communicating with login service: {}", e.getMessage(), e);
            throw new RuntimeException("Authentication service unavailable");
        }
    }

    /**
     * Refresh JWT tokens via login service
     */
    public Object refreshToken(Map<String, String> refreshRequest) {
        try {
            HttpHeaders headers = createInternalHeaders();
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(refreshRequest, headers);

            String url = loginServiceUrl + "/internal/v1/auth/refresh";

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    log.debug("Token refresh successful via login service");
                    return responseBody.get("data");
                } else {
                    String error = (String) responseBody.get("error");
                    throw new IllegalArgumentException(error != null ? error : "Token refresh failed");
                }
            }

            throw new RuntimeException("Invalid response from login service");

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error refreshing token via login service: {}", e.getMessage(), e);
            throw new RuntimeException("Token refresh service unavailable");
        }
    }

    /**
     * Get current user information from security context
     * Uses the authenticated user's information from JWT
     */
    public Object getCurrentUserInfo() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                Integer userId = (Integer) authentication.getPrincipal();
                String username = (String) authentication.getCredentials();

                // Get detailed user info from login service
                HttpHeaders headers = createInternalHeaders();
                HttpEntity<String> requestEntity = new HttpEntity<>(headers);

                String url = loginServiceUrl + "/internal/v1/auth/user-info/" + userId;

                ResponseEntity<Map> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        requestEntity,
                        Map.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Map<String, Object> responseBody = response.getBody();

                    if (Boolean.TRUE.equals(responseBody.get("success"))) {
                        return responseBody.get("data");
                    }
                }

                // Fallback to basic info from token if service call fails
                return Map.of(
                        "userId", userId,
                        "username", username
                );
            }

            throw new IllegalStateException("No authenticated user found");

        } catch (Exception e) {
            log.error("Error getting current user info: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve user information");
        }
    }

    /**
     * Validate user exists via login service
     * Used by other services to verify user existence
     */
    public boolean validateUserExists(Integer userId) {
        try {
            HttpHeaders headers = createInternalHeaders();
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            String url = loginServiceUrl + "/internal/v1/auth/validate-user/" + userId;

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

                if (data != null) {
                    return Boolean.TRUE.equals(data.get("exists"));
                }
            }

            return false;

        } catch (Exception e) {
            log.warn("Error validating user existence for ID {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Create headers for internal service communication
     * Includes the gateway secret for authentication
     */
    private HttpHeaders createInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Gateway-Secret", gatewaySecret);
        return headers;
    }

    /**
     * Create headers with user context for internal service communication
     */
    private HttpHeaders createInternalHeadersWithUser(Integer userId, String username) {
        HttpHeaders headers = createInternalHeaders();
        headers.set("X-User-ID", String.valueOf(userId));
        if (username != null) {
            headers.set("X-Username", username);
        }
        return headers;
    }
}
