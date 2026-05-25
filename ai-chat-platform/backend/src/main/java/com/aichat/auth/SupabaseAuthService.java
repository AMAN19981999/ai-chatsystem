package com.aichat.auth;

import com.aichat.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupabaseAuthService {

    private final AppProperties appProperties;

    public SupabaseAuthResponse signInWithPassword(String email, String password) {
        String apiKey = appProperties.supabase().anonKey();

        return RestClient.create(appProperties.supabase().url())
                .post()
                .uri("/auth/v1/token?grant_type=password")
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .body(Map.of(
                        "email", email,
                        "password", password
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                    throw new SupabaseAuthException("Supabase rejected the login credentials.");
                })
                .body(SupabaseAuthResponse.class);
    }

    public SupabaseSignupResponse createUser(String username, String email, String password) {
        String serviceRoleKey = appProperties.supabase().serviceRoleKey();

        return RestClient.create(appProperties.supabase().url())
                .post()
                .uri("/auth/v1/admin/users")
                .header("apikey", serviceRoleKey)
                .header("Authorization", "Bearer " + serviceRoleKey)
                .body(Map.of(
                        "email", email,
                        "password", password,
                        "email_confirm", true,
                        "user_metadata", Map.of(
                                "full_name", username,
                                "username", username
                        )
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (httpRequest, response) -> {
                    throw new AuthConflictException("Unable to create Supabase user. Email may already exist.");
                })
                .body(SupabaseSignupResponse.class);
    }
}
