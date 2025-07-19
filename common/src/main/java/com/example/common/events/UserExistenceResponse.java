package com.example.common.events;

import java.util.UUID;

public class UserExistenceResponse {
    private String correlationId;
    private UUID userId;
    private boolean exists;
    private String message;

    public UserExistenceResponse() {}

    public UserExistenceResponse(String correlationId, UUID userId, boolean exists, String message) {
        this.correlationId = correlationId;
        this.userId = userId;
        this.exists = exists;
        this.message = message;
    }

    // Getters and setters
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

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}