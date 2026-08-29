package com.intensify.repository;

import com.intensify.entity.PracticeSession;
import com.intensify.entity.PracticeSession.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PracticeSessionRepository extends JpaRepository<PracticeSession, Long> {

    Optional<PracticeSession> findByIdAndUserId(Long id, Long userId);

    Page<PracticeSession> findByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);

    Page<PracticeSession> findByUserIdAndStatusOrderByStartedAtDesc(Long userId, SessionStatus status, Pageable pageable);

    /** Daily cap check: how many non-ABANDONED sessions did the user start today? */
    @Query("""
           SELECT COUNT(s) FROM PracticeSession s
           WHERE s.user.id = :userId
             AND s.status <> com.intensify.entity.PracticeSession.SessionStatus.ABANDONED
             AND s.startedAt >= :startOfDay
           """)
    long countSessionsForUserToday(@Param("userId") Long userId, @Param("startOfDay") LocalDateTime startOfDay);

    /** Scheduled cleanup: find sessions stuck in STARTED for over 24 hours. */
    List<PracticeSession> findByStatusAndStartedAtBefore(SessionStatus status, LocalDateTime cutoff);
}
