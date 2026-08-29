package com.intensify.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "code_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * UNIQUE constraint enforces one submission per session at the DB level.
     * Re-submissions update the existing row (handled in service layer).
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ToString.Exclude
    private PracticeSession session;

    /**
     * Stored as a plain string (not an enum) for easy extension.
     * Example values: java, python, cpp, javascript, csharp, go
     */
    @Column(nullable = false, length = 30)
    private String language;

    @Column(name = "code_text", nullable = false, columnDefinition = "TEXT")
    private String codeText;

    @CreationTimestamp
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;
}
