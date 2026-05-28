package com.aichat.websocket;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceManager {

    // Maps sessionId -> username
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    // Maps username -> active session count
    private final Map<String, Integer> userSessionCountMap = new ConcurrentHashMap<>();

    /**
     * Registers a new WebSocket session.
     * @return true if the user transitioned from offline to online.
     */
    public boolean registerSession(String sessionId, String username) {
        if (sessionId == null || username == null) {
            return false;
        }

        sessionUserMap.put(sessionId, username);
        
        // Atomically increment the session count for the user
        final boolean[] transitioned = {false};
        userSessionCountMap.compute(username, (key, count) -> {
            if (count == null || count <= 0) {
                transitioned[0] = true;
                return 1;
            }
            return count + 1;
        });

        return transitioned[0];
    }

    /**
     * Removes a WebSocket session by sessionId.
     * @return the username if the user is now completely offline, otherwise null.
     */
    public String removeSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }

        String username = sessionUserMap.remove(sessionId);
        if (username == null) {
            return null;
        }

        final boolean[] completelyOffline = {false};
        userSessionCountMap.compute(username, (key, count) -> {
            if (count == null || count <= 1) {
                completelyOffline[0] = true;
                return null; // removes from map
            }
            return count - 1;
        });

        return completelyOffline[0] ? username : null;
    }

    /**
     * Checks if a user is currently online.
     */
    public boolean isUserOnline(String username) {
        if (username == null) {
            return false;
        }
        Integer count = userSessionCountMap.get(username);
        return count != null && count > 0;
    }
}
