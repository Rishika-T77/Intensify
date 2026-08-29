package com.intensify.controller;

import com.intensify.dto.QuestionDtos.*;
import com.intensify.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public ResponseEntity<?> listQuestions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<QuestionSummaryResponse> result = questionService.listQuestions(category, difficulty, page, size);
        return ResponseEntity.ok(Map.of("data", result, "error", ""));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getQuestion(@PathVariable Long id) {
        QuestionDetailResponse question = questionService.getQuestion(id);
        return ResponseEntity.ok(Map.of("data", question, "error", ""));
    }
}
