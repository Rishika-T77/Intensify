package com.intensify.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "practice_sessions", indexes = {
        @Index(name = "idx_sessions_user_started", columnList = "user_id, started_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PracticeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private InterviewQuestion question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SessionStatus status = SessionStatus.STARTED;

    /**
     * Populated on FAILED; null otherwise.
     * Values: TIMEOUT | VALIDATION_FAILED | PROVIDER_ERROR
     */
    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "started_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CandidateResponse> responses = new ArrayList<>();

    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private CodeSubmission codeSubmission;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AnalysisResult> analysisResults = new ArrayList<>();

    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private FollowUpQuestion followUpQuestion;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SkillMetric> skillMetrics = new ArrayList<>();

    public enum SessionStatus {
        STARTED,
        RESPONSE_SUBMITTED,
        ANALYZING,
        ANALYZED,
        FOLLOWUP_PENDING,
        FOLLOWUP_ANSWERED,
        COMPLETED,
        FAILED,
        ABANDONED
    }
}
