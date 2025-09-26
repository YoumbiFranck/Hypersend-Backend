package com.thm_modul.login_service.dto;

public record UserExistenceResponse(
        Integer userId,
        boolean exists
) {}
