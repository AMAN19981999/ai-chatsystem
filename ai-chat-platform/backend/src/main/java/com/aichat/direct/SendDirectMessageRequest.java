package com.aichat.direct;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendDirectMessageRequest(
        @NotBlank @Size(max = 4000) String content
) {
}
