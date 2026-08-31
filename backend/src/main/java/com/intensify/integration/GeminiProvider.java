package com.intensify.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Concrete Google Gemini LLM implementation of AIProvider.
 * Uses Gemini REST API with automatic model fallback for 503 high-demand spikes and version deprecation.
 */
@Component("geminiProvider")
@Slf4j
public class GeminiProvider implements AIProvider {

    @Value("${app.ai.gemini.api-key:${GEMINI_API_KEY:}}")
    private String apiKey;

    @Value("${app.ai.gemini.model}")
    private String configuredModel;

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(8000);
        requestFactory.setReadTimeout(15000);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public EvaluationResult evaluate(String systemPrompt, String userContent) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AIProviderException("Gemini API key is missing. Please set GEMINI_API_KEY in environment variables.");
        }

        // Ordered list of candidate models to try on 503/404 errors
        Set<String> modelsToTry = new LinkedHashSet<>();
        if (configuredModel != null && !configuredModel.isBlank()) {
            modelsToTry.add(configuredModel);
        }
        modelsToTry.add("gemini-3.6-flash");
        modelsToTry.add("gemini-flash-latest");
        modelsToTry.add("gemini-1.5-flash-latest");

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

        Exception lastException = null;

        for (String targetModel : modelsToTry) {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + targetModel + ":generateContent?key=" + apiKey;
            log.info("Attempting Gemini API evaluation with model: {}", targetModel);

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
                EvaluationResult result = objectMapper.readValue(jsonText, EvaluationResult.class);
                log.info("Gemini evaluation succeeded with model: {}", targetModel);
                return result;

            } catch (Exception e) {
                log.warn("Gemini evaluation failed with model {} ({}). Trying next candidate...", targetModel, e.getMessage());
                lastException = e;
            }
        }

        log.error("All Gemini model candidates failed.");
        throw new AIProviderException("Gemini provider failed across all candidate models: " +
                (lastException != null ? lastException.getMessage() : "Unknown error"), lastException);
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
