package com.thm_modul.login_service.service;

import com.thm_modul.login_service.dto.LoginRequest;
import com.thm_modul.login_service.dto.LoginResponse;
import com.thm_modul.login_service.dto.RefreshTokenRequest;
import com.thm_modul.login_service.entity.User;
import com.thm_modul.login_service.repository.UserRepository;
import com.thm_modul.login_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticate user and generate JWT tokens
     * Now handles authentication without Spring Security's AuthenticationManager
     */
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        log.debug("Attempting login for user: {}", loginRequest.usernameOrEmail());

        try {
            // Find user by username or email
            User user = userRepository.findByUserNameOrEmail(
                    loginRequest.usernameOrEmail(),
                    loginRequest.usernameOrEmail()
            ).orElseThrow(() -> new BadCredentialsException("User not found"));

            // Check if user is enabled
            if (!user.getEnabled()) {
                throw new BadCredentialsException("User account is disabled");
            }

            // Verify password manually since we don't have AuthenticationManager
            if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
                throw new BadCredentialsException("Invalid password");
            }

            // Update last login timestamp
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generate tokens with user ID included
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUserName());
            String accessToken = jwtUtil.generateToken(userDetails, user.getId());
            String refreshToken = jwtUtil.generateRefreshToken(userDetails, user.getId());

            log.info("Login successful for user: {} (ID: {})", user.getUserName(), user.getId());

            return LoginResponse.of(
                    accessToken,
                    refreshToken,
                    86400, // 24h in seconds
                    user.getUserName(),
                    user.getEmail()
            );

        } catch (BadCredentialsException e) {
            log.warn("Login failed for user: {} - {}", loginRequest.usernameOrEmail(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login for user: {}", loginRequest.usernameOrEmail(), e);
            throw new BadCredentialsException("Authentication failed");
        }
    }

    /**
     * Refresh JWT tokens
     */
    public LoginResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.refreshToken();

        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        try {
            String username = jwtUtil.extractUsername(refreshToken);
            Integer userId = jwtUtil.getUserIdFromToken(refreshToken);

            if (username == null || userId == null) {
                throw new BadCredentialsException("Invalid refresh token claims");
            }

            // Verify user still exists and is enabled
            User user = userRepository.findByIdAndEnabledTrue(userId)
                    .orElseThrow(() -> new BadCredentialsException("User not found or disabled"));

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Generate new tokens
            String newAccessToken = jwtUtil.generateToken(userDetails, userId);
            String newRefreshToken = jwtUtil.generateRefreshToken(userDetails, userId);

            log.debug("Token refresh successful for user: {} (ID: {})", username, userId);

            return LoginResponse.of(
                    newAccessToken,
                    newRefreshToken,
                    86400,
                    user.getUserName(),
                    user.getEmail()
            );

        } catch (Exception e) {
            log.warn("Token refresh failed: {}", e.getMessage());
            throw new BadCredentialsException("Token refresh failed");
        }
    }

    /**
     * Validate user credentials (without generating tokens)
     * Useful for internal validations
     */
    public boolean validateCredentials(String usernameOrEmail, String password) {
        try {
            User user = userRepository.findByUserNameOrEmail(usernameOrEmail, usernameOrEmail)
                    .orElse(null);

            return user != null
                    && user.getEnabled()
                    && passwordEncoder.matches(password, user.getPassword());

        } catch (Exception e) {
            log.warn("Error validating credentials for user: {}", usernameOrEmail, e);
            return false;
        }
    }

    /**
     * Get user information by ID
     * Used by other services for user data retrieval
     */
    @Transactional(readOnly = true)
    public User getUserById(Integer userId) {
        return userRepository.findByIdAndEnabledTrue(userId).orElse(null);
    }

    /**
     * Check if user exists and is enabled
     */
    @Transactional(readOnly = true)
    public boolean userExists(Integer userId) {
        return userRepository.existsByIdAndEnabledTrue(userId);
    }
}
