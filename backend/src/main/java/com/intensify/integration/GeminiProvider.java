package com.intensify.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Concrete Google Gemini LLM implementation of AIProvider.
 * Uses Gemini REST API (gemini-2.0-flash) with structured JSON response_mime_type.
 */
@Component("geminiProvider")
@Slf4j
public class GeminiProvider implements AIProvider {

    @Value("${app.ai.gemini.api-key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${app.ai.gemini.model}")
    private String model;

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(25000);
        requestFactory.setReadTimeout(25000);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public EvaluationResult evaluate(String systemPrompt, String userContent) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AIProviderException("Gemini API key is missing. Please set GEMINI_API_KEY in your .env file.");
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", userContent))
                        )
                ),
                "generationConfig", Map.of(
                        "response_mime_type", "application/json",
                        "temperature", 0.2
                )
        );

        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String responseBody = restClient.post()
                        .uri(url)
                        .header("Content-Type", "application/json")
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                if (responseBody == null || responseBody.isBlank()) {
                    throw new AIProviderException("Gemini API returned an empty response.");
                }

                JsonNode rootNode = objectMapper.readTree(responseBody);
                JsonNode textNode = rootNode.path("candidates")
                        .path(0)
                        .path("content")
                        .path("parts")
                        .path(0)
                        .path("text");

                if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                    throw new AIProviderException("Could not extract response text from Gemini API output.");
                }

                String jsonText = extractJson(textNode.asText());
                return objectMapper.readValue(jsonText, EvaluationResult.class);

            } catch (Exception e) {
                boolean is503 = e.getMessage() != null && e.getMessage().contains("503");
                if (is503 && attempt < maxAttempts) {
                    log.warn("Gemini 503 high demand spike on attempt {}/{}. Retrying in 2 seconds...", attempt, maxAttempts);
                    try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    continue;
                }
                log.error("Gemini provider call failed (attempt {}/{}): {}", attempt, maxAttempts, e.getMessage());
                throw new AIProviderException("Gemini provider call failed: " + e.getMessage(), e);
            }
        }
        throw new AIProviderException("Gemini provider failed after max attempts.");
    }

    private String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                trimmed = trimmed.substring(start, end).trim();
            }
        }
        return trimmed;
    }
}
