package com.intensify.service;

import com.intensify.dto.SessionDtos.FollowUpQuestionResponse;
import com.intensify.entity.FollowUpQuestion;
import com.intensify.entity.PracticeSession;
import com.intensify.entity.PracticeSession.SessionStatus;
import com.intensify.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowUpServiceTest {

    @Mock
    private PracticeSessionService sessionService;

    @InjectMocks
    private FollowUpService followUpService;

    @Test
    @DisplayName("prepareFollowUp transitions ANALYZED to FOLLOWUP_PENDING when returning existing follow-up question")
    void prepareFollowUp_existingQuestion_transitionsToFollowUpPending() {
        Long userId = 1L;
        Long sessionId = 10L;

        PracticeSession session = PracticeSession.builder()
                .id(sessionId)
                .status(SessionStatus.ANALYZED)
                .build();

        FollowUpQuestion followUp = FollowUpQuestion.builder()
                .session(session)
                .questionText("What is the time complexity if n is 1 million?")
                .build();
        session.setFollowUpQuestion(followUp);

        when(sessionService.findOwnedSession(userId, sessionId)).thenReturn(session);

        FollowUpQuestionResponse response = followUpService.prepareFollowUp(userId, sessionId);

        assertNotNull(response);
        assertEquals("What is the time complexity if n is 1 million?", response.questionText());
        assertEquals(SessionStatus.FOLLOWUP_PENDING, session.getStatus());
    }

    @Test
    @DisplayName("prepareFollowUp fails when session is in invalid state (e.g. STARTED)")
    void prepareFollowUp_invalidStatus_throwsException() {
        Long userId = 1L;
        Long sessionId = 11L;

        PracticeSession session = PracticeSession.builder()
                .id(sessionId)
                .status(SessionStatus.STARTED)
                .build();

        when(sessionService.findOwnedSession(userId, sessionId)).thenReturn(session);

        AppException ex = assertThrows(AppException.class, () ->
                followUpService.prepareFollowUp(userId, sessionId)
        );

        assertTrue(ex.getMessage().contains("Session must be in ANALYZED state"));
    }
}
