package com.aichat.ai;

import com.aichat.message.MessageDto;
import com.aichat.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiReplyService {

    private final GroqService groqService;
    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageDto createReply(AiReplyRequest request) {
        String replyText = groqService.generateReply(request.content());
        MessageDto reply = messageService.createAssistantMessage(request.chatId(), replyText);
        messagingTemplate.convertAndSend("/topic/chats/" + request.chatId(), reply);
        return reply;
    }
}
