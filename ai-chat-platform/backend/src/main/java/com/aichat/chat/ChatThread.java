package com.aichat.chat;

import lombok.Builder;

import java.time.Instant;

@Builder
public record ChatThread(
        String id,
        String ownerId,
        String title,
        Instant updatedAt
) {
}
