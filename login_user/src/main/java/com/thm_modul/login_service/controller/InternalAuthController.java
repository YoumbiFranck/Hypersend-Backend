package com.thm_modul.login_service.controller;

import com.thm_modul.login_service.dto.*;
import com.thm_modul.login_service.entity.User;
import com.thm_modul.login_service.repository.UserRepository;
import com.thm_modul.login_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/internal/v1/auth")
@RequiredArgsConstructor
public class InternalAuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @Value("${app.gateway.secret:shared_secret_key}")
    private String gatewaySecret;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {


        if (!validateGatewayRequest(request)) {
            log.warn("Unauthorized internal login request from IP: {}", getClientIP(request));
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden - Invalid gateway authentication"));
        }

        try {
            log.info("Internal login request for user: {}", loginRequest.usernameOrEmail());

            LoginResponse loginResponse = authService.login(loginRequest);
            return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));

        } catch (BadCredentialsException e) {
            log.warn("Login failed - invalid credentials: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid credentials"));
        } catch (Exception e) {
            log.error("Login error", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest,
            HttpServletRequest request) {

        if (!validateGatewayRequest(request)) {
            log.warn("Unauthorized internal refresh request from IP: {}", getClientIP(request));
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden - Invalid gateway authentication"));
        }

        try {
            LoginResponse loginResponse = authService.refreshToken(refreshTokenRequest);
            return ResponseEntity.ok(ApiResponse.success("Token refreshed", loginResponse));
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid refresh token"));
        } catch (Exception e) {
            log.error("Token refresh error", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<String>> validateToken(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request) {

        if (!validateGatewayRequest(request)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden"));
        }

        try {
            String token = authHeader.replace("Bearer ", "");
            return ResponseEntity.ok(ApiResponse.success("Token is valid"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid token"));
        }
    }

    /**
     * Endpoint to validate if a user exists by userId
     * useful for other microservices to check user existence
     */
    @GetMapping("/validate-user/{userId}")
    public ResponseEntity<ApiResponse<UserExistenceResponse>> validateUser(
            @PathVariable Integer userId,
            HttpServletRequest request) {

        if (!validateGatewayRequest(request)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden"));
        }

        try {
            boolean userExists = userRepository.existsById(userId);
            UserExistenceResponse response = new UserExistenceResponse(userId, userExists);

            if (userExists) {
                return ResponseEntity.ok(ApiResponse.success("User validation completed", response));
            } else {
                return ResponseEntity.ok(ApiResponse.success("User validation completed", response));
            }
        } catch (Exception e) {
            log.error("Error validating user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error validating user"));
        }
    }

    /**
     * endpoint to get basic user info by userId
     * useful for other microservices to get user details
     */
    @GetMapping("/user-info/{userId}")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserInfo(
            @PathVariable Integer userId,
            HttpServletRequest request) {

        if (!validateGatewayRequest(request)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden"));
        }

        try {
            User user = userRepository.findById(userId).orElse(null);

            if (user != null) {
                UserInfoResponse userInfo = new UserInfoResponse(
                        user.getId(),
                        user.getUserName(),
                        user.getEmail()
                );
                return ResponseEntity.ok(ApiResponse.success("User found", userInfo));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting user info for {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error getting user info"));
        }
    }

    /**
     * Health check endpoint for internal monitoring
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Login service is healthy"));
    }

    /**
     * Validate that the request comes from the API Gateway
     */
    private boolean validateGatewayRequest(HttpServletRequest request) {
        String gatewaySecretHeader = request.getHeader("X-Gateway-Secret");
        return gatewaySecret.equals(gatewaySecretHeader);
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
