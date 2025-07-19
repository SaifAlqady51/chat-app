package com.example.common.events;

import java.util.UUID;

public class UserExistenceRequest {
    private String correlationId;
    private UUID userId;
    private String replyTopic;

    public UserExistenceRequest() {
        this.correlationId = UUID.randomUUID().toString();
    }

    public UserExistenceRequest(UUID userId, String replyTopic) {
        this();
        this.userId = userId;
        this.replyTopic = replyTopic;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getReplyTopic() {
        return replyTopic;
    }

    public void setReplyTopic(String replyTopic) {
        this.replyTopic = replyTopic;
    }
}
