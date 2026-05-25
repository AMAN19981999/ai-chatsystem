package com.aichat.websocket;

import com.aichat.auth.JwtService;
import com.aichat.direct.DirectMessageDto;
import com.aichat.direct.DirectMessageService;
import com.aichat.direct.SendDirectMessageRequest;
import com.aichat.message.MessageDto;
import com.aichat.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final MessageService messageService;
    private final DirectMessageService directMessageService;
    private final JwtService jwtService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void send(ChatSocketMessage request) {
        MessageDto message = messageService.createUserMessage(request.chatId(), request.content());
        messagingTemplate.convertAndSend("/topic/chats/" + request.chatId(), message);
    }

    @MessageMapping("/direct.send")
    public void sendDirect(DirectSocketMessage request) {
        UUID supabaseAuthUserId = UUID.fromString(jwtService.validateAndGetSubject(request.token()));
        UUID conversationId = UUID.fromString(request.conversationId());
        DirectMessageDto message = directMessageService.sendMessage(
                conversationId,
                new SendDirectMessageRequest(request.content()),
                supabaseAuthUserId
        );
        messagingTemplate.convertAndSend("/topic/direct/" + conversationId, message);
    }
}
