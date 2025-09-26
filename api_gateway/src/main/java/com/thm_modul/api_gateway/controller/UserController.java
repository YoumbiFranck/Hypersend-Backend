package com.thm_modul.api_gateway.controller;

import com.thm_modul.api_gateway.dto.ApiResponse;
import com.thm_modul.api_gateway.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Public user registration endpoint
     * Routes to register user service
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> registerUser(@Valid @RequestBody Map<String, String> registrationRequest) {
        try {
            log.info("User registration request received for: {}", registrationRequest.get("userName"));

            Object registrationResponse = userService.registerUser(registrationRequest);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("User registered successfully", registrationResponse));

        } catch (IllegalArgumentException e) {
            log.warn("User registration failed - validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Registration failed: " + e.getMessage()));

        } catch (Exception e) {
            log.error("User registration error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Internal server error during registration"));
        }
    }

    /**
     * Get current user profile information
     * Requires authentication
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Object>> getUserProfile(Authentication authentication) {
        try {
            Integer userId = (Integer) authentication.getPrincipal();
            log.debug("Profile request for user ID: {}", userId);

            Object userProfile = userService.getUserProfile(userId);

            return ResponseEntity.ok(ApiResponse.success("User profile retrieved", userProfile));

        } catch (Exception e) {
            log.error("Error retrieving user profile: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve user profile"));
        }
    }

    /**
     * Get public user information by ID
     * Requires authentication - for finding other users
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Object>> getUserById(
            @PathVariable Integer userId,
            Authentication authentication) {

        try {
            Integer requestingUserId = (Integer) authentication.getPrincipal();
            log.debug("User {} requesting info for user {}", requestingUserId, userId);

            Object userInfo = userService.getUserById(userId);

            if (userInfo != null) {
                return ResponseEntity.ok(ApiResponse.success("User information retrieved", userInfo));
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            log.error("Error retrieving user by ID {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve user information"));
        }
    }

    /**
     * Search users by username (partial match)
     * Requires authentication - for finding conversation partners
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Object>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {

        try {
            Integer requestingUserId = (Integer) authentication.getPrincipal();
            log.debug("User {} searching for users with query: '{}'", requestingUserId, query);

            Object searchResults = userService.searchUsers(query, limit);

            return ResponseEntity.ok(ApiResponse.success("User search completed", searchResults));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid user search query: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid search parameters"));

        } catch (Exception e) {
            log.error("Error searching users: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to search users"));
        }
    }
}
