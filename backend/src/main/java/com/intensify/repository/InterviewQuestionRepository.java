package com.intensify.repository;

import com.intensify.entity.InterviewQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    Page<InterviewQuestion> findByCategoryIgnoreCase(String category, Pageable pageable);

    Page<InterviewQuestion> findByCategoryIgnoreCaseAndDifficulty(
            String category,
            InterviewQuestion.Difficulty difficulty,
            Pageable pageable
    );
}
