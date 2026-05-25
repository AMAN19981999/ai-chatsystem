package com.aichat.direct;

import com.aichat.auth.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/direct")
@RequiredArgsConstructor
public class DirectMessageController {

    private final DirectMessageService directMessageService;
    private final DirectAutoReplyService autoReplyService;
    private final JwtService jwtService;

    @GetMapping("/conversations")
    public List<DirectConversationDto> listConversations() {
        return directMessageService.listConversations();
    }

    @PostMapping("/requests")
    public DirectConversationDto createRequest(@Valid @RequestBody CreateDirectRequest request) {
        return directMessageService.createRequest(request);
    }

    @PostMapping("/conversations/{conversationId}/accept")
    public DirectConversationDto acceptRequest(@PathVariable UUID conversationId) {
        return directMessageService.acceptRequest(conversationId);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<DirectMessageDto> listMessages(@PathVariable UUID conversationId) {
        return directMessageService.listMessages(conversationId);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public DirectMessageDto sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendDirectMessageRequest request
    ) {
        return directMessageService.sendMessage(conversationId, request);
    }

    @GetMapping("/auto-reply/settings")
    public AutoReplySettingsDto getAutoReplySettings(@RequestHeader("Authorization") String authorization) {
        return autoReplyService.getSettings(resolveSupabaseAuthUserId(authorization));
    }

    @PutMapping("/auto-reply/settings")
    public AutoReplySettingsDto updateAutoReplySettings(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody UpdateAutoReplySettingsRequest request
    ) {
        return autoReplyService.updateSettings(resolveSupabaseAuthUserId(authorization), request);
    }

    private UUID resolveSupabaseAuthUserId(String authorization) {
        String token = authorization.replace("Bearer ", "");
        return UUID.fromString(jwtService.validateAndGetSubject(token));
    }
}
