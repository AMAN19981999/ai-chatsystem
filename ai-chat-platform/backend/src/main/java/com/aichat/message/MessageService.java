package com.aichat.message;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    public List<MessageDto> listMessages(String chatId) {
        // This starter returns a seed message so the UI works before migrations are installed.
        return List.of(new MessageDto(
                UUID.randomUUID().toString(),
                chatId,
                MessageRole.ASSISTANT,
                "Welcome. Ask me anything and I will route the reply through the AI service boundary.",
                Instant.now()
        ));
    }

    public MessageDto createUserMessage(String chatId, String content) {
        return new MessageDto(UUID.randomUUID().toString(), chatId, MessageRole.USER, content, Instant.now());
    }

    public MessageDto createAssistantMessage(String chatId, String content) {
        return new MessageDto(UUID.randomUUID().toString(), chatId, MessageRole.ASSISTANT, content, Instant.now());
    }
}
