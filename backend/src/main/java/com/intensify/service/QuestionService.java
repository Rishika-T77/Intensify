package com.intensify.service;

import com.intensify.dto.QuestionDtos.*;
import com.intensify.entity.InterviewQuestion;
import com.intensify.exception.AppException;
import com.intensify.repository.InterviewQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final InterviewQuestionRepository questionRepository;

    public Page<QuestionSummaryResponse> listQuestions(String category, String difficulty, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("difficulty").and(Sort.by("title")));

        if (category != null && difficulty != null) {
            InterviewQuestion.Difficulty diff = parseDifficulty(difficulty);
            return questionRepository
                    .findByCategoryIgnoreCaseAndDifficulty(category, diff, pageable)
                    .map(QuestionSummaryResponse::from);
        } else if (category != null) {
            return questionRepository
                    .findByCategoryIgnoreCase(category, pageable)
                    .map(QuestionSummaryResponse::from);
        } else {
            return questionRepository.findAll(pageable).map(QuestionSummaryResponse::from);
        }
    }

    public QuestionDetailResponse getQuestion(Long id) {
        return questionRepository.findById(id)
                .map(QuestionDetailResponse::from)
                .orElseThrow(() -> AppException.notFound("Question not found: " + id));
    }

    private InterviewQuestion.Difficulty parseDifficulty(String difficulty) {
        try {
            return InterviewQuestion.Difficulty.valueOf(difficulty.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest("Invalid difficulty: " + difficulty + ". Valid values: EASY, MEDIUM, HARD");
        }
    }
}
