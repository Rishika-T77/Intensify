package com.intensify.dto;

import com.intensify.entity.InterviewQuestion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class QuestionDtos {

    public record QuestionSummaryResponse(
            Long id,
            String category,
            String title,
            String difficulty,
            LocalDateTime createdAt
    ) {
        public static QuestionSummaryResponse from(InterviewQuestion q) {
            return new QuestionSummaryResponse(
                    q.getId(),
                    q.getCategory(),
                    q.getTitle(),
                    q.getDifficulty().name(),
                    q.getCreatedAt()
            );
        }
    }

    public record QuestionDetailResponse(
            Long id,
            String category,
            String title,
            String promptText,
            String difficulty,
            LocalDateTime createdAt
    ) {
        public static QuestionDetailResponse from(InterviewQuestion q) {
            return new QuestionDetailResponse(
                    q.getId(),
                    q.getCategory(),
                    q.getTitle(),
                    q.getPromptText(),
                    q.getDifficulty().name(),
                    q.getCreatedAt()
            );
        }
    }
}
