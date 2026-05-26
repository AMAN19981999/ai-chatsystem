package com.aichat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cors cors,
        Jwt jwt,
        Supabase supabase,
        Groq groq
) {
    public record Cors(String allowedOrigin) {
    }

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Supabase(String url, String anonKey, String serviceRoleKey) {
    }

    public record Groq(String apiKey, String model) {
    }
}
