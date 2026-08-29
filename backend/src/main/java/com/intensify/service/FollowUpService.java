package com.intensify.service;

import com.intensify.dto.SessionDtos.*;
import com.intensify.entity.FollowUpQuestion;
import com.intensify.entity.PracticeSession;
import com.intensify.entity.PracticeSession.SessionStatus;
import com.intensify.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowUpService {

    private final PracticeSessionService sessionService;

    /**
     * After MAIN analysis completes, extract the follow-up question from the analysis result
     * and persist it, transitioning the session to FOLLOWUP_PENDING.
     */
    @Transactional
    public FollowUpQuestionResponse prepareFollowUp(Long userId, Long sessionId) {
        PracticeSession session = sessionService.findOwnedSession(userId, sessionId);

        FollowUpQuestion existing = session.getFollowUpQuestion();
        if (existing != null) {
            if (session.getStatus() == SessionStatus.ANALYZED) {
                session.setStatus(SessionStatus.FOLLOWUP_PENDING);
            }
            log.debug("Session {} already has a follow-up question — returning it (status={}).",
                    sessionId, session.getStatus());
            return new FollowUpQuestionResponse(sessionId, existing.getQuestionText());
        }

        // First call: valid from ANALYZED or FOLLOWUP_PENDING state
        if (session.getStatus() != SessionStatus.ANALYZED && session.getStatus() != SessionStatus.FOLLOWUP_PENDING) {
            throw AppException.conflict(
                    "Session must be in ANALYZED state to retrieve the follow-up question. Current: " + session.getStatus()
            );
        }

        // Generate (or fall back to) a follow-up question and transition to FOLLOWUP_PENDING
        String defaultPrompt = "Can you elaborate on the time and space complexity trade-offs of your chosen approach if the input constraints scale significantly?";
        FollowUpQuestion followUpQuestion = FollowUpQuestion.builder()
                .session(session)
                .questionText(defaultPrompt)
                .build();
        session.setFollowUpQuestion(followUpQuestion);
        session.setStatus(SessionStatus.FOLLOWUP_PENDING);

        return new FollowUpQuestionResponse(sessionId, followUpQuestion.getQuestionText());
    }
}
