package com.intensify.service;

import com.intensify.entity.*;
import com.intensify.entity.PracticeSession.SessionStatus;
import com.intensify.integration.EvaluationResult;
import com.intensify.integration.GeminiProvider;
import com.intensify.integration.OpenAIProvider;
import com.intensify.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AIAnalysisServiceTest {

    @Mock
    private OpenAIProvider openAIProvider;
    @Mock
    private GeminiProvider geminiProvider;
    @Mock
    private AnalysisResultRepository analysisResultRepository;
    @Mock
    private PracticeSessionRepository sessionRepository;
    @Mock
    private SkillMetricRepository skillMetricRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private AIAnalysisService aiAnalysisService;

    @BeforeEach
    void setUp() {
        TransactionStatus status = new SimpleTransactionStatus();
        when(transactionManager.getTransaction(any())).thenReturn(status);

        aiAnalysisService = new AIAnalysisService(
                openAIProvider,
                geminiProvider,
                analysisResultRepository,
                sessionRepository,
                skillMetricRepository,
                userRepository,
                transactionManager
        );

        ReflectionTestUtils.setField(aiAnalysisService, "configuredProvider", "gemini");
        ReflectionTestUtils.setField(aiAnalysisService, "timeoutSeconds", 30);
    }

    private EvaluationResult.CategoryScore createCategoryScore(String dimension, int score, boolean applicable) {
        EvaluationResult.CategoryScore cs = new EvaluationResult.CategoryScore();
        cs.setDimension(dimension);
        cs.setScore(score);
        cs.setApplicable(applicable);
        return cs;
    }

    private EvaluationResult.FeedbackPoint createFeedbackPoint(String point, String evidence) {
        EvaluationResult.FeedbackPoint fp = new EvaluationResult.FeedbackPoint();
        fp.setPoint(point);
        fp.setEvidence(evidence);
        return fp;
    }

    @Test
    @DisplayName("analyzeSession verifies valid evidence quotes and discards hallucinated evidence")
    void analyzeSession_verifiesEvidence() {
        Long sessionId = 1L;

        User user = User.builder().id(10L).email("test@example.com").build();
        InterviewQuestion question = InterviewQuestion.builder()
                .id(100L)
                .title("Two Sum")
                .category("DSA")
                .difficulty(InterviewQuestion.Difficulty.EASY)
                .promptText("Find two numbers that add up to target.")
                .build();

        PracticeSession session = PracticeSession.builder()
                .id(sessionId)
                .user(user)
                .question(question)
                .status(SessionStatus.RESPONSE_SUBMITTED)
                .build();

        CandidateResponse response = CandidateResponse.builder()
                .session(session)
                .responseType(CandidateResponse.ResponseType.APPROACH_EXPLANATION)
                .contentText("I will use a HashMap to store complements as I iterate through the array in O(N) time.")
                .build();
        session.getResponses().add(response);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        EvaluationResult evalResult = new EvaluationResult();
        evalResult.setOverallScore(90);
        evalResult.setCategoryScores(List.of(
                createCategoryScore("REASONING_QUALITY", 90, true),
                createCategoryScore("COMPLEXITY_ANALYSIS", 90, true)
        ));
        // Valid verbatim quote
        evalResult.setStrengths(List.of(
                createFeedbackPoint("Good complexity reasoning", "O(N) time")
        ));
        // Hallucinated evidence not in candidate text
        evalResult.setWeaknesses(List.of(
                createFeedbackPoint("Missing binary search explanation", "I will sort the array and use binary search")
        ));

        when(geminiProvider.evaluate(any(), any())).thenReturn(evalResult);

        aiAnalysisService.analyzeSession(sessionId);

        // Verify analysis result persistence
        ArgumentCaptor<AnalysisResult> resultCaptor = ArgumentCaptor.forClass(AnalysisResult.class);
        verify(analysisResultRepository).save(resultCaptor.capture());

        AnalysisResult savedResult = resultCaptor.getValue();
        assertNotNull(savedResult);
        assertEquals(90, savedResult.getOverallScore());

        // Verbatim quote retained
        assertEquals(1, savedResult.getStrengths().size());
        assertEquals("O(N) time", savedResult.getStrengths().get(0).getEvidence());

        // Hallucinated evidence discarded silently
        assertEquals(0, savedResult.getWeaknesses().size());

        // Session updated to ANALYZED
        assertEquals(SessionStatus.ANALYZED, session.getStatus());
    }

    @Test
    @DisplayName("analyzeSession marks session as FAILED when provider throws exception")
    void analyzeSession_failureHandled() {
        Long sessionId = 2L;

        User user = User.builder().id(10L).email("test@example.com").build();
        InterviewQuestion question = InterviewQuestion.builder()
                .id(101L)
                .title("Design Rate Limiter")
                .category("SYSTEM_DESIGN")
                .difficulty(InterviewQuestion.Difficulty.MEDIUM)
                .promptText("Design a distributed rate limiter.")
                .build();

        PracticeSession session = PracticeSession.builder()
                .id(sessionId)
                .user(user)
                .question(question)
                .status(SessionStatus.RESPONSE_SUBMITTED)
                .build();

        CandidateResponse response = CandidateResponse.builder()
                .session(session)
                .responseType(CandidateResponse.ResponseType.APPROACH_EXPLANATION)
                .contentText("Token bucket algorithm using Redis for distributed rate limiting.")
                .build();
        session.getResponses().add(response);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(geminiProvider.evaluate(any(), any())).thenThrow(new com.intensify.integration.AIProviderException("TIMEOUT"));

        aiAnalysisService.analyzeSession(sessionId);

        // Verify session marked FAILED
        assertEquals(SessionStatus.FAILED, session.getStatus());
        assertEquals("PROVIDER_ERROR", session.getFailureReason());
    }
}
