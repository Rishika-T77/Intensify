package com.intensify.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured output contract for AI evaluation calls.
 * Maps directly to the JSON schema defined in PRD §11.5.
 * The backend always RECOMPUTES overallScore from dimension scores — the AI value is a hint only.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EvaluationResult {

    private Integer overallScore;

    private List<CategoryScore> categoryScores;

    private List<FeedbackPoint> strengths;

    private List<FeedbackPoint> weaknesses;

    private List<String> missingConcepts;

    private List<String> incorrectClaims;

    private List<String> recommendations;

    private String followUpQuestion;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CategoryScore {
        private String dimension;
        private Integer score;
        private boolean applicable;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FeedbackPoint {
        private String point;
        private String evidence;
    }
}
