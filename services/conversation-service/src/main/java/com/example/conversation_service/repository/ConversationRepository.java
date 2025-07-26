package com.example.conversation_service.repository;

import com.example.conversation_service.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    boolean existsByUser1IdAndUser2Id(UUID user1Id, UUID user2Id);

    @Query("""
        SELECT c FROM Conversation c 
        WHERE (c.user1Id = :user1Id AND c.user2Id = :user2Id) 
           OR (c.user1Id = :user2Id AND c.user2Id = :user1Id)
        """)
    Optional<Conversation> findConversationBetweenUsers(
            @Param("user1Id") UUID user1Id,
            @Param("user2Id") UUID user2Id
    );

    @Query("""
        SELECT COUNT(c) > 0 FROM Conversation c 
        WHERE (c.user1Id = :user1Id AND c.user2Id = :user2Id) 
           OR (c.user1Id = :user2Id AND c.user2Id = :user1Id)
        """)
    boolean existsConversationBetweenUsers(
            @Param("user1Id") UUID user1Id,
            @Param("user2Id") UUID user2Id
    );

    @Query("""
        SELECT c FROM Conversation c 
        WHERE c.user1Id = :userId OR c.user2Id = :userId
        """)
    List<Conversation> findAllConversationsForUser(@Param("userId") UUID userId);

    @Query(value = """
        SELECT * FROM conversations 
        WHERE (user1_id = :user1Id AND user2_id = :user2Id) 
           OR (user1_id = :user2Id AND user2_id = :user1Id)
        LIMIT 1
        """, nativeQuery = true)
    Optional<Conversation> nativeFindConversationBetweenUsers(
            @Param("user1Id") UUID user1Id,
            @Param("user2Id") UUID user2Id
    );

    default boolean existsConversationBetweenUsersWithFlush(UUID user1Id, UUID user2Id) {
        flush();
        return existsConversationBetweenUsers(user1Id, user2Id);
    }
}