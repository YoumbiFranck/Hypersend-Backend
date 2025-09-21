package com.thm_modul.login_service.controller;

import com.thm_modul.login_service.dto.*;
import com.thm_modul.login_service.entity.User;
import com.thm_modul.login_service.repository.UserRepository;
import com.thm_modul.login_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse loginResponse = authService.login(loginRequest);
            return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid credentials"));
        } catch (Exception e) {
            log.error("Login error", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Internal server error"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
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
    public ResponseEntity<ApiResponse<String>> validateToken(@RequestHeader("Authorization") String authHeader) {
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
    public ResponseEntity<ApiResponse<String>> validateUser(@PathVariable Integer userId) {
        try {
            boolean userExists = userRepository.existsById(userId);

            if (userExists) {
                return ResponseEntity.ok(ApiResponse.success("User exists"));
            } else {
                return ResponseEntity.notFound().build();
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
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserInfo(@PathVariable Integer userId) {
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
}
