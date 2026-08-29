package com.intensify.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

/**
 * Concrete OpenAI implementation of AIProvider using Spring AI's ChatClient.
 * Prompt injection mitigation: candidate content is wrapped in XML-style delimiters.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAIProvider implements AIProvider {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public EvaluationResult evaluate(String systemPrompt, String userContent) {
        try {
            ChatClient client = chatClientBuilder.build();

            String response = client.prompt()
                    .system(systemPrompt)
                    .user(userContent)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                throw new AIProviderException("AI returned an empty response.");
            }

            // Extract JSON from the response (model may wrap it in markdown code blocks)
            String json = extractJson(response);
            return objectMapper.readValue(json, EvaluationResult.class);

        } catch (AIProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("OpenAI provider call failed: {}", e.getMessage());
            throw new AIProviderException("AI provider call failed: " + e.getMessage(), e);
        }
    }

    private String extractJson(String response) {
        // Strip markdown code fences if present
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
