package com.example.conversation_service.controller;

import com.example.common.dto.ApiResponse;
import com.example.conversation_service.dto.ConversationResponse;
import com.example.conversation_service.dto.CreateConversationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.conversation_service.service.ConversationService;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @RequestBody CreateConversationRequest request) {
        ApiResponse<ConversationResponse> response = conversationService.createConversation(
                request.getUser1Id(),
                request.getUser2Id()
        );
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}