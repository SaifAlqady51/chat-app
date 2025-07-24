package com.example.conversation_service.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {
    private UUID user1Id;
    private UUID user2Id;

}