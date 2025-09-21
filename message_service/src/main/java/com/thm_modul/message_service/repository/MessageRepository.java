package com.thm_modul.message_service.repository;

import com.thm_modul.message_service.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Find all messages between two users ordered by creation date
     * Used for conversation display
     */
    @Query("SELECT m FROM Message m WHERE " +
            "(m.senderId = :userId1 AND m.receiverId = :userId2) OR " +
            "(m.senderId = :userId2 AND m.receiverId = :userId1) " +
            "ORDER BY m.createdAt ASC")
    List<Message> findMessagesBetweenUsers(@Param("userId1") Integer userId1,
                                           @Param("userId2") Integer userId2);

    /**
     * Find messages between two users with pagination
     * Used for large conversations
     */
    @Query("SELECT m FROM Message m WHERE " +
            "(m.senderId = :userId1 AND m.receiverId = :userId2) OR " +
            "(m.senderId = :userId2 AND m.receiverId = :userId1) " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findMessagesBetweenUsersWithPagination(@Param("userId1") Integer userId1,
                                                         @Param("userId2") Integer userId2,
                                                         Pageable pageable);

    /**
     * Find the latest message between two users
     * Used for conversation summaries
     */
    @Query("SELECT m FROM Message m WHERE " +
            "(m.senderId = :userId1 AND m.receiverId = :userId2) OR " +
            "(m.senderId = :userId2 AND m.receiverId = :userId1) " +
            "ORDER BY m.createdAt DESC")
    List<Message> findLatestMessageBetweenUsersQuery(@Param("userId1") Integer userId1,
                                                     @Param("userId2") Integer userId2,
                                                     Pageable pageable);

    /**
     * Default method to get the latest message between two users
     * Returns null if no messages found
     */
    default Message findLatestMessageBetweenUsers(Integer userId1, Integer userId2) {
        List<Message> messages = findLatestMessageBetweenUsersQuery(userId1, userId2,
                PageRequest.of(0, 1));
        return messages.isEmpty() ? null : messages.get(0);
    }

    /**
     * Find all distinct conversation partners for a user
     * Returns user IDs that have exchanged messages with the given user
     */
    @Query("SELECT DISTINCT " +
            "CASE WHEN m.senderId = :userId THEN m.receiverId " +
            "     ELSE m.senderId END " +
            "FROM Message m " +
            "WHERE m.senderId = :userId OR m.receiverId = :userId")
    List<Integer> findConversationPartners(@Param("userId") Integer userId);

    /**
     * Count total messages between two users
     * Used for conversation statistics
     */
    @Query("SELECT COUNT(m) FROM Message m WHERE " +
            "(m.senderId = :userId1 AND m.receiverId = :userId2) OR " +
            "(m.senderId = :userId2 AND m.receiverId = :userId1)")
    Long countMessagesBetweenUsers(@Param("userId1") Integer userId1,
                                   @Param("userId2") Integer userId2);

    /**
     * Find all messages sent by a specific user
     * Used for user message history
     */
    List<Message> findBySenderIdOrderByCreatedAtDesc(Integer senderId);

    /**
     * Find all messages received by a specific user
     * Used for user inbox
     */
    List<Message> findByReceiverIdOrderByCreatedAtDesc(Integer receiverId);

    /**
     * Check if user has any messages (sent or received)
     * Used for user validation
     */
    @Query("SELECT COUNT(m) > 0 FROM Message m WHERE m.senderId = :userId OR m.receiverId = :userId")
    boolean hasUserAnyMessages(@Param("userId") Integer userId);
}
