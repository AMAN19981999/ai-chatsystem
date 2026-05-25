package com.aichat.auth;

import com.aichat.users.AppUserEntity;
import com.aichat.users.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final SupabaseAuthService supabaseAuthService;
    private final AppUserRepository appUserRepository;

    public LoginResponse login(LoginRequest request) {
        String email = resolveEmail(request.identifier());
        SupabaseAuthResponse supabaseSession = supabaseAuthService.signInWithPassword(email, request.password());
        SupabaseAuthResponse.SupabaseUser supabaseUser = supabaseSession.user();
        AppUserEntity appUser = upsertAppUser(supabaseUser);

        String token = jwtService.generateToken(supabaseUser.id(), supabaseUser.email());
        AuthUserResponse user = new AuthUserResponse(
                supabaseUser.id(),
                supabaseUser.email(),
                appUser.getDisplayName()
        );
        return new LoginResponse(token, user);
    }

    @Transactional
    public LoginResponse signup(SignupRequest request) {
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());

        if (appUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new AuthConflictException("Username is already taken.");
        }

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new AuthConflictException("Email is already registered.");
        }

        SupabaseSignupResponse supabaseUser = supabaseAuthService.createUser(username, email, request.password());
        Instant now = Instant.now();
        AppUserEntity appUser = AppUserEntity.builder()
                .id(UUID.randomUUID())
                .supabaseAuthUserId(UUID.fromString(supabaseUser.id()))
                .username(username)
                .email(email)
                .displayName(request.username())
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            appUserRepository.saveAndFlush(appUser);
        } catch (DataIntegrityViolationException exception) {
            throw new AuthConflictException("Username or email is already registered.");
        }

        return login(new LoginRequest(email, request.password()));
    }

    private String resolveEmail(String identifier) {
        String normalized = normalizeEmail(identifier);

        if (normalized.contains("@")) {
            return normalized;
        }

        return appUserRepository.findByUsernameIgnoreCase(normalized)
                .map(AppUserEntity::getEmail)
                .orElseThrow(() -> new SupabaseAuthException("Invalid username/email or password."));
    }

    private AppUserEntity upsertAppUser(SupabaseAuthResponse.SupabaseUser user) {
        UUID supabaseUserId = UUID.fromString(user.id());
        return appUserRepository.findBySupabaseAuthUserId(supabaseUserId)
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    String fallbackUsername = user.email().split("@")[0].replaceAll("[^a-zA-Z0-9_]", "_");

                    return appUserRepository.save(AppUserEntity.builder()
                            .id(UUID.randomUUID())
                            .supabaseAuthUserId(supabaseUserId)
                            .username(uniqueUsername(fallbackUsername))
                            .email(user.email().toLowerCase())
                            .displayName(resolveDisplayName(user))
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
                });
    }

    private String uniqueUsername(String baseUsername) {
        String candidate = normalizeUsername(baseUsername);
        int suffix = 1;

        while (appUserRepository.existsByUsernameIgnoreCase(candidate)) {
            candidate = normalizeUsername(baseUsername) + suffix;
            suffix++;
        }

        return candidate;
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveDisplayName(SupabaseAuthResponse.SupabaseUser user) {
        if (user.userMetadata() != null && user.userMetadata().fullName() != null) {
            return user.userMetadata().fullName();
        }

        if (user.userMetadata() != null && user.userMetadata().name() != null) {
            return user.userMetadata().name();
        }

        return user.email();
    }
}
