package com.intensify.controller;

import com.intensify.security.SecurityUtils;
import com.intensify.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;
    private final SecurityUtils securityUtils;

    /**
     * GET /api/progress/summary?category=DSA
     * Returns per-dimension trend data for the given category.
     * Dimensions with fewer than 3 sessions are returned as "locked" (chart hidden until threshold met).
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestParam(defaultValue = "DSA") String category) {
        Long userId = securityUtils.getCurrentUserId();
        ProgressService.ProgressSummary summary = progressService.getSummary(userId, category);
        return ResponseEntity.ok(Map.of("data", summary, "error", ""));
    }
}
