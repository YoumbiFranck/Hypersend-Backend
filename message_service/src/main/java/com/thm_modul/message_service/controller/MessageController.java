package com.thm_modul.message_service.controller;

import com.thm_modul.message_service.dto.ApiResponse;
import com.thm_modul.message_service.dto.ConversationResponse;
import com.thm_modul.message_service.dto.MessageRequest;
import com.thm_modul.message_service.dto.MessageResponse;
import com.thm_modul.message_service.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * Send a new message to another user
     * Authentication required - sender ID extracted from JWT token
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @Valid @RequestBody MessageRequest messageRequest,
            Authentication authentication) {

        try {
            // Extract sender ID from authentication (set by JWT filter)
            Integer senderId = (Integer) authentication.getPrincipal();

            log.info("User {} sending message to user {}", senderId, messageRequest.receiverId());

            MessageResponse response = messageService.sendMessage(senderId, messageRequest);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Message sent successfully", response));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid message request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid request: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to send message"));
        }
    }

    /**
     * Get conversation between authenticated user and another user
     * Returns full conversation with all messages
     */
    @GetMapping("/conversation/{otherUserId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @PathVariable Integer otherUserId,
            Authentication authentication) {

        try {
            Integer userId = (Integer) authentication.getPrincipal();

            log.debug("User {} requesting conversation with user {}", userId, otherUserId);

            ConversationResponse conversation = messageService.getConversation(userId, otherUserId);

            return ResponseEntity.ok(ApiResponse.success("Conversation retrieved successfully", conversation));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid conversation request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid request: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Error retrieving conversation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve conversation"));
        }
    }

    /**
     * Get conversation with pagination support
     * Useful for large conversations to avoid loading all messages at once
     */
    @GetMapping("/conversation/{otherUserId}/paginated")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversationPaginated(
            @PathVariable Integer otherUserId,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            Authentication authentication) {

        try {
            Integer userId = (Integer) authentication.getPrincipal();

            log.debug("User {} requesting paginated conversation with user {} (page: {}, size: {})",
                    userId, otherUserId, page, size);

            ConversationResponse conversation = messageService.getConversationWithPagination(
                    userId, otherUserId, page, size);

            return ResponseEntity.ok(ApiResponse.success("Conversation page retrieved successfully", conversation));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid paginated conversation request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid request: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Error retrieving paginated conversation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve conversation"));
        }
    }

    /**
     * Get list of all conversations for the authenticated user
     * Returns conversation summaries without full message content
     */
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getUserConversations(
            Authentication authentication) {

        try {
            Integer userId = (Integer) authentication.getPrincipal();

            log.debug("User {} requesting all conversations", userId);

            List<ConversationResponse> conversations = messageService.getUserConversations(userId);

            return ResponseEntity.ok(ApiResponse.success("Conversations retrieved successfully", conversations));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid user conversations request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid request: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Error retrieving user conversations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve conversations"));
        }
    }

    /**
     * Get user's message history (both sent and received messages)
     * Useful for user profile or message search functionality
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessageHistory(
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) Integer limit,
            Authentication authentication) {

        try {
            Integer userId = (Integer) authentication.getPrincipal();

            log.debug("User {} requesting message history (limit: {})", userId, limit);

            List<MessageResponse> messages = messageService.getUserMessageHistory(userId, limit);

            return ResponseEntity.ok(ApiResponse.success("Message history retrieved successfully", messages));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid message history request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid request: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Error retrieving message history: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve message history"));
        }
    }

    /**
     * Health check endpoint for the message service
     * Can be used by load balancers or monitoring systems
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Message service is running"));
    }

    /**
     * Get conversation summary between authenticated user and another user
     * Returns only basic conversation info without message list
     */
    @GetMapping("/conversation/{otherUserId}/summary")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversationSummary(
            @PathVariable Integer otherUserId,
            Authentication authentication) {

        try {
            Integer userId = (Integer) authentication.getPrincipal();

            log.debug("User {} requesting conversation summary with user {}", userId, otherUserId);

            // Get conversation but we'll create a summary version
            ConversationResponse fullConversation = messageService.getConversation(userId, otherUserId);

            // Create summary without messages
            ConversationResponse summary = ConversationResponse.summary(
                    fullConversation.otherUserId(),
                    fullConversation.otherUsername(),
                    fullConversation.lastMessage(),
                    fullConversation.lastMessageTime(),
                    fullConversation.totalMessages()
            );

            return ResponseEntity.ok(ApiResponse.success("Conversation summary retrieved successfully", summary));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid conversation summary request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid request: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Error retrieving conversation summary: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve conversation summary"));
        }
    }

    /**
     * Get basic statistics about user's messaging activity
     * Returns total sent, received, and conversation count
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<MessageStatsResponse>> getMessageStats(
            Authentication authentication) {

        try {
            Integer userId = (Integer) authentication.getPrincipal();

            log.debug("User {} requesting message statistics", userId);

            // Get user conversations to calculate stats
            List<ConversationResponse> conversations = messageService.getUserConversations(userId);
            List<MessageResponse> messageHistory = messageService.getUserMessageHistory(userId, 1000);

            // Calculate statistics
            int totalConversations = conversations.size();
            long totalSent = messageHistory.stream()
                    .mapToLong(msg -> msg.senderId().equals(userId) ? 1 : 0)
                    .sum();
            long totalReceived = messageHistory.stream()
                    .mapToLong(msg -> msg.receiverId().equals(userId) ? 1 : 0)
                    .sum();

            MessageStatsResponse stats = new MessageStatsResponse(
                    totalConversations,
                    (int) totalSent,
                    (int) totalReceived,
                    messageHistory.size()
            );

            return ResponseEntity.ok(ApiResponse.success("Message statistics retrieved successfully", stats));

        } catch (Exception e) {
            log.error("Error retrieving message statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve message statistics"));
        }
    }

    /**
     * Internal DTO for message statistics
     */
    public record MessageStatsResponse(
            int totalConversations,
            int totalSent,
            int totalReceived,
            int totalMessages
    ) {}
}
