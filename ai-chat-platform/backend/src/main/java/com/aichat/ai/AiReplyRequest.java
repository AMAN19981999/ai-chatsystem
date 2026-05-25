package com.aichat.ai;

import jakarta.validation.constraints.NotBlank;

public record AiReplyRequest(
        @NotBlank String chatId,
        @NotBlank String content
) {
}
