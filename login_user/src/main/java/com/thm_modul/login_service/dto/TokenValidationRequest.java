package com.thm_modul.login_service.dto;

import javax.validation.constraints.NotBlank;

/**
 * DTO for token validation requests from API Gateway
 */
public record TokenValidationRequest(
        @NotBlank(message = "Token is required")
        String token
) {}