package com.intensify.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "analysis_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ToString.Exclude
    private PracticeSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_type", nullable = false, length = 20)
    private AnalysisType analysisType;

    @Column(name = "overall_score")
    private Integer overallScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_scores", columnDefinition = "json")
    private List<CategoryScore> categoryScores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strengths", columnDefinition = "json")
    private List<FeedbackPoint> strengths;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weaknesses", columnDefinition = "json")
    private List<FeedbackPoint> weaknesses;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_concepts", columnDefinition = "json")
    private List<String> missingConcepts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "incorrect_claims", columnDefinition = "json")
    private List<String> incorrectClaims;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendations", columnDefinition = "json")
    private List<String> recommendations;

    /**
     * Raw AI response retained for debugging/audit — NOT the source of truth for display.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_ai_response", columnDefinition = "json")
    private Object rawAiResponse;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum AnalysisType {
        MAIN, FOLLOWUP
    }

    // ── Embedded value objects ────────────────────────────────────────────────

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CategoryScore {
        private String dimension;
        private Integer score;       // null when not applicable
        private boolean applicable;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FeedbackPoint {
        private String point;
        private String evidence;
        private boolean verified;    // true = passed evidence verification
    }
}
