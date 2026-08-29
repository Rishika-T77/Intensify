package com.intensify.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "skill_metrics", indexes = {
        @Index(name = "idx_skill_metrics_user_category_dim", columnList = "user_id, category, dimension, recorded_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ToString.Exclude
    private PracticeSession session;

    /** e.g. "APPROACH_FORMULATION", "COMPLEXITY_ANALYSIS" */
    @Column(nullable = false, length = 50)
    private String dimension;

    /**
     * Denormalized from session.question.category.
     * Enables category-filtered trend queries without a join.
     */
    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    private Integer score;

    @CreationTimestamp
    @Column(name = "recorded_at", updatable = false)
    private LocalDateTime recordedAt;
}
