package com.aichat.auth;

public record AuthUserResponse(
        String id,
        String email,
        String displayName
) {
}
