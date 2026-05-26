package com.aichat.direct;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "direct_conversations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectConversationEntity {
    @Id
    private UUID id;

    private UUID requesterId;

    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    private DirectConversationStatus status;

    private Instant createdAt;

    private Instant updatedAt;

    private UUID blockedByUserId;
}
