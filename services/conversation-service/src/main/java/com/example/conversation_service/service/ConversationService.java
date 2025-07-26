package com.example.conversation_service.service;

import com.example.common.dto.ApiResponse;
import com.example.conversation_service.dto.ConversationResponse;
import java.util.UUID;

public interface ConversationService {
    ApiResponse<ConversationResponse> createConversation(UUID user1Id, UUID user2Id);

}
