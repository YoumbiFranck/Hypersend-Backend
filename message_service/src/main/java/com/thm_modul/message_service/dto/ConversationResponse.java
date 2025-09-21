package com.thm_modul.message_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for conversation response
 * Contains conversation summary and list of messages between two users
 */
public record ConversationResponse(
        Integer otherUserId,
        String otherUsername,
        String lastMessage,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime lastMessageTime,

        Integer totalMessages,
        List<MessageResponse> messages
) {

    /**
     * Factory method to create a conversation summary without full message list
     * Used for conversation list endpoint
     */
    public static ConversationResponse summary(
            Integer otherUserId,
            String otherUsername,
            String lastMessage,
            LocalDateTime lastMessageTime,
            Integer totalMessages
    ) {
        return new ConversationResponse(
                otherUserId,
                otherUsername,
                lastMessage,
                lastMessageTime,
                totalMessages,
                null // No messages in summary
        );
    }

    /**
     * Factory method to create a full conversation with message list
     * Used for detailed conversation endpoint
     */
    public static ConversationResponse full(
            Integer otherUserId,
            String otherUsername,
            String lastMessage,
            LocalDateTime lastMessageTime,
            Integer totalMessages,
            List<MessageResponse> messages
    ) {
        return new ConversationResponse(
                otherUserId,
                otherUsername,
                lastMessage,
                lastMessageTime,
                totalMessages,
                messages
        );
    }
}
