package com.thm_modul.api_gateway.config;

import com.thm_modul.api_gateway.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Main security configuration for API Gateway
     * Handles authentication for all incoming requests
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // Disable CSRF since we use JWT tokens
                .csrf(csrf -> csrf.disable())

                // Stateless session management - no HTTP sessions
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure request authorization
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints - no authentication required
                        .antMatchers("/api/v1/auth/login").permitAll()
                        .antMatchers("/api/v1/auth/refresh").permitAll()
                        .antMatchers("/api/v1/users/register").permitAll()

                        // Health and monitoring endpoints
                        .antMatchers("/actuator/**").permitAll()
                        .antMatchers("/api/v1/health").permitAll()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Add JWT authentication filter before standard authentication
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }
}
