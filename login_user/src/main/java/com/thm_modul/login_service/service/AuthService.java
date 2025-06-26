package com.thm_modul.login_service.service;

import com.thm_modul.login_service.dto.LoginRequest;
import com.thm_modul.login_service.dto.LoginResponse;
import com.thm_modul.login_service.dto.RefreshTokenRequest;
import com.thm_modul.login_service.entity.User;
import com.thm_modul.login_service.repository.UserRepository;
import com.thm_modul.login_service.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        log.info("Attempting login for user: {}", loginRequest.usernameOrEmail());

        try {
            // Authentification
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.usernameOrEmail(),
                            loginRequest.password()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Récupération de l'utilisateur pour mise à jour du lastLogin
            User user = userRepository.findByUserNameOrEmail(
                    loginRequest.usernameOrEmail(),
                    loginRequest.usernameOrEmail()
            ).orElseThrow(() -> new BadCredentialsException("User not found"));

            // Mise à jour du dernier login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Génération des tokens
            String accessToken = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            log.info("Login successful for user: {}", user.getUserName());

            return LoginResponse.of(
                    accessToken,
                    refreshToken,
                    86400, // 24h en secondes
                    user.getUserName(),
                    user.getEmail()
            );

        } catch (BadCredentialsException e) {
            log.warn("Login failed for user: {} - Invalid credentials", loginRequest.usernameOrEmail());
            throw new BadCredentialsException("Invalid username/email or password");
        }
    }

    public LoginResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.refreshToken();

        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String username = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        String newAccessToken = jwtUtil.generateToken(userDetails);
        String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

        User user = userRepository.findByUserNameOrEmail(username, username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        return LoginResponse.of(
                newAccessToken,
                newRefreshToken,
                86400,
                user.getUserName(),
                user.getEmail()
        );
    }
}
