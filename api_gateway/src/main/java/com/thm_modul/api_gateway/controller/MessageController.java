package com.thm_modul.api_gateway.controller;

import com.thm_modul.api_gateway.dto.ApiResponse;
import com.thm_modul.api_gateway.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * Send a new message to another user
     * Requires authentication
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Object>> sendMessage(
            @Valid @RequestBody Map<String, Object> messageRequest,
            Authentication authentication) {

        try {
            Integer senderId = (Integer) authentication.getPrincipal();
            String senderUsername = (String) authentication.getCredentials();

            log.info("User {} sending message to user {}", senderId, messageRequest.get("receiverId"));

            Object messageResponse = messageService.sendMessage(senderId, senderUsername, messageRequest);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Message sent successfully", messageResponse));

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
     */
    @GetMapping("/conversation/{otherUserId}")
    public ResponseEntity<ApiResponse<Object>> getConversation(
            @PathVariable Integer otherUserId,
            Authentication authentication) {

        try {
            Integer userId = (Integer) authentication.getPrincipal();
            String username = (String) authentication.getCredentials();

            log.debug("User {} requesting conversation with user {}", userId, otherUserId);

            Object conversation = messageService.getConversation(userId, username, otherUserId);

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
     */
    @GetMapping("/conversation/{otherUserId}/paginated")
    public ResponseEntity<ApiResponse<Object>> getConversationPaginated(
            @PathVariable Integer otherUserId,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            Authentication authentication) {

        try {
            Integer userId = (Integer) authentication.getPrincipal();
            String username = (String) authentication.getCredentials();

            log.debug("User {} requesting paginated conversation with user {} (page: {}, size: {})",
                    userId, otherUserId, page, size);

            Object conversation = messageService.getConversationPaginated(userId, username, otherUserId, page, size);

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
     */
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<Object>> getUserConversations(Authentication authentication) {

        try {
            Integer userId = (Integer) authentication.getPrincipal();
            String username = (String) authentication.getCredentials();

            log.debug("User {} requesting all conversations", userId);

            Object conversations = messageService.getUserConversations(userId, username);

            return ResponseEntity.ok(ApiResponse.success("Conversations retrieved successfully", conversations));

        } catch (Exception e) {
            log.error("Error retrieving user conversations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve conversations"));
        }
    }

    /**
     * Get user's message history
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Object>> getMessageHistory(
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) Integer limit,
            Authentication authentication) {

        try {
            Integer userId = (Integer) authentication.getPrincipal();
            String username = (String) authentication.getCredentials();

            log.debug("User {} requesting message history (limit: {})", userId, limit);

            Object messageHistory = messageService.getMessageHistory(userId, username, limit);

            return ResponseEntity.ok(ApiResponse.success("Message history retrieved successfully", messageHistory));

        } catch (Exception e) {
            log.error("Error retrieving message history: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve message history"));
        }
    }

    /**
     * Get conversation summary
     */
    @GetMapping("/conversation/{otherUserId}/summary")
    public ResponseEntity<ApiResponse<Object>> getConversationSummary(
            @PathVariable Integer otherUserId,
            Authentication authentication) {

        try {
            Integer userId = (Integer) authentication.getPrincipal();
            String username = (String) authentication.getCredentials();

            log.debug("User {} requesting conversation summary with user {}", userId, otherUserId);

            Object summary = messageService.getConversationSummary(userId, username, otherUserId);

            return ResponseEntity.ok(ApiResponse.success("Conversation summary retrieved successfully", summary));

        } catch (Exception e) {
            log.error("Error retrieving conversation summary: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve conversation summary"));
        }
    }

    /**
     * Get message statistics for the authenticated user
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Object>> getMessageStats(Authentication authentication) {

        try {
            Integer userId = (Integer) authentication.getPrincipal();
            String username = (String) authentication.getCredentials();

            log.debug("User {} requesting message statistics", userId);

            Object stats = messageService.getMessageStats(userId, username);

            return ResponseEntity.ok(ApiResponse.success("Message statistics retrieved successfully", stats));

        } catch (Exception e) {
            log.error("Error retrieving message statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve message statistics"));
        }
    }
}