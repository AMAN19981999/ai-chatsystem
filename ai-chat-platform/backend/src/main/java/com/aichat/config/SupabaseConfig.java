package com.aichat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SupabaseConfig {

    private final AppProperties appProperties;

    public String projectUrl() {
        return appProperties.supabase().url();
    }

    public String serviceRoleKey() {
        return appProperties.supabase().serviceRoleKey();
    }

    public String anonKey() {
        return appProperties.supabase().anonKey();
    }
}
