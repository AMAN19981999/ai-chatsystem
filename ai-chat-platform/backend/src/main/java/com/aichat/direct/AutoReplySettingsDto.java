package com.aichat.direct;

public record AutoReplySettingsDto(
        boolean enabled,
        int delayMinutes,
        String instructions
) {
}
