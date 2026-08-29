package com.intensify.service;

import com.intensify.dto.SessionDtos.*;
import com.intensify.entity.*;
import com.intensify.entity.PracticeSession.SessionStatus;
import com.intensify.exception.AppException;
import com.intensify.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PracticeSessionService {

    private final PracticeSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final AIAnalysisService aiAnalysisService;
    private final PlatformTransactionManager transactionManager;

    @Value("${app.session.daily-cap}")
    private int dailyCap;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public SessionSummaryResponse createSession(Long userId, CreateSessionRequest request) {
        enforceDailyCap(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found."));
        InterviewQuestion question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> AppException.notFound("Question not found: " + request.questionId()));

        PracticeSession session = PracticeSession.builder()
                .user(user)
                .question(question)
                .status(SessionStatus.STARTED)
                .build();

        return SessionSummaryResponse.from(sessionRepository.save(session));
    }

    // ── Submit Response ───────────────────────────────────────────────────────
    // NOTE: Intentionally NOT @Transactional. The pre-AI work runs in a short
    // dedicated transaction that commits before the AI call starts. This prevents
    // holding a DB connection open for the full 30-second AI timeout and ensures
    // ANALYZING status is committed and visible to concurrent polls. (Audit §2.2)

    public SessionSummaryResponse submitResponse(Long userId, Long sessionId, SubmitResponseRequest request) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        // Phase 1: persist candidate input and mark ANALYZING — commits immediately
        Boolean shouldAnalyze = tx.execute(status -> {
            PracticeSession session = findOwnedSession(userId, sessionId);

            // Retry path: FAILED sessions are allowed to re-trigger analysis (Audit §2.3)
            if (session.getStatus() == SessionStatus.FAILED) {
                log.info("Session {} is FAILED — updating response for retry.", sessionId);
                // Update the existing explanation in-place so getExplanationText() picks up the new text
                session.getResponses().stream()
                        .filter(r -> r.getResponseType() == CandidateResponse.ResponseType.APPROACH_EXPLANATION)
                        .findFirst()
                        .ifPresent(r -> r.setContentText(request.explanationText()));
                // Update code submission if provided
                if (request.code() != null && !request.code().isBlank()) {
                    CodeSubmission code = session.getCodeSubmission();
                    if (code == null) {
                        code = CodeSubmission.builder()
                                .session(session)
                                .language(request.language() != null ? request.language() : "unknown")
                                .codeText(request.code())
                                .build();
                    } else {
                        code.setCodeText(request.code());
                        code.setLanguage(request.language() != null ? request.language() : code.getLanguage());
                    }
                    session.setCodeSubmission(code);
                }
                session.setFailureReason(null);
                session.setStatus(SessionStatus.ANALYZING);
                sessionRepository.save(session);
                return true;
            }

            // Idempotency: if already past RESPONSE_SUBMITTED (and not FAILED), skip analysis
            if (session.getStatus().ordinal() > SessionStatus.RESPONSE_SUBMITTED.ordinal()) {
                log.info("Session {} already in status {}, returning current state.", sessionId, session.getStatus());
                return false;
            }

            if (session.getStatus() != SessionStatus.STARTED) {
                throw AppException.conflict("Session is not in STARTED state.");
            }

            // Persist the candidate's approach explanation
            CandidateResponse response = CandidateResponse.builder()
                    .session(session)
                    .responseType(CandidateResponse.ResponseType.APPROACH_EXPLANATION)
                    .contentText(request.explanationText())
                    .build();
            session.getResponses().add(response);

            // Persist optional code submission (DSA only, enforces UNIQUE via UPDATE logic)
            if (request.code() != null && !request.code().isBlank()) {
                CodeSubmission code = session.getCodeSubmission();
                if (code == null) {
                    code = CodeSubmission.builder()
                            .session(session)
                            .language(request.language() != null ? request.language() : "unknown")
                            .codeText(request.code())
                            .build();
                } else {
                    // Re-submission: update existing row
                    code.setCodeText(request.code());
                    code.setLanguage(request.language() != null ? request.language() : code.getLanguage());
                }
                session.setCodeSubmission(code);
            }

            session.setStatus(SessionStatus.ANALYZING);
            sessionRepository.save(session);
            return true;
        }); // ← commits here; ANALYZING is now durable and visible to concurrent GETs

        // Phase 2: AI analysis runs outside any open DB transaction
        if (Boolean.TRUE.equals(shouldAnalyze)) {
            aiAnalysisService.analyzeSession(sessionId);
        }

        // Phase 3: return the final persisted state (ANALYZED or FAILED)
        return SessionSummaryResponse.from(
                sessionRepository.findById(sessionId)
                        .orElseThrow(() -> AppException.notFound("Session not found: " + sessionId))
        );
    }

    // ── Submit Follow-up Answer ───────────────────────────────────────────────
    // Same split-transaction pattern as submitResponse(). (Audit §2.2)

    public SessionSummaryResponse submitFollowUpAnswer(Long userId, Long sessionId, SubmitFollowUpRequest request) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        // Phase 1: persist follow-up answer and mark FOLLOWUP_ANSWERED — commits immediately
        tx.execute(status -> {
            PracticeSession session = findOwnedSession(userId, sessionId);

            if (session.getStatus() != SessionStatus.FOLLOWUP_PENDING) {
                throw AppException.conflict(
                        "Session must be in FOLLOWUP_PENDING state to submit a follow-up answer. Current: " + session.getStatus()
                );
            }

            CandidateResponse answer = CandidateResponse.builder()
                    .session(session)
                    .responseType(CandidateResponse.ResponseType.FOLLOWUP_ANSWER)
                    .contentText(request.answerText())
                    .build();
            session.getResponses().add(answer);
            session.setStatus(SessionStatus.FOLLOWUP_ANSWERED);
            sessionRepository.save(session);
            return null;
        }); // ← commits here

        // Phase 2: AI follow-up analysis outside any open DB transaction
        aiAnalysisService.analyzeFollowUp(sessionId);

        // Phase 3: return the final persisted state
        return SessionSummaryResponse.from(
                sessionRepository.findById(sessionId)
                        .orElseThrow(() -> AppException.notFound("Session not found: " + sessionId))
        );
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SessionSummaryResponse getSession(Long userId, Long sessionId) {
        return SessionSummaryResponse.from(findOwnedSession(userId, sessionId));
    }

    @Transactional(readOnly = true)
    public Page<SessionSummaryResponse> listSessions(Long userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (status != null) {
            try {
                SessionStatus s = SessionStatus.valueOf(status.toUpperCase());
                return sessionRepository.findByUserIdAndStatusOrderByStartedAtDesc(userId, s, pageable)
                        .map(SessionSummaryResponse::from);
            } catch (IllegalArgumentException e) {
                throw AppException.badRequest("Invalid status filter: " + status);
            }
        }
        return sessionRepository.findByUserIdOrderByStartedAtDesc(userId, pageable)
                .map(SessionSummaryResponse::from);
    }

    // ── Ownership helper ──────────────────────────────────────────────────────

    public PracticeSession findOwnedSession(Long userId, Long sessionId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> AppException.forbidden("Session not found or access denied."));
    }

    // ── Daily cap ─────────────────────────────────────────────────────────────

    private void enforceDailyCap(Long userId) {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIDNIGHT);
        long count = sessionRepository.countSessionsForUserToday(userId, startOfDay);
        if (count >= dailyCap) {
            throw AppException.tooManyRequests(
                    "Daily session limit of " + dailyCap + " reached. Try again tomorrow."
            );
        }
    }

    // ── Scheduled cleanup: mark STARTED sessions older than 24h as ABANDONED ─

    @Scheduled(cron = "0 0 2 * * *") // runs daily at 02:00
    @Transactional
    public void cleanupAbandonedSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<PracticeSession> stale = sessionRepository.findByStatusAndStartedAtBefore(
                SessionStatus.STARTED, cutoff
        );
        if (!stale.isEmpty()) {
            stale.forEach(s -> s.setStatus(SessionStatus.ABANDONED));
            sessionRepository.saveAll(stale);
            log.info("Marked {} stale sessions as ABANDONED.", stale.size());
        }
    }
}
