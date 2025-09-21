package com.thm_modul.message_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserValidationService {

    private final JdbcTemplate jdbcTemplate;

    @Value("${app.user-cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${app.user-cache.ttl:300000}") // 5 minutes default
    private long cacheTtl;

    // Simple in-memory cache for user validation
    private final Map<Integer, CacheEntry> userCache = new HashMap<>();

    /**
     * Check if a user exists in the database
     * Uses local database query with optional caching
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

        try {
            // Query the user table directly
            String sql = "SELECT COUNT(*) FROM app_user WHERE id = ? AND enabled = true";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
            boolean exists = count != null && count > 0;

            // Cache the result if caching is enabled
            if (cacheEnabled) {
                userCache.put(userId, new CacheEntry(exists, System.currentTimeMillis()));
            }

            log.debug("User {} existence check: {}", userId, exists);
            return exists;

        } catch (Exception e) {
            log.error("Error checking user existence for ID {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Get username for a given user ID
     * Returns null if user doesn't exist
     */
    public String getUsername(Integer userId) {
        if (userId == null) {
            return null;
        }

        try {
            String sql = "SELECT user_name FROM app_user WHERE id = ? AND enabled = true";
            List<String> usernames = jdbcTemplate.queryForList(sql, String.class, userId);

            if (!usernames.isEmpty()) {
                String username = usernames.get(0);
                log.debug("Username for user {}: {}", userId, username);
                return username;
            }

            log.debug("No username found for user {}", userId);
            return null;

        } catch (Exception e) {
            log.error("Error getting username for user ID {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Get usernames for multiple user IDs
     * Returns a map of userId -> username
     */
    public Map<Integer, String> getUsernames(List<Integer> userIds) {
        Map<Integer, String> result = new HashMap<>();

        if (userIds == null || userIds.isEmpty()) {
            return result;
        }

        try {
            // Create placeholders for IN clause
            String placeholders = String.join(",", userIds.stream().map(id -> "?").toArray(String[]::new));
            String sql = "SELECT id, user_name FROM app_user WHERE id IN (" + placeholders + ") AND enabled = true";

            jdbcTemplate.query(sql, userIds.toArray(), (rs) -> {
                Integer id = rs.getInt("id");
                String username = rs.getString("user_name");
                result.put(id, username);
            });

            log.debug("Retrieved {} usernames out of {} requested", result.size(), userIds.size());

        } catch (Exception e) {
            log.error("Error getting usernames for user IDs {}: {}", userIds, e.getMessage());
        }

        return result;
    }

    /**
     * Validate that both sender and receiver exist
     * Returns true only if both users are valid
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
     * Clear cache for a specific user
     * Can be called when user data is updated
     */
    public void clearUserCache(Integer userId) {
        if (cacheEnabled && userId != null) {
            userCache.remove(userId);
            log.debug("Cleared cache for user {}", userId);
        }
    }

    /**
     * Clear entire user cache
     * Can be called periodically or when needed
     */
    public void clearAllCache() {
        if (cacheEnabled) {
            userCache.clear();
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
