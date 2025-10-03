package com.thm_modul.login_service.dto;

import javax.validation.constraints.NotBlank;

/**
 * DTO for token validation responses
 */
public record TokenValidationResponse(
        Integer userId,
        String username,
        boolean valid
) {}

