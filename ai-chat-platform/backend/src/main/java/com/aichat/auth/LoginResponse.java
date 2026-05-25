package com.aichat.auth;

public record LoginResponse(
        String token,
        AuthUserResponse user
) {
}
