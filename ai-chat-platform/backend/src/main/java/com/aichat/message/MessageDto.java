package com.aichat.message;

import java.time.Instant;

public record MessageDto(
        String id,
        String chatId,
        MessageRole role,
        String content,
        Instant createdAt
) {
    public static MessageDto fromEntity(MessageEntity entity) {
        return new MessageDto(
                entity.getId(),
                entity.getChatId(),
                entity.getRole(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }
}
