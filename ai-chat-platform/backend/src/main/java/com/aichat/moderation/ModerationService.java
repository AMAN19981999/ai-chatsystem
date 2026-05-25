package com.aichat.moderation;

import org.springframework.stereotype.Service;

@Service
public class ModerationService {

    public boolean isAllowed(String content) {
        return content != null && !content.isBlank();
    }
}
