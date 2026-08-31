package com.intensify.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Intelligent Multi-Provider Router with automatic failover between 100% free providers.
 * Priority Chain: Gemini -> Cloudflare Workers AI -> OpenAI (optional if key provided).
 */
@Component
@Primary
@Slf4j
public class AIProviderRouter implements AIProvider {

    private final GeminiProvider geminiProvider;
    private final CloudflareAIProvider cloudflareAIProvider;
    private final OpenAIProvider openAIProvider;

    @Value("${app.ai.provider-chain:gemini,cloudflare,openai}")
    private String providerChainConfig;

    public AIProviderRouter(GeminiProvider geminiProvider,
                            CloudflareAIProvider cloudflareAIProvider,
                            OpenAIProvider openAIProvider) {
        this.geminiProvider = geminiProvider;
        this.cloudflareAIProvider = cloudflareAIProvider;
        this.openAIProvider = openAIProvider;
    }

    @Override
    public EvaluationResult evaluate(String systemPrompt, String userContent) {
        List<String> chain = getProviderChain();
        List<String> errors = new ArrayList<>();

        for (String providerName : chain) {
            AIProvider provider = resolveProvider(providerName);
            if (provider == null) {
                continue;
            }

            try {
                log.info("[Router] Attempting evaluation using provider: {}", providerName.toUpperCase());
                EvaluationResult result = provider.evaluate(systemPrompt, userContent);
                log.info("[Router] Evaluation successfully completed by provider: {}", providerName.toUpperCase());
                return result;
            } catch (Exception e) {
                String errorMsg = String.format("%s failed: %s", providerName.toUpperCase(), e.getMessage());
                log.warn("[Router] {} — Failing over to next provider in chain...", errorMsg);
                errors.add(errorMsg);
            }
        }

        log.error("[Router] All AI providers in chain failed: {}", errors);
        throw new AIProviderException("All AI providers in chain failed. Errors: " + String.join(" | ", errors));
    }

    private List<String> getProviderChain() {
        if (providerChainConfig == null || providerChainConfig.isBlank()) {
            return List.of("gemini", "cloudflare");
        }
        return List.of(providerChainConfig.toLowerCase().split(","));
    }

    private AIProvider resolveProvider(String name) {
        return switch (name.trim().toLowerCase()) {
            case "gemini" -> geminiProvider;
            case "cloudflare" -> cloudflareAIProvider;
            case "openai" -> openAIProvider;
            default -> {
                log.warn("[Router] Unknown provider name in chain: {}", name);
                yield null;
            }
        };
    }
}
