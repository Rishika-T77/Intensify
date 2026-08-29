package com.intensify.controller;

import com.intensify.dto.SessionDtos.*;
import com.intensify.entity.AnalysisResult;
import com.intensify.exception.AppException;
import com.intensify.repository.AnalysisResultRepository;
import com.intensify.security.SecurityUtils;
import com.intensify.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final PracticeSessionService sessionService;
    private final FollowUpService followUpService;
    private final AnalysisResultRepository analysisResultRepository;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<?> createSession(@Valid @RequestBody CreateSessionRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        SessionSummaryResponse session = sessionService.createSession(userId, request);
        return ResponseEntity.ok(Map.of("data", session, "error", ""));
    }

    @PostMapping("/{id}/response")
    public ResponseEntity<?> submitResponse(
            @PathVariable Long id,
            @Valid @RequestBody SubmitResponseRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        SessionSummaryResponse session = sessionService.submitResponse(userId, id, request);
        return ResponseEntity.ok(Map.of("data", session, "error", ""));
    }

    @GetMapping("/{id}/analysis")
    public ResponseEntity<?> getAnalysis(
            @PathVariable Long id,
            @RequestParam(defaultValue = "MAIN") String type) {
        Long userId = securityUtils.getCurrentUserId();
        // Ownership check
        sessionService.findOwnedSession(userId, id);

        AnalysisResult.AnalysisType analysisType;
        try {
            analysisType = AnalysisResult.AnalysisType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest("Invalid analysis type. Use MAIN or FOLLOWUP.");
        }

        AnalysisResult result = analysisResultRepository
                .findBySessionIdAndAnalysisType(id, analysisType)
                .orElseThrow(() -> AppException.notFound("Analysis not ready yet for session: " + id));

        return ResponseEntity.ok(Map.of("data", result, "error", ""));
    }

    @GetMapping("/{id}/followup")
    public ResponseEntity<?> getFollowUpQuestion(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        FollowUpQuestionResponse followUp = followUpService.prepareFollowUp(userId, id);
        return ResponseEntity.ok(Map.of("data", followUp, "error", ""));
    }

    @PostMapping("/{id}/followup-response")
    public ResponseEntity<?> submitFollowUpAnswer(
            @PathVariable Long id,
            @Valid @RequestBody SubmitFollowUpRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        SessionSummaryResponse session = sessionService.submitFollowUpAnswer(userId, id, request);
        return ResponseEntity.ok(Map.of("data", session, "error", ""));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSession(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        SessionSummaryResponse session = sessionService.getSession(userId, id);
        return ResponseEntity.ok(Map.of("data", session, "error", ""));
    }

    @GetMapping
    public ResponseEntity<?> listSessions(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = securityUtils.getCurrentUserId();
        Page<SessionSummaryResponse> sessions = sessionService.listSessions(userId, status, page, size);
        return ResponseEntity.ok(Map.of("data", sessions, "error", ""));
    }
}
