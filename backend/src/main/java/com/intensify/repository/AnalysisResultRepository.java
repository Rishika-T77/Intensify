package com.intensify.repository;

import com.intensify.entity.AnalysisResult;
import com.intensify.entity.AnalysisResult.AnalysisType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    Optional<AnalysisResult> findBySessionIdAndAnalysisType(Long sessionId, AnalysisType analysisType);
}
