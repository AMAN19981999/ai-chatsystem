package com.aichat.common;

import java.time.Instant;

public record ApiError(
        String message,
        Instant timestamp
) {
}
