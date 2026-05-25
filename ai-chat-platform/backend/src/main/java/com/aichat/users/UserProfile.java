package com.aichat.users;

import lombok.Builder;

@Builder
public record UserProfile(
        String id,
        String email,
        String displayName
) {
}
