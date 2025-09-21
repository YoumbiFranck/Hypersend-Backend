package com.thm_modul.login_service.dto;

public record UserInfoResponse(
        Integer userId,
        String username,
        String email
) {}