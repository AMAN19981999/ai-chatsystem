package com.aichat.ai;

import com.aichat.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final AppProperties appProperties;

    public String generateReply(String prompt) {
        OpenAiResponse response = RestClient.create("https://api.openai.com")
                .post()
                .uri("/v1/responses")
                .header("Authorization", "Bearer " + appProperties.openai().apiKey())
                .header("Content-Type", "application/json")
                .body(Map.of(
                        "model", appProperties.openai().model(),
                        "input", prompt
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, errorResponse) -> {
                    throw new IllegalStateException("OpenAI reply generation failed.");
                })
                .body(OpenAiResponse.class);

        String text = response == null ? null : response.outputText();
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("OpenAI returned an empty reply.");
        }

        return text.trim();
    }

    private record OpenAiResponse(List<OpenAiOutput> output) {
        String outputText() {
            if (output == null) {
                return "";
            }

            return output.stream()
                    .filter(item -> item.content() != null)
                    .flatMap(item -> item.content().stream())
                    .filter(content -> "output_text".equals(content.type()))
                    .map(OpenAiContent::text)
                    .filter(text -> text != null && !text.isBlank())
                    .findFirst()
                    .orElse("");
        }
    }

    private record OpenAiOutput(List<OpenAiContent> content) {
    }

    private record OpenAiContent(String type, String text) {
    }
}
