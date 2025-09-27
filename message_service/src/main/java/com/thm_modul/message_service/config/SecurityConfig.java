package com.thm_modul.message_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Security configuration for Message Service
     * Allows internal endpoints without authentication
     * while keeping public endpoints secured if needed
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // Disable CSRF since this is an internal service
                .csrf(csrf -> csrf.disable())

                // Stateless session management
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure request authorization
                .authorizeHttpRequests(authz -> authz
                        // Allow all internal endpoints (called by API Gateway)
                        .antMatchers("/internal/**").permitAll()

                        // Allow health and monitoring endpoints
                        .antMatchers("/actuator/**").permitAll()
                        .antMatchers("/health").permitAll()

                        // For this internal service, allow all requests
                        // since authentication is handled by API Gateway
                        .anyRequest().permitAll()
                )

                .build();
    }
}
