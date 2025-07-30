package com.example.auth_service.kafka;

import com.example.auth_service.repository.UserRepository;
import com.example.common.constants.KafkaTopics;
import com.example.common.dto.UserValidationRequest;
import com.example.common.dto.UserValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserValidationKafkaService {
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = KafkaTopics.USER_EXISTENCE_REQUEST)
    public void validateUsers(
            @Payload UserValidationRequest request,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header("kafka_correlationId") byte[] correlationIdBytes
    ) {
        String correlationId = new String(correlationIdBytes, StandardCharsets.UTF_8);

        log.info("Processing validation for correlationId: {}, key: {}", correlationId, key);

        try {
            List<UUID> existingUsers = userRepository.findAllExistingUserIds(request.userIds());
            boolean allUsersExist = existingUsers.size() == request.userIds().size();

            UserValidationResponse response = new UserValidationResponse(
                    correlationId,
                    allUsersExist,
                    allUsersExist ? "All users exist" : "Missing users"
            );

            // Create ProducerRecord with Object type
            ProducerRecord<String, Object> responseRecord = new ProducerRecord<>(
                    KafkaTopics.USER_EXISTENCE_RESPONSE,
                    response
            );

            // Add the correlation ID header back to the response
            responseRecord.headers().add("kafka_correlationId", correlationIdBytes);

            kafkaTemplate.send(responseRecord).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send response for correlationId: {}", correlationId, ex);
                } else {
                    log.debug("Response sent successfully for correlationId: {}", correlationId);
                }
            });

        } catch (Exception e) {
            log.error("Validation failed for correlationId: {}", correlationId, e);

            UserValidationResponse errorResponse = new UserValidationResponse(
                    correlationId,
                    false,
                    "Error: " + e.getMessage()
            );

            ProducerRecord<String, Object> errorRecord = new ProducerRecord<>(
                    KafkaTopics.USER_EXISTENCE_RESPONSE,
                    errorResponse
            );

            errorRecord.headers().add("kafka_correlationId", correlationIdBytes);
            kafkaTemplate.send(errorRecord);
        }
    }
}