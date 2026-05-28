package com.aichat.ai;

import com.aichat.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import org.springframework.util.StreamUtils;

@Service
@RequiredArgsConstructor
public class GroqService {

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public String generateReply(String prompt) {
        return callGroq(List.of(Map.of("role", "user", "content", prompt)), GroqResponse.class, false).choices().get(0).message().content().trim();
    }

    public SpamAnalysis analyzeMessage(String content) {
        String prompt = """
                Analyze the following chat message for spam, scams, or offensive commercial content (like loan offers, gambling, or phishing).
                Respond with a JSON object. The JSON should be in this format: { "isSpam": boolean, "reason": "short explanation" }.
                
                Content:
                "%s"
                """.formatted(content);

        try {
            GroqResponse response = callGroq(List.of(Map.of("role", "user", "content", prompt)), GroqResponse.class, true);
            String jsonContent = response.choices().get(0).message().content().trim();
            return objectMapper.readValue(jsonContent, SpamAnalysis.class);
        } catch (Exception e) {
            System.err.println("Failed to analyze spam: " + e.getMessage());
            return new SpamAnalysis(false, "Failed to analyze spam: " + e.getMessage());
        }
    }

    private <T> T callGroq(List<Map<String, String>> messages, Class<T> responseType, boolean useJson) {
        Map<String, Object> body = new java.util.HashMap<>(Map.of(
                "model", appProperties.groq().model(),
                "messages", messages
        ));
        if (useJson) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        return RestClient.create("https://api.groq.com/openai")
                .post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + appProperties.groq().apiKey())
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, errorResponse) -> {
                    String responseBody = StreamUtils.copyToString(errorResponse.getBody(), StandardCharsets.UTF_8);
                    throw new IllegalStateException("Groq API call failed with status: " + errorResponse.getStatusCode() + " and body: " + responseBody);
                })
                .body(responseType);
    }

    public record SpamAnalysis(boolean isSpam, String reason) {
    }

    private record GroqResponse(List<GroqChoice> choices) {
    }

    private record GroqChoice(GroqMessage message) {
    }

    private record GroqMessage(String role, String content) {
    }
}
