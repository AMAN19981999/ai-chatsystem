package com.aichat.direct;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateAutoReplySettingsRequest(
        boolean enabled,
        @Min(1) @Max(1440) int delayMinutes,
        @Size(max = 1000) String instructions
) {
}
