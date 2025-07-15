package com.example.conversation_service.dto;

import java.util.UUID;
import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConversationResponse {
    private UUID conversationId;
    private UUID user1Id;
    private UUID user2Id;
    private Instant createdAt;
}