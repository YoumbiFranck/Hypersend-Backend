package com.thm_modul.message_service.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * DTO for sending a new message
 * Contains receiver ID and message content
 */
public record MessageRequest(

        @NotNull(message = "Receiver ID is required")
        Integer receiverId,

        @NotBlank(message = "Message content cannot be empty")
        @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
        String content
) {}
