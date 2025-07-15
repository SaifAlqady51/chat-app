package com.example.conversation_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@Setter
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "conversation_id")
    private UUID id;

    @Column(name = "user1_id", nullable = false, columnDefinition = "UUID")
    private UUID user1Id;

    @Column(name = "user2_id", nullable = false, columnDefinition = "UUID")
    private UUID user2Id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean involvesUser(UUID userId) {
        return user1Id.equals(userId) || user2Id.equals(userId);
    }

    public UUID getOtherParticipant(UUID userId) {
        if (user1Id.equals(userId)) {
            return user2Id;
        } else if (user2Id.equals(userId)) {
            return user1Id;
        }
        throw new IllegalArgumentException("User is not part of this conversation");
    }
}