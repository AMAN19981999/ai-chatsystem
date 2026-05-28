package com.aichat.direct;

import java.time.Instant;

public record DirectConversationDto(
        String id,
        DirectConversationStatus status,
        String otherUserId,
        String otherUsername,
        String otherDisplayName,
        boolean incomingRequest,
        boolean otherUserOnline,
        Instant updatedAt
) {
}
