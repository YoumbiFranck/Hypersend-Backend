package com.thm_modul.api_gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientResponseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final RestTemplate restTemplate;

    @Value("${app.register-service.url}")
    private String registerServiceUrl;

    @Value("${app.login-service.url}")
    private String loginServiceUrl;

    @Value("${app.gateway.secret}")
    private String gatewaySecret;

    /**
     * Register a new user via register service
     */
    public Object registerUser(Map<String, String> registrationRequest) {
        try {
            HttpHeaders headers = createInternalHeaders();
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(registrationRequest, headers);

            String url = registerServiceUrl + "/internal/v1/register";

            log.debug("Sending registration request to URL: {}", url);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            log.debug("Registration response: {}", response.getBody());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    log.debug("User registration successful via register service");
                    return responseBody.get("data");
                } else {
                    String error = (String) responseBody.get("error");
                    throw new IllegalArgumentException(error != null ? error : "Registration failed");
                }
            }

            throw new RuntimeException("Invalid response from register service");

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error communicating with register service: {}", e.getMessage(), e);
            throw new RuntimeException("Registration service unavailable");
        }
    }

    /**
     * Get user profile information via login service
     */
    public Object getUserProfile(Integer userId) {
        try {
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
                } else {
                    return null;
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Error getting user profile for ID {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve user profile");
        }
    }

    /**
     * Get public user information by ID
     */
    public Object getUserById(Integer userId) {
        try {
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
                    Map<String, Object> userData = (Map<String, Object>) responseBody.get("data");

                    // Return only public information
                    return Map.of(
                            "userId", userData.get("userId"),
                            "username", userData.get("username")
                            // Email is not included for privacy
                    );
                }
            }

            return null;

        } catch (Exception e) {
            log.warn("Error getting user by ID {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Search users by username (placeholder implementation)
     * In a real implementation, this might search a user directory service
     */
    public Object searchUsers(String query, int limit) {
        // Validate query
        if (query == null || query.trim().length() < 2) {
            throw new IllegalArgumentException("Search query must be at least 2 characters long");
        }

        if (limit <= 0 || limit > 50) {
            throw new IllegalArgumentException("Limit must be between 1 and 50");
        }

        try {
            // For now, this is a placeholder implementation
            // In a real system, you might:
            // 1. Call a dedicated user search service
            // 2. Use a search engine like Elasticsearch
            // 3. Query the user database with LIKE operators

            log.debug("User search not fully implemented - query: '{}', limit: {}", query, limit);

            // Return empty results for now
            return Map.of(
                    "query", query,
                    "results", java.util.List.of(),
                    "totalCount", 0
            );

        } catch (Exception e) {
            log.error("Error searching users: {}", e.getMessage(), e);
            throw new RuntimeException("User search service unavailable");
        }
    }

    /**
     * Create headers for internal service communication
     */
    private HttpHeaders createInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Gateway-Secret", gatewaySecret);
        return headers;
    }
}
