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
 * Concrete Cloudflare Workers AI implementation of AIProvider.
 * Runs Llama 3 / Mistral via Cloudflare's global edge network (10,000 free requests/day).
 */
@Component("cloudflareProvider")
@Slf4j
public class CloudflareAIProvider implements AIProvider {

    @Value("${app.ai.cloudflare.account-id:${CLOUDFLARE_ACCOUNT_ID:}}")
    private String accountId;

    @Value("${app.ai.cloudflare.api-token:${CLOUDFLARE_API_TOKEN:}}")
    private String apiToken;

    @Value("${app.ai.cloudflare.model:@cf/meta/llama-3-8b-instruct}")
    private String model;

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public CloudflareAIProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(15000);
        requestFactory.setReadTimeout(20000);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public EvaluationResult evaluate(String systemPrompt, String userContent) {
        if (accountId == null || accountId.isBlank() || apiToken == null || apiToken.isBlank()) {
            throw new AIProviderException("Cloudflare AI configuration missing. Set CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_API_TOKEN.");
        }

        String url = "https://api.cloudflare.com/client/v4/accounts/" + accountId + "/ai/run/" + model;

        Map<String, Object> requestBody = Map.of(
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContent)
                ),
                "temperature", 0.2
        );

        try {
            log.info("Calling Cloudflare Workers AI model: {}", model);
            String responseBody = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new AIProviderException("Cloudflare AI returned an empty response.");
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            boolean success = rootNode.path("success").asBoolean(false);
            if (!success) {
                String errorMsg = rootNode.path("errors").path(0).path("message").asText("Unknown Cloudflare error");
                throw new AIProviderException("Cloudflare AI returned error: " + errorMsg);
            }

            JsonNode responseNode = rootNode.path("result").path("response");
            if (responseNode.isMissingNode() || responseNode.asText().isBlank()) {
                throw new AIProviderException("Could not extract response text from Cloudflare AI output.");
            }

            String jsonText = extractJson(responseNode.asText());
            return objectMapper.readValue(jsonText, EvaluationResult.class);

        } catch (Exception e) {
            log.error("Cloudflare AI provider call failed: {}", e.getMessage());
            throw new AIProviderException("Cloudflare AI call failed: " + e.getMessage(), e);
        }
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
