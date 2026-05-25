package com.aichat.direct;

import jakarta.validation.constraints.NotBlank;

public record CreateDirectRequest(
        @NotBlank String username
) {
}
