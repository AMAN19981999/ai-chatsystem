package com.aichat.ai;

import com.aichat.message.MessageDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiReplyService aiReplyService;

    @PostMapping("/reply")
    public MessageDto reply(@Valid @RequestBody AiReplyRequest request) {
        return aiReplyService.createReply(request);
    }
}
