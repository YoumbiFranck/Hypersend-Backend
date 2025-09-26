package com.thm_modul.register_user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * Standard API response wrapper for register service
 * Provides consistent response format for internal communications
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse(
        boolean success,
        String message,
        Object data,
        String error
) {

    /**
     * Create success response with message only
     */
    public static ApiResponse success(String message) {
        return ApiResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * Create success response with message and data
     */
    public static ApiResponse success(String message, Object data) {
        return ApiResponse.builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Create error response with error message
     */
    public static ApiResponse error(String error) {
        return ApiResponse.builder()
                .success(false)
                .error(error)
                .build();
    }
}
