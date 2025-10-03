package com.thm_modul.message_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserValidationService {

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    @Value("${app.user-cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${app.user-cache.ttl:300000}") // 5 minutes default
    private long cacheTtl;

    @Value("${app.gateway.secret:shared_secret_key}")
    private String gatewaySecret;

    @Value("${app.login-service.url:http://hps_login_user:8082}")
    private String loginServiceUrl;

    // Simple in-memory cache for user validation
    private final Map<Integer, CacheEntry> userCache = new HashMap<>();
    private final Map<Integer, String> usernameCache = new HashMap<>();

    /**
     * Check if a user exists using multiple strategies:
     * 1. Local cache (if enabled)
     * 2. Direct database query (primary method)
     * 3. Login service call (fallback)
     */
    public boolean userExists(Integer userId) {
        if (userId == null) {
            return false;
        }

        // Check cache first if enabled
        if (cacheEnabled && isCacheValid(userId)) {
            log.debug("User {} found in cache", userId);
            return userCache.get(userId).exists;
        }

        // Primary method: Direct database query
        boolean exists = checkUserExistsInDatabase(userId);

        // If database check fails, try login service as fallback
        if (!exists) {
            exists = checkUserExistsViaLoginService(userId);
        }

        // Cache the result if caching is enabled
        if (cacheEnabled) {
            userCache.put(userId, new CacheEntry(exists, System.currentTimeMillis()));
        }

        log.debug("User {} existence check result: {}", userId, exists);
        return exists;
    }

    /**
     * Get username for a given user ID
     * Uses local database first, then login service as fallback
     */
    public String getUsername(Integer userId) {
        if (userId == null) {
            return null;
        }

        // Check username cache first
        if (cacheEnabled && usernameCache.containsKey(userId)) {
            return usernameCache.get(userId);
        }

        // Try local database first
        String username = getUsernameFromDatabase(userId);

        // If not found locally, try login service
        if (username == null) {
            username = getUsernameFromLoginService(userId);
        }

        // Cache the result
        if (cacheEnabled && username != null) {
            usernameCache.put(userId, username);
        }

        log.debug("Username for user {}: {}", userId, username);
        return username;
    }

    /**
     * Get usernames for multiple user IDs
     */
    public Map<Integer, String> getUsernames(List<Integer> userIds) {
        Map<Integer, String> result = new HashMap<>();

        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        for (Integer userId : userIds) {
            String username = getUsername(userId);
            if (username != null) {
                result.put(userId, username);
            }
        }

        log.debug("Retrieved {} usernames out of {} requested", result.size(), userIds.size());
        return result;
    }

    /**
     * Validate that both sender and receiver exist
     */
    public boolean validateUserPair(Integer senderId, Integer receiverId) {
        if (senderId == null || receiverId == null) {
            log.warn("Null user ID provided - sender: {}, receiver: {}", senderId, receiverId);
            return false;
        }

        if (senderId.equals(receiverId)) {
            log.warn("User {} trying to send message to themselves", senderId);
            return false;
        }

        boolean senderExists = userExists(senderId);
        boolean receiverExists = userExists(receiverId);

        if (!senderExists) {
            log.warn("Sender user {} does not exist", senderId);
        }
        if (!receiverExists) {
            log.warn("Receiver user {} does not exist", receiverId);
        }

        return senderExists && receiverExists;
    }

    /**
     * Check user existence in local database
     */
    private boolean checkUserExistsInDatabase(Integer userId) {
        try {
            String sql = "SELECT COUNT(*) FROM app_user WHERE id = ? AND enabled = true";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("Database query failed for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Get username from local database
     */
    private String getUsernameFromDatabase(Integer userId) {
        try {
            String sql = "SELECT user_name FROM app_user WHERE id = ? AND enabled = true";
            List<String> usernames = jdbcTemplate.queryForList(sql, String.class, userId);
            return !usernames.isEmpty() ? usernames.get(0) : null;
        } catch (Exception e) {
            log.warn("Database username query failed for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Check user existence via login service call
     */
    private boolean checkUserExistsViaLoginService(Integer userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Gateway-Secret", gatewaySecret);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = loginServiceUrl + "/internal/v1/auth/validate-user/" + userId;
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                if (data != null) {
                    return Boolean.TRUE.equals(data.get("exists"));
                }
            }

            return false;

        } catch (Exception e) {
            log.warn("Login service call failed for user validation {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Get username from login service
     */
    private String getUsernameFromLoginService(Integer userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Gateway-Secret", gatewaySecret);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = loginServiceUrl + "/internal/v1/auth/user-info/" + userId;
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                Map<String, Object> data = (Map<String, Object>) body.get("data");
                if (data != null) {
                    return (String) data.get("username");
                }
            }

            return null;

        } catch (Exception e) {
            log.warn("Login service call failed for username retrieval {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Clear cache for a specific user
     */
    public void clearUserCache(Integer userId) {
        if (cacheEnabled && userId != null) {
            userCache.remove(userId);
            usernameCache.remove(userId);
            log.debug("Cleared cache for user {}", userId);
        }
    }

    /**
     * Clear entire user cache
     */
    public void clearAllCache() {
        if (cacheEnabled) {
            userCache.clear();
            usernameCache.clear();
            log.debug("Cleared all user cache");
        }
    }

    /**
     * Check if cache entry is valid (not expired)
     */
    private boolean isCacheValid(Integer userId) {
        CacheEntry entry = userCache.get(userId);
        if (entry == null) {
            return false;
        }

        long age = System.currentTimeMillis() - entry.timestamp;
        return age < cacheTtl;
    }

    /**
     * Internal cache entry class
     */
    private static class CacheEntry {
        final boolean exists;
        final long timestamp;

        CacheEntry(boolean exists, long timestamp) {
            this.exists = exists;
            this.timestamp = timestamp;
        }
    }
}