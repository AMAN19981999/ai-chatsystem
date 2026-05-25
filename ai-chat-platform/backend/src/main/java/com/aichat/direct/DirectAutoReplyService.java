package com.aichat.direct;

import com.aichat.ai.OpenAiService;
import com.aichat.users.AppUserEntity;
import com.aichat.users.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DirectAutoReplyService {

    private final AutoReplySettingsRepository settingsRepository;
    private final AppUserRepository appUserRepository;
    private final DirectConversationRepository conversationRepository;
    private final DirectMessageRepository messageRepository;
    private final OpenAiService openAiService;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;

    public AutoReplySettingsDto getSettings(UUID supabaseAuthUserId) {
        AppUserEntity user = currentUser(supabaseAuthUserId);
        return settingsRepository.findById(user.getId())
                .map(this::toDto)
                .orElse(new AutoReplySettingsDto(false, 5, ""));
    }

    public AutoReplySettingsDto updateSettings(
            UUID supabaseAuthUserId,
            UpdateAutoReplySettingsRequest request
    ) {
        AppUserEntity user = currentUser(supabaseAuthUserId);
        AutoReplySettingsEntity settings = settingsRepository.findById(user.getId())
                .orElseGet(() -> AutoReplySettingsEntity.builder()
                        .userId(user.getId())
                        .build());

        settings.setEnabled(request.enabled());
        settings.setDelayMinutes(request.delayMinutes());
        settings.setInstructions(request.instructions() == null ? "" : request.instructions().trim());
        settings.setUpdatedAt(Instant.now());

        return toDto(settingsRepository.save(settings));
    }

    public void scheduleIfNeeded(DirectConversationEntity conversation, DirectMessageEntity incomingMessage) {
        UUID recipientId = conversation.getRequesterId().equals(incomingMessage.getSenderId())
                ? conversation.getRecipientId()
                : conversation.getRequesterId();

        settingsRepository.findById(recipientId)
                .filter(AutoReplySettingsEntity::isEnabled)
                .ifPresent(settings -> taskScheduler.schedule(
                        () -> sendAutoReplyIfStillUnanswered(conversation.getId(), incomingMessage.getId(), recipientId),
                        Instant.now().plus(Duration.ofMinutes(settings.getDelayMinutes()))
                ));
    }

    private void sendAutoReplyIfStillUnanswered(UUID conversationId, UUID incomingMessageId, UUID recipientId) {
        AutoReplySettingsEntity settings = settingsRepository.findById(recipientId)
                .filter(AutoReplySettingsEntity::isEnabled)
                .orElse(null);
        if (settings == null) {
            return;
        }

        DirectMessageEntity incomingMessage = messageRepository.findById(incomingMessageId).orElse(null);
        if (incomingMessage == null) {
            return;
        }

        boolean alreadyReplied = messageRepository.existsByConversationIdAndSenderIdAndCreatedAtAfter(
                conversationId,
                recipientId,
                incomingMessage.getCreatedAt()
        );
        if (alreadyReplied) {
            return;
        }

        AppUserEntity recipient = appUserRepository.findById(recipientId).orElse(null);
        if (recipient == null) {
            return;
        }

        List<DirectMessageEntity> recentMessages = messageRepository
                .findTop10ByConversationIdOrderByCreatedAtDesc(conversationId)
                .stream()
                .sorted(Comparator.comparing(DirectMessageEntity::getCreatedAt))
                .toList();
        Map<UUID, AppUserEntity> usersById = appUserRepository.findAllById(
                recentMessages.stream()
                        .map(DirectMessageEntity::getSenderId)
                        .collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(AppUserEntity::getId, Function.identity()));
        String conversationText = recentMessages.stream()
                .map(message -> "%s%s: %s".formatted(
                        usersById.get(message.getSenderId()).getUsername(),
                        message.isAiGenerated() ? " (AI)" : "",
                        message.getContent()
                ))
                .collect(Collectors.joining("\n"));

        String prompt = """
                You are writing exactly one chat message on behalf of %s (@%s).
                Read the recent conversation below. Reply to the most recent human message.
                Keep the reply natural, short, and in the same language as the latest message when possible.
                Do not mention that you are AI. Do not explain. Output only the message text.
                
                Recent conversation:
                %s
                
                User instructions:
                %s
                """.formatted(
                recipient.getDisplayName(),
                recipient.getUsername(),
                conversationText,
                settings.getInstructions() == null || settings.getInstructions().isBlank()
                        ? "Reply politely and naturally."
                        : settings.getInstructions()
        );

        Instant now = Instant.now();
        DirectMessageEntity reply = messageRepository.save(DirectMessageEntity.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .senderId(recipientId)
                .content(openAiService.generateReply(prompt))
                .aiGenerated(true)
                .createdAt(now)
                .build());

        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            conversation.setUpdatedAt(now);
            conversationRepository.save(conversation);
        });

        messagingTemplate.convertAndSend("/topic/direct/" + conversationId, toMessageDto(reply, recipient));
    }

    private AppUserEntity currentUser(UUID supabaseAuthUserId) {
        return appUserRepository.findBySupabaseAuthUserId(supabaseAuthUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user profile not found."));
    }

    private AutoReplySettingsDto toDto(AutoReplySettingsEntity settings) {
        return new AutoReplySettingsDto(
                settings.isEnabled(),
                settings.getDelayMinutes(),
                settings.getInstructions() == null ? "" : settings.getInstructions()
        );
    }

    private DirectMessageDto toMessageDto(DirectMessageEntity message, AppUserEntity sender) {
        return new DirectMessageDto(
                message.getId().toString(),
                message.getConversationId().toString(),
                message.getSenderId().toString(),
                sender.getUsername(),
                message.getContent(),
                message.isAiGenerated(),
                message.getCreatedAt()
        );
    }
}
