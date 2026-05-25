package com.aichat.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SupabaseSignupResponse(
        String id,
        String email,
        @JsonProperty("user_metadata") SupabaseAuthResponse.UserMetadata userMetadata
) {
}
