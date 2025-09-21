package com.thm_modul.message_service.service;

import com.thm_modul.message_service.dto.ConversationResponse;
import com.thm_modul.message_service.dto.MessageRequest;
import com.thm_modul.message_service.dto.MessageResponse;
import com.thm_modul.message_service.entity.Message;
import com.thm_modul.message_service.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserValidationService userValidationService;

    /**
     * Send a new message from authenticated user to receiver
     * Validates both users exist before sending
     */
    @Transactional
    public MessageResponse sendMessage(Integer senderId, MessageRequest request) {
        log.info("Attempting to send message from user {} to user {}", senderId, request.receiverId());

        // Validate that both users exist
        if (!userValidationService.validateUserPair(senderId, request.receiverId())) {
            throw new IllegalArgumentException("Invalid sender or receiver user ID");
        }

        // Create and save the message
        Message message = Message.builder()
                .senderId(senderId)
                .receiverId(request.receiverId())
                .content(request.content().trim())
                .build();

        Message savedMessage = messageRepository.save(message);
        log.info("Message {} sent successfully from user {} to user {}",
                savedMessage.getId(), senderId, request.receiverId());

        // Convert to response DTO with usernames
        return convertToMessageResponse(savedMessage);
    }

    /**
     * Get conversation between authenticated user and another user
     * Returns messages ordered by creation date (oldest first)
     */
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(Integer userId, Integer otherUserId) {
        log.debug("Getting conversation between user {} and user {}", userId, otherUserId);

        // Validate that both users exist
        if (!userValidationService.validateUserPair(userId, otherUserId)) {
            throw new IllegalArgumentException("Invalid user IDs for conversation");
        }

        // Get all messages between the two users
        List<Message> messages = messageRepository.findMessagesBetweenUsers(userId, otherUserId);

        // Get usernames for both users
        Map<Integer, String> usernames = userValidationService.getUsernames(List.of(userId, otherUserId));
        String otherUsername = usernames.get(otherUserId);

        // Convert messages to response DTOs
        List<MessageResponse> messageResponses = messages.stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());

        // Get conversation metadata
        String lastMessage = messages.isEmpty() ? null :
                messages.get(messages.size() - 1).getContent();

        return ConversationResponse.full(
                otherUserId,
                otherUsername,
                lastMessage,
                messages.isEmpty() ? null : messages.get(messages.size() - 1).getCreatedAt(),
                messages.size(),
                messageResponses
        );
    }

    /**
     * Get conversation with pagination support
     * Useful for large conversations
     */
    @Transactional(readOnly = true)
    public ConversationResponse getConversationWithPagination(Integer userId, Integer otherUserId,
                                                              int page, int size) {
        log.debug("Getting paginated conversation between user {} and user {} (page: {}, size: {})",
                userId, otherUserId, page, size);

        // Validate users
        if (!userValidationService.validateUserPair(userId, otherUserId)) {
            throw new IllegalArgumentException("Invalid user IDs for conversation");
        }

        // Create pageable (sorted by creation date descending for recent messages first)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Message> messagePage = messageRepository.findMessagesBetweenUsersWithPagination(
                userId, otherUserId, pageable);

        // Get usernames
        Map<Integer, String> usernames = userValidationService.getUsernames(List.of(userId, otherUserId));
        String otherUsername = usernames.get(otherUserId);

        // Convert messages (reverse to get chronological order)
        List<MessageResponse> messageResponses = messagePage.getContent().stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());

        // Reverse the list to show chronological order (oldest first)
        messageResponses = messageResponses.stream()
                .collect(Collectors.toList());

        // Get latest message for summary
        Message latestMessage = messageRepository.findLatestMessageBetweenUsers(userId, otherUserId);
        String lastMessage = latestMessage != null ? latestMessage.getContent() : null;

        return ConversationResponse.full(
                otherUserId,
                otherUsername,
                lastMessage,
                latestMessage != null ? latestMessage.getCreatedAt() : null,
                (int) messagePage.getTotalElements(),
                messageResponses
        );
    }

    /**
     * Get list of all conversations for a user
     * Returns conversation summaries without full message content
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> getUserConversations(Integer userId) {
        log.debug("Getting all conversations for user {}", userId);

        // Validate user exists
        if (!userValidationService.userExists(userId)) {
            throw new IllegalArgumentException("User not found");
        }

        // Get all conversation partners
        List<Integer> partnerIds = messageRepository.findConversationPartners(userId);

        if (partnerIds.isEmpty()) {
            log.debug("No conversations found for user {}", userId);
            return new ArrayList<>();
        }

        // Get usernames for all partners
        Map<Integer, String> usernames = userValidationService.getUsernames(partnerIds);

        // Build conversation summaries
        List<ConversationResponse> conversations = new ArrayList<>();

        for (Integer partnerId : partnerIds) {
            // Get latest message and total count
            Message latestMessage = messageRepository.findLatestMessageBetweenUsers(userId, partnerId);
            Long totalMessages = messageRepository.countMessagesBetweenUsers(userId, partnerId);

            if (latestMessage != null) {
                ConversationResponse conversation = ConversationResponse.summary(
                        partnerId,
                        usernames.get(partnerId),
                        latestMessage.getContent(),
                        latestMessage.getCreatedAt(),
                        totalMessages.intValue()
                );
                conversations.add(conversation);
            }
        }

        // Sort conversations by last message time (most recent first)
        conversations.sort((c1, c2) -> c2.lastMessageTime().compareTo(c1.lastMessageTime()));

        log.debug("Found {} conversations for user {}", conversations.size(), userId);
        return conversations;
    }

    /**
     * Get user's message history (sent and received)
     * Useful for user profile or admin purposes
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> getUserMessageHistory(Integer userId, int limit) {
        log.debug("Getting message history for user {} (limit: {})", userId, limit);

        // Validate user exists
        if (!userValidationService.userExists(userId)) {
            throw new IllegalArgumentException("User not found");
        }

        // Get sent messages
        List<Message> sentMessages = messageRepository.findBySenderIdOrderByCreatedAtDesc(userId);

        // Get received messages
        List<Message> receivedMessages = messageRepository.findByReceiverIdOrderByCreatedAtDesc(userId);

        // Combine and sort all messages by creation date (most recent first)
        List<Message> allMessages = new ArrayList<>();
        allMessages.addAll(sentMessages);
        allMessages.addAll(receivedMessages);

        allMessages.sort((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt()));

        // Apply limit
        List<Message> limitedMessages = allMessages.stream()
                .limit(limit)
                .collect(Collectors.toList());

        // Convert to response DTOs
        return limitedMessages.stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert Message entity to MessageResponse DTO
     * Includes username lookup
     */
    private MessageResponse convertToMessageResponse(Message message) {
        // Get usernames for sender and receiver
        Map<Integer, String> usernames = userValidationService.getUsernames(
                List.of(message.getSenderId(), message.getReceiverId()));

        return MessageResponse.from(
                message.getId(),
                message.getSenderId(),
                usernames.get(message.getSenderId()),
                message.getReceiverId(),
                usernames.get(message.getReceiverId()),
                message.getContent(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }
}
