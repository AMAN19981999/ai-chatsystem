package com.aichat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cors cors,
        Jwt jwt,
        Supabase supabase,
        OpenAi openai
) {
    public record Cors(String allowedOrigin) {
    }

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Supabase(String url, String anonKey, String serviceRoleKey) {
    }

    public record OpenAi(String apiKey, String model) {
    }
}
