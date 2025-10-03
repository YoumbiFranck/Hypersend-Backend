package com.thm_modul.api_gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final RestTemplate restTemplate;

    @Value("${app.message-service.url}")
    private String messageServiceUrl;

    @Value("${app.gateway.secret}")
    private String gatewaySecret;

    /**
     * Send a message via message service
     */
    public Object sendMessage(Integer senderId, String senderUsername, Map<String, Object> messageRequest) {
        try {
            HttpHeaders headers = createInternalHeadersWithUser(senderId, senderUsername);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(messageRequest, headers);

            String url = messageServiceUrl + "/internal/v1/messages/send";

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    log.debug("Message sent successfully via message service");
                    return responseBody.get("data");
                } else {
                    String error = (String) responseBody.get("error");
                    throw new IllegalArgumentException(error != null ? error : "Failed to send message");
                }
            }

            throw new RuntimeException("Invalid response from message service");

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error communicating with message service: {}", e.getMessage(), e);
            throw new RuntimeException("Message service unavailable");
        }
    }

    /**
     * Get conversation between users via message service
     */
    public Object getConversation(Integer userId, String username, Integer otherUserId) {
        try {
            HttpHeaders headers = createInternalHeadersWithUser(userId, username);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            String url = messageServiceUrl + "/internal/v1/messages/conversation/" + otherUserId;

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    return responseBody.get("data");
                } else {
                    String error = (String) responseBody.get("error");
                    throw new IllegalArgumentException(error != null ? error : "Failed to retrieve conversation");
                }
            }

            throw new RuntimeException("Invalid response from message service");

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting conversation from message service: {}", e.getMessage(), e);
            throw new RuntimeException("Message service unavailable");
        }
    }

    /**
     * Get paginated conversation via message service
     */
    public Object getConversationPaginated(Integer userId, String username, Integer otherUserId, Integer page, Integer size) {
        try {
            HttpHeaders headers = createInternalHeadersWithUser(userId, username);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            String url = messageServiceUrl + "/internal/v1/messages/conversation/" + otherUserId +
                    "/paginated?page=" + page + "&size=" + size;

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    return responseBody.get("data");
                } else {
                    String error = (String) responseBody.get("error");
                    throw new IllegalArgumentException(error != null ? error : "Failed to retrieve conversation");
                }
            }

            throw new RuntimeException("Invalid response from message service");

        } catch (Exception e) {
            log.error("Error getting paginated conversation from message service: {}", e.getMessage(), e);
            throw new RuntimeException("Message service unavailable");
        }
    }

    /**
     * Get user conversations via message service
     */
    public Object getUserConversations(Integer userId, String username) {
        try {
            HttpHeaders headers = createInternalHeadersWithUser(userId, username);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            String url = messageServiceUrl + "/internal/v1/messages/conversations";

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    return responseBody.get("data");
                } else {
                    return java.util.List.of(); // Return empty list if no conversations
                }
            }

            return java.util.List.of();

        } catch (Exception e) {
            log.error("Error getting user conversations from message service: {}", e.getMessage(), e);
            throw new RuntimeException("Message service unavailable");
        }
    }

    /**
     * Get message history via message service
     */
    public Object getMessageHistory(Integer userId, String username, Integer limit) {
        try {
            HttpHeaders headers = createInternalHeadersWithUser(userId, username);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            String url = messageServiceUrl + "/internal/v1/messages/history?limit=" + limit;

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    return responseBody.get("data");
                } else {
                    return java.util.List.of();
                }
            }

            return java.util.List.of();

        } catch (Exception e) {
            log.error("Error getting message history from message service: {}", e.getMessage(), e);
            throw new RuntimeException("Message service unavailable");
        }
    }

    /**
     * Get conversation summary via message service
     */
    public Object getConversationSummary(Integer userId, String username, Integer otherUserId) {
        try {
            HttpHeaders headers = createInternalHeadersWithUser(userId, username);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            String url = messageServiceUrl + "/internal/v1/messages/conversation/" + otherUserId + "/summary";

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    return responseBody.get("data");
                } else {
                    return null;
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Error getting conversation summary from message service: {}", e.getMessage(), e);
            throw new RuntimeException("Message service unavailable");
        }
    }

    /**
     * Get message statistics via message service
     */
    public Object getMessageStats(Integer userId, String username) {
        try {
            HttpHeaders headers = createInternalHeadersWithUser(userId, username);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            String url = messageServiceUrl + "/internal/v1/messages/stats";

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    return responseBody.get("data");
                } else {
                    return Map.of(
                            "totalConversations", 0,
                            "totalSent", 0,
                            "totalReceived", 0,
                            "totalMessages", 0
                    );
                }
            }

            return Map.of(
                    "totalConversations", 0,
                    "totalSent", 0,
                    "totalReceived", 0,
                    "totalMessages", 0
            );

        } catch (Exception e) {
            log.error("Error getting message stats from message service: {}", e.getMessage(), e);
            throw new RuntimeException("Message service unavailable");
        }
    }

    /**
     * Create headers for internal service communication
     */
    private HttpHeaders createInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Gateway-Secret", gatewaySecret);
        return headers;
    }

    /**
     * Create headers with user context for internal service communication
     */
    private HttpHeaders createInternalHeadersWithUser(Integer userId, String username) {
        HttpHeaders headers = createInternalHeaders();
        headers.set("X-User-ID", String.valueOf(userId));
        if (username != null) {
            headers.set("X-Username", username);
        }
        return headers;
    }
}
