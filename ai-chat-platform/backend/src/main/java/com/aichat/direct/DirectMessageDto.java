package com.aichat.direct;

import java.time.Instant;

public record DirectMessageDto(
        String id,
        String conversationId,
        String senderId,
        String senderUsername,
        String content,
        boolean aiGenerated,
        Instant createdAt
) {
}
