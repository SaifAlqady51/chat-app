package com.example.conversation_service.dto;

import java.util.UUID;
import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConversationResponse {
    private UUID id;
    private UUID user1Id;
    private UUID user2Id;
    private Instant createdAt;

    public ConversationResponse(UUID id, UUID user1Id, UUID user2Id, Instant createdAt) {
        this.id = id;
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.createdAt = createdAt;
    }

}