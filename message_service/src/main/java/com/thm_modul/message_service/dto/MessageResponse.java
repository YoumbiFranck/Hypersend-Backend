package com.thm_modul.message_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * DTO for message response
 * Contains all message information for client display
 */
public record MessageResponse(
        Long id,
        Integer senderId,
        String senderUsername,
        Integer receiverId,
        String receiverUsername,
        String content,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {

    /**
     * Factory method to create MessageResponse from Message entity
     * Username fields will be populated by the service layer
     */
    public static MessageResponse from(
            Long id,
            Integer senderId,
            String senderUsername,
            Integer receiverId,
            String receiverUsername,
            String content,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new MessageResponse(
                id,
                senderId,
                senderUsername,
                receiverId,
                receiverUsername,
                content,
                createdAt,
                updatedAt
        );
    }
}