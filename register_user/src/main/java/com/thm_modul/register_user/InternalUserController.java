package com.thm_modul.register_user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/internal/v1/register")
public class InternalUserController {

    private final UserService userService;

    @Value("${app.gateway.secret:shared_secret_key}")
    private String gatewaySecret;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Register a new user - called internally by API Gateway
     * Validates that the request comes from the API Gateway
     */
    @PostMapping
    public ResponseEntity<ApiResponse> registerUser(
            @Valid @RequestBody UserRegistrationRequest userRegistrationRequest,
            HttpServletRequest request) {

        // Validate that request comes from API Gateway
        if (!validateGatewayRequest(request)) {
            log.warn("Unauthorized internal request to register user from IP: {}",
                    getClientIP(request));
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden - Invalid gateway authentication"));
        }

        try {
            log.info("Internal request: Registering user: {}", userRegistrationRequest.userName());
            userService.registerUser(userRegistrationRequest);

            return ResponseEntity.ok(
                    ApiResponse.success("User registered successfully")
            );

        } catch (IllegalArgumentException e) {
            log.warn("User registration failed - validation error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Registration failed: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error during user registration: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Internal server error during registration"));
        }
    }

    /**
     * Health check endpoint for internal monitoring
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Register service is healthy"));
    }

    /**
     * Validate that the request comes from the API Gateway
     * Checks for the presence and validity of gateway secret header
     */
    private boolean validateGatewayRequest(HttpServletRequest request) {
        String gatewaySecretHeader = request.getHeader("X-Gateway-Secret");
        return gatewaySecret.equals(gatewaySecretHeader);
    }

    /**
     * Extract client IP address from request
     * Useful for logging and security monitoring
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
