package com.aichat.websocket;

import jakarta.validation.constraints.NotBlank;

public record ChatSocketMessage(
        @NotBlank String chatId,
        @NotBlank String content
) {
}
