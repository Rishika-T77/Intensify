package com.intensify.dto;

import com.intensify.entity.AnalysisResult;
import com.intensify.entity.PracticeSession;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public class SessionDtos {

    // ── Requests ─────────────────────────────────────────────────────────────

    public record CreateSessionRequest(
            @NotNull(message = "questionId is required") Long questionId
    ) {}

    public record SubmitResponseRequest(
            @NotBlank(message = "Explanation text is required")
            @Size(max = 2000, message = "Explanation must not exceed 2000 characters")
            String explanationText,

            @Size(max = 5000, message = "Code must not exceed 5000 characters")
            String code,

            @Size(max = 30) String language
    ) {}

    public record SubmitFollowUpRequest(
            @NotBlank(message = "Answer text is required")
            @Size(max = 2000, message = "Answer must not exceed 2000 characters")
            String answerText
    ) {}

    // ── Responses ─────────────────────────────────────────────────────────────

    public record SessionSummaryResponse(
            Long id,
            Long questionId,
            String questionTitle,
            String category,
            String status,
            String failureReason,
            LocalDateTime startedAt,
            LocalDateTime completedAt
    ) {
        public static SessionSummaryResponse from(PracticeSession s) {
            return new SessionSummaryResponse(
                    s.getId(),
                    s.getQuestion().getId(),
                    s.getQuestion().getTitle(),
                    s.getQuestion().getCategory(),
                    s.getStatus().name(),
                    s.getFailureReason(),
                    s.getStartedAt(),
                    s.getCompletedAt()
            );
        }
    }

    public record AnalysisResponse(
            Long sessionId,
            String analysisType,
            Integer overallScore,
            List<AnalysisResult.CategoryScore> categoryScores,
            List<AnalysisResult.FeedbackPoint> strengths,
            List<AnalysisResult.FeedbackPoint> weaknesses,
            List<String> missingConcepts,
            List<String> incorrectClaims,
            List<String> recommendations,
            String followUpQuestion
    ) {}

    public record FollowUpQuestionResponse(
            Long sessionId,
            String questionText
    ) {}
}
