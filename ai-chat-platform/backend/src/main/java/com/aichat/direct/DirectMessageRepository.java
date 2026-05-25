package com.aichat.direct;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DirectMessageRepository extends JpaRepository<DirectMessageEntity, UUID> {
    List<DirectMessageEntity> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    List<DirectMessageEntity> findTop10ByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    boolean existsByConversationIdAndSenderIdAndCreatedAtAfter(
            UUID conversationId,
            UUID senderId,
            java.time.Instant createdAt
    );
}
