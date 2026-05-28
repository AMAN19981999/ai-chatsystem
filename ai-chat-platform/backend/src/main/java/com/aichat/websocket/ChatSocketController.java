package com.aichat.websocket;

import com.aichat.auth.JwtService;
import com.aichat.direct.DirectMessageDto;
import com.aichat.direct.DirectMessageService;
import com.aichat.direct.SendDirectMessageRequest;
import com.aichat.message.MessageDto;
import com.aichat.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatSocketController {

    private final MessageService messageService;
    private final DirectMessageService directMessageService;
    private final JwtService jwtService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceManager presenceManager;

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

    @MessageMapping("/presence.register")
    public void registerPresence(PresenceRegisterRequest request, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String username = jwtService.validateAndGetSubject(request.token());
            String sessionId = headerAccessor.getSessionId();
            
            log.info("Registering online session {} for user {}", sessionId, username);
            boolean transitioned = presenceManager.registerSession(sessionId, username);
            
            if (transitioned) {
                log.info("Broadcasting user {} went online", username);
                messagingTemplate.convertAndSend("/topic/presence", Map.of(
                        "username", username,
                        "online", true
                ));
            }
        } catch (Exception e) {
            log.error("Failed to register presence for token: {}", e.getMessage());
        }
    }

    @MessageMapping("/direct.typing")
    public void handleTyping(TypingRequest request) {
        try {
            String username = jwtService.validateAndGetSubject(request.token());
            messagingTemplate.convertAndSend("/topic/direct/" + request.conversationId() + "/typing", Map.of(
                    "username", username,
                    "typing", request.typing()
            ));
        } catch (Exception e) {
            log.error("Failed to process typing indicator: {}", e.getMessage());
        }
    }
}
