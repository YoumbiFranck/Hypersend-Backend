// RefreshTokenRequest.java
package com.thm_modul.login_service.dto;

import javax.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}
