package com.aichat.queue;

import org.springframework.stereotype.Component;

@Component
public class ChatJobPublisher {

    public void enqueueAiReply(String chatId, String messageId) {
        // Replace with RabbitMQ, Kafka, SQS, or Supabase Edge Function dispatch when needed.
    }
}
