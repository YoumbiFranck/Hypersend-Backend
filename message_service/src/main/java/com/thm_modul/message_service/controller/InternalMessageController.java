package com.thm_modul.message_service.controller;

import com.thm_modul.message_service.dto.ApiResponse;
import com.thm_modul.message_service.dto.ConversationResponse;
import com.thm_modul.message_service.dto.MessageRequest;
import com.thm_modul.message_service.dto.MessageResponse;
import com.thm_modul.message_service.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/internal/v1/messages")
@RequiredArgsConstructor
public class InternalMessageController {

    private final MessageService messageService;

    @Value("${app.gateway.secret:shared_secret_key}")
    private String gatewaySecret;

    /**
     * Send a new message to another user - called internally by API Gateway
     * User ID is extracted from Gateway headers
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @Valid @RequestBody MessageRequest messageRequest,
            HttpServletRequest request) {

        if (!validateGatewayRequest(request)) {
            log.warn("Unauthorized internal message send request from IP: {}", getClientIP(request));
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden - Invalid gateway authentication"));
        }

        try {
            // Extract user ID from Gateway headers
            Integer senderId = extractUserIdFromHeaders(request);
            if (senderId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Missing user information from gateway"));
            }

            log.info("Internal request: User {} sending message to user {}", senderId, messageRequest.receiverId());

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
     */
    @GetMapping("/conversation/{otherUserId}")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversation(
            @PathVariable Integer otherUserId,
            HttpServletRequest request) {

        if (!validateGatewayRequest(request)) {
            log.warn("Unauthorized internal conversation request from IP: {}", getClientIP(request));
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden - Invalid gateway authentication"));
        }

        try {
            Integer userId = extractUserIdFromHeaders(request);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Missing user information from gateway"));
            }

            log.debug("Internal request: User {} requesting conversation with user {}", userId, otherUserId);

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
     */
    @GetMapping("/conversation/{otherUserId}/paginated")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversationPaginated(
            @PathVariable Integer otherUserId,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            HttpServletRequest request) {

        if (!validateGatewayRequest(request)) {
            log.warn("Unauthorized internal paginated conversation request from IP: {}", getClientIP(request));
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden - Invalid gateway authentication"));
        }

        try {
            Integer userId = extractUserIdFromHeaders(request);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Missing user information from gateway"));
            }

            log.debug("Internal request: User {} requesting paginated conversation with user {} (page: {}, size: {})",
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
     */
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getUserConversations(
            HttpServletRequest request) {

        if (!validateGatewayRequest(request)) {
            log.warn("Unauthorized internal conversations request from IP: {}", getClientIP(request));
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden - Invalid gateway authentication"));
        }

        try {
            Integer userId = extractUserIdFromHeaders(request);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Missing user information from gateway"));
            }

            log.debug("Internal request: User {} requesting all conversations", userId);

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
     * Get user's message history
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessageHistory(
            @RequestParam(defaultValue = "50") @Min(1) @Max(500) Integer limit,
            HttpServletRequest request) {

        if (!validateGatewayRequest(request)) {
            log.warn("Unauthorized internal message history request from IP: {}", getClientIP(request));
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden - Invalid gateway authentication"));
        }

        try {
            Integer userId = extractUserIdFromHeaders(request);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Missing user information from gateway"));
            }

            log.debug("Internal request: User {} requesting message history (limit: {})", userId, limit);

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
     * Get conversation summary between authenticated user and another user
     */
    @GetMapping("/conversation/{otherUserId}/summary")
    public ResponseEntity<ApiResponse<ConversationResponse>> getConversationSummary(
            @PathVariable Integer otherUserId,
            HttpServletRequest request) {

        if (!validateGatewayRequest(request)) {
            log.warn("Unauthorized internal conversation summary request from IP: {}", getClientIP(request));
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden - Invalid gateway authentication"));
        }

        try {
            Integer userId = extractUserIdFromHeaders(request);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Missing user information from gateway"));
            }

            log.debug("Internal request: User {} requesting conversation summary with user {}", userId, otherUserId);

            ConversationResponse fullConversation = messageService.getConversation(userId, otherUserId);

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
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<MessageStatsResponse>> getMessageStats(
            HttpServletRequest request) {

        if (!validateGatewayRequest(request)) {
            log.warn("Unauthorized internal message stats request from IP: {}", getClientIP(request));
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden - Invalid gateway authentication"));
        }

        try {
            Integer userId = extractUserIdFromHeaders(request);
            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Missing user information from gateway"));
            }

            log.debug("Internal request: User {} requesting message statistics", userId);

            List<ConversationResponse> conversations = messageService.getUserConversations(userId);
            List<MessageResponse> messageHistory = messageService.getUserMessageHistory(userId, 1000);

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
     * Health check endpoint for internal monitoring
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Message service is healthy"));
    }

    /**
     * Validate that the request comes from the API Gateway
     */
    private boolean validateGatewayRequest(HttpServletRequest request) {
        String gatewaySecretHeader = request.getHeader("X-Gateway-Secret");
        return gatewaySecret.equals(gatewaySecretHeader);
    }

    /**
     * Extract user ID from Gateway headers
     */
    private Integer extractUserIdFromHeaders(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-ID");
        if (userIdHeader != null) {
            try {
                return Integer.valueOf(userIdHeader);
            } catch (NumberFormatException e) {
                log.warn("Invalid user ID in header: {}", userIdHeader);
            }
        }
        return null;
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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
