package com.aichat.direct;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AutoReplySettingsRepository extends JpaRepository<AutoReplySettingsEntity, UUID> {
}
