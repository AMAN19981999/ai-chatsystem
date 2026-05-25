package com.aichat.direct;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auto_reply_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoReplySettingsEntity {
    @Id
    private UUID userId;

    private boolean enabled;

    private int delayMinutes;

    private String instructions;

    private Instant updatedAt;
}
