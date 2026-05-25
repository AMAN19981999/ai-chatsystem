package com.aichat.websocket;

import jakarta.validation.constraints.NotBlank;

public record DirectSocketMessage(
        @NotBlank String conversationId,
        @NotBlank String content,
        @NotBlank String token
) {
}
