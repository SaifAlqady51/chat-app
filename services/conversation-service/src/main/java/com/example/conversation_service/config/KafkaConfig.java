package com.example.conversation_service.config;

import com.example.common.constants.KafkaTopics;
import com.example.common.dto.UserValidationRequest;
import com.example.common.dto.UserValidationResponse;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:conversation-service-group}")
    private String groupId;

    // Producer Configuration
    @Bean
    public ProducerFactory<String, UserValidationRequest> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    // Consumer Configuration for Reply Messages
    @Bean
    public ConsumerFactory<String, UserValidationResponse> replyConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-reply");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, UserValidationResponse.class.getName());
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserValidationResponse> replyListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserValidationResponse> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(replyConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    @Bean
    public ReplyingKafkaTemplate<String, UserValidationRequest, UserValidationResponse> replyingKafkaTemplate() {
        ConcurrentKafkaListenerContainerFactory<String, UserValidationResponse> factory = replyListenerContainerFactory();

        ConcurrentMessageListenerContainer<String, UserValidationResponse> replyContainer =
                factory.createContainer(KafkaTopics.USER_EXISTENCE_RESPONSE);

        replyContainer.getContainerProperties().setGroupId(groupId + "-reply-template");
        replyContainer.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        ReplyingKafkaTemplate<String, UserValidationRequest, UserValidationResponse> template =
                new ReplyingKafkaTemplate<>(producerFactory(), replyContainer);

        template.setDefaultReplyTimeout(Duration.ofSeconds(10));

        return template;
    }
}