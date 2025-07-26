package com.example.conversation_service.service;

import com.example.common.constants.KafkaTopics;
import com.example.common.dto.ApiResponse;
import com.example.common.dto.UserValidationRequest;
import com.example.common.dto.UserValidationResponse;
import com.example.conversation_service.dto.ConversationResponse;
import com.example.conversation_service.entity.Conversation;
import com.example.conversation_service.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final ReplyingKafkaTemplate<String, UserValidationRequest, UserValidationResponse> kafkaTemplate;
    private final ConversationRepository conversationRepo;

    @Override
    public ApiResponse<ConversationResponse> createConversation(UUID user1Id, UUID user2Id) {
        String correlationId = UUID.randomUUID().toString();
        final String path = "api/conversations";

        try {
            ProducerRecord<String, UserValidationRequest> record = new ProducerRecord<>(
                    KafkaTopics.USER_EXISTENCE_REQUEST,
                    correlationId,
                    new UserValidationRequest(correlationId, List.of(user1Id, user2Id))
            );

            RequestReplyFuture<String, UserValidationRequest, UserValidationResponse> future =
                    kafkaTemplate.sendAndReceive(record);

            ConsumerRecord<String, UserValidationResponse> responseRecord =
                    future.get(5, TimeUnit.SECONDS);

            UserValidationResponse validationResponse = responseRecord.value();
            if (!validationResponse.isValid()) {
                return buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Invalid user(s)",
                        validationResponse.message(),
                        path
                );
            }

            UUID orderedUser1Id = user1Id.compareTo(user2Id) < 0 ? user1Id : user2Id;
            UUID orderedUser2Id = user1Id.compareTo(user2Id) < 0 ? user2Id : user1Id;

            if (conversationRepo.existsByUser1IdAndUser2Id(orderedUser1Id, orderedUser2Id)) {
                return buildErrorResponse(
                        HttpStatus.CONFLICT,
                        "Conversation exists",
                        "Conversation between these users already exists",
                        path
                );
            }

            Conversation conversation = Conversation.builder()
                    .user1Id(orderedUser1Id)
                    .user2Id(orderedUser2Id)
                    .build();

            Conversation savedConversation = conversationRepo.save(conversation);

            ConversationResponse responseData = new ConversationResponse(
                    savedConversation.getId(),
                    savedConversation.getUser1Id(),
                    savedConversation.getUser2Id(),
                    savedConversation.getCreatedAt()
            );
            return ApiResponse.<ConversationResponse>builder()
                    .data(responseData)
                    .timestamp(LocalDateTime.now())
                    .status(HttpStatus.CREATED.value())
                    .reason(HttpStatus.CREATED.getReasonPhrase())
                    .message("Conversation created successfully")
                    .path(path)
                    .build();


        } catch (TimeoutException e) {
            log.error("Kafka timeout validating users: {}", e.getMessage());
            return buildErrorResponse(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "Validation service timeout",
                    "User validation service did not respond",
                    path
            );
        } catch (Exception e) {
            log.error("Conversation creation failed: {}", e.getMessage());
            return buildErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error",
                    e.getMessage(),
                    path
            );
        }
    }



    private ApiResponse<ConversationResponse> buildErrorResponse(
            HttpStatus status,
            String reason,
            String message,
            String path
    ) {
        return ApiResponse.<ConversationResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .reason(reason)
                .message(message)
                .path(path)
                .build();
    }
}