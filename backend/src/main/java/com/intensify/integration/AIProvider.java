package com.intensify.integration;

/**
 * AIProvider interface — all LLM implementations go behind this.
 * Swapping or adding providers must not require changes to AIAnalysisService.
 */
public interface AIProvider {

    /**
     * Calls the LLM and returns a structured EvaluationResult.
     *
     * @param systemPrompt The system prompt containing rubric and instructions.
     * @param userContent  The candidate's response content (delimited to mitigate prompt injection).
     * @return             Parsed and validated EvaluationResult.
     * @throws AIProviderException if the call fails or returns unretriable error.
     */
    EvaluationResult evaluate(String systemPrompt, String userContent);
}
