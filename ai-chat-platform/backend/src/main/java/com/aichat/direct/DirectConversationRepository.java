package com.aichat.direct;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DirectConversationRepository extends JpaRepository<DirectConversationEntity, UUID> {
    @Query("""
            select conversation
            from DirectConversationEntity conversation
            where conversation.requesterId = :userId or conversation.recipientId = :userId
            order by conversation.updatedAt desc
            """)
    List<DirectConversationEntity> findForUser(@Param("userId") UUID userId);

    @Query("""
            select conversation
            from DirectConversationEntity conversation
            where (conversation.requesterId = :firstUserId and conversation.recipientId = :secondUserId)
               or (conversation.requesterId = :secondUserId and conversation.recipientId = :firstUserId)
            """)
    Optional<DirectConversationEntity> findBetweenUsers(
            @Param("firstUserId") UUID firstUserId,
            @Param("secondUserId") UUID secondUserId
    );
}
