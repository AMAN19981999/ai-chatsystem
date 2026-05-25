package com.aichat.message;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, String> {
    List<MessageEntity> findByChatIdOrderByCreatedAtAsc(String chatId);
}
