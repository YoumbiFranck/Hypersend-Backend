package com.thm_modul.login_service.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String username,
        String email
) {
    public static LoginResponse of(String accessToken, String refreshToken, long expiresIn, String username, String email) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn, username, email);
    }
}
