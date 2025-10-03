package com.thm_modul.api_gateway.controller;

import com.thm_modul.api_gateway.dto.ApiResponse;
import com.thm_modul.api_gateway.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Public login endpoint
     * Authenticates user and returns JWT tokens via login service
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Object>> login(@Valid @RequestBody Map<String, String> loginRequest) {
        try {
            log.info("Login request received for user: {}", loginRequest.get("usernameOrEmail"));

            Object loginResponse = authService.authenticateUser(loginRequest);

            return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));

        } catch (IllegalArgumentException e) {
            log.warn("Login failed - invalid credentials: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid credentials"));

        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Internal server error during login"));
        }
    }

    /**
     * Public refresh token endpoint
     * Refreshes JWT tokens via login service
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Object>> refreshToken(@Valid @RequestBody Map<String, String> refreshRequest) {
        try {
            log.debug("Token refresh request received");

            Object refreshResponse = authService.refreshToken(refreshRequest);

            return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", refreshResponse));

        } catch (IllegalArgumentException e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid refresh token"));

        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Internal server error during token refresh"));
        }
    }

    /**
     * Validate current user's token and return user info
     * Requires authentication via JWT
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Object>> getCurrentUser() {
        try {
            Object userInfo = authService.getCurrentUserInfo();

            return ResponseEntity.ok(ApiResponse.success("User information retrieved", userInfo));

        } catch (Exception e) {
            log.error("Error getting current user info: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve user information"));
        }
    }

    /**
     * Logout endpoint (optional - mainly for client-side token cleanup)
     * Since we use stateless JWT, this is primarily informational
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        log.debug("Logout request received");

        // In a stateless JWT system, logout is handled client-side by discarding the token
        // This endpoint exists for consistency and potential future token blacklisting

        return ResponseEntity.ok(ApiResponse.success("Logout successful - please discard your tokens"));
    }
}
