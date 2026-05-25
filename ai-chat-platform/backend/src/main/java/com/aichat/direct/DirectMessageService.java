package com.aichat.direct;

import com.aichat.users.AppUserEntity;
import com.aichat.users.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DirectMessageService {

    private final AppUserRepository appUserRepository;
    private final DirectConversationRepository conversationRepository;
    private final DirectMessageRepository messageRepository;
    private final DirectAutoReplyService autoReplyService;

    public List<DirectConversationDto> listConversations() {
        AppUserEntity currentUser = currentUser();
        List<DirectConversationEntity> conversations = conversationRepository.findForUser(currentUser.getId());
        Map<UUID, AppUserEntity> usersById = appUserRepository.findAllById(
                conversations.stream()
                        .flatMap(conversation -> List.of(conversation.getRequesterId(), conversation.getRecipientId()).stream())
                        .collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(AppUserEntity::getId, Function.identity()));

        return conversations.stream()
                .map(conversation -> toConversationDto(conversation, currentUser, usersById))
                .toList();
    }

    public DirectConversationDto createRequest(CreateDirectRequest request) {
        AppUserEntity currentUser = currentUser();
        AppUserEntity recipient = appUserRepository.findByUsernameIgnoreCase(request.username().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        if (currentUser.getId().equals(recipient.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot send a request to yourself.");
        }

        DirectConversationEntity conversation = conversationRepository
                .findBetweenUsers(currentUser.getId(), recipient.getId())
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    return conversationRepository.save(DirectConversationEntity.builder()
                            .id(UUID.randomUUID())
                            .requesterId(currentUser.getId())
                            .recipientId(recipient.getId())
                            .status(DirectConversationStatus.PENDING)
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
                });

        return toConversationDto(conversation, currentUser, Map.of(
                currentUser.getId(), currentUser,
                recipient.getId(), recipient
        ));
    }

    public DirectConversationDto acceptRequest(UUID conversationId) {
        AppUserEntity currentUser = currentUser();
        DirectConversationEntity conversation = findConversationForUser(conversationId, currentUser);

        if (!conversation.getRecipientId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the recipient can accept this request.");
        }

        conversation.setStatus(DirectConversationStatus.ACCEPTED);
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        AppUserEntity requester = appUserRepository.findById(conversation.getRequesterId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Requester not found."));

        return toConversationDto(conversation, currentUser, Map.of(
                currentUser.getId(), currentUser,
                requester.getId(), requester
        ));
    }

    public List<DirectMessageDto> listMessages(UUID conversationId) {
        AppUserEntity currentUser = currentUser();
        DirectConversationEntity conversation = findConversationForUser(conversationId, currentUser);
        requireAccepted(conversation);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toMessageDto)
                .toList();
    }

    public DirectMessageDto sendMessage(UUID conversationId, SendDirectMessageRequest request) {
        AppUserEntity currentUser = currentUser();
        return sendMessage(conversationId, request, currentUser);
    }

    public DirectMessageDto sendMessage(
            UUID conversationId,
            SendDirectMessageRequest request,
            UUID supabaseAuthUserId
    ) {
        AppUserEntity currentUser = currentUser(supabaseAuthUserId);
        return sendMessage(conversationId, request, currentUser);
    }

    private DirectMessageDto sendMessage(
            UUID conversationId,
            SendDirectMessageRequest request,
            AppUserEntity currentUser
    ) {
        DirectConversationEntity conversation = findConversationForUser(conversationId, currentUser);
        requireAccepted(conversation);

        Instant now = Instant.now();
        DirectMessageEntity message = messageRepository.save(DirectMessageEntity.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .senderId(currentUser.getId())
                .content(request.content().trim())
                .aiGenerated(false)
                .createdAt(now)
                .build());

        conversation.setUpdatedAt(now);
        conversationRepository.save(conversation);
        if (!message.isAiGenerated()) {
            autoReplyService.scheduleIfNeeded(conversation, message);
        }

        return toMessageDto(message);
    }

    private AppUserEntity currentUser() {
        String subject = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID supabaseAuthUserId = UUID.fromString(subject);
        return appUserRepository.findBySupabaseAuthUserId(supabaseAuthUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user profile not found."));
    }

    private AppUserEntity currentUser(UUID supabaseAuthUserId) {
        return appUserRepository.findBySupabaseAuthUserId(supabaseAuthUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current user profile not found."));
    }

    private DirectConversationEntity findConversationForUser(UUID conversationId, AppUserEntity user) {
        DirectConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found."));

        if (!conversation.getRequesterId().equals(user.getId()) && !conversation.getRecipientId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not part of this conversation.");
        }

        return conversation;
    }

    private void requireAccepted(DirectConversationEntity conversation) {
        if (conversation.getStatus() != DirectConversationStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This message request has not been accepted yet.");
        }
    }

    private DirectConversationDto toConversationDto(
            DirectConversationEntity conversation,
            AppUserEntity currentUser,
            Map<UUID, AppUserEntity> usersById
    ) {
        UUID otherUserId = conversation.getRequesterId().equals(currentUser.getId())
                ? conversation.getRecipientId()
                : conversation.getRequesterId();
        AppUserEntity otherUser = usersById.get(otherUserId);

        return new DirectConversationDto(
                conversation.getId().toString(),
                conversation.getStatus(),
                otherUserId.toString(),
                otherUser.getUsername(),
                otherUser.getDisplayName(),
                conversation.getStatus() == DirectConversationStatus.PENDING
                        && conversation.getRecipientId().equals(currentUser.getId()),
                conversation.getUpdatedAt()
        );
    }

    private DirectMessageDto toMessageDto(DirectMessageEntity message) {
        AppUserEntity sender = appUserRepository.findById(message.getSenderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender not found."));

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
