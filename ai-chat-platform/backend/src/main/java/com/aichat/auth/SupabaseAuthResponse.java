package com.aichat.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabaseAuthResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        SupabaseUser user
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SupabaseUser(
            String id,
            String email,
            @JsonProperty("user_metadata") UserMetadata userMetadata
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserMetadata(
            @JsonProperty("full_name") String fullName,
            String name
    ) {
    }
}
