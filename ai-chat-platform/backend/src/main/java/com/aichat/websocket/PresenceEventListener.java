package com.aichat.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PresenceEventListener {

    private final PresenceManager presenceManager;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        log.info("WebSocket session disconnected: {}", sessionId);

        String username = presenceManager.removeSession(sessionId);
        if (username != null) {
            log.info("User {} went completely offline.", username);
            // Broadcast offline status to all clients
            messagingTemplate.convertAndSend("/topic/presence", Map.of(
                    "username", username,
                    "online", false
            ));
        }
    }
}
