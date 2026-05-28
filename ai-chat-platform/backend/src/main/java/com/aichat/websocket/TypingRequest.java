package com.aichat.websocket;

public record TypingRequest(
        String conversationId,
        boolean typing,
        String token
) {
}
