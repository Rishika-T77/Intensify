package com.intensify.service;

import com.intensify.entity.SkillMetric;
import com.intensify.repository.SkillMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final SkillMetricRepository skillMetricRepository;

    private static final int MIN_SESSIONS_FOR_CHART = 3;

    /**
     * Returns per-dimension trend data for the given user + category.
     * Dimensions with fewer than 3 session data points are excluded (replaced with a placeholder in the response).
     */
    @Transactional(readOnly = true)
    public ProgressSummary getSummary(Long userId, String category) {
        List<SkillMetric> metrics = skillMetricRepository.findByUserIdAndCategory(userId, category);

        // Group by dimension
        Map<String, List<SkillMetric>> byDimension = metrics.stream()
                .collect(Collectors.groupingBy(SkillMetric::getDimension));

        List<DimensionTrend> trends = new ArrayList<>();
        List<String> lockedDimensions = new ArrayList<>();

        for (Map.Entry<String, List<SkillMetric>> entry : byDimension.entrySet()) {
            String dimension = entry.getKey();
            List<SkillMetric> dimMetrics = entry.getValue();

            long distinctSessions = skillMetricRepository.countDistinctSessionsByUserCategoryDimension(
                    userId, category, dimension);

            if (distinctSessions >= MIN_SESSIONS_FOR_CHART) {
                List<DataPoint> dataPoints = dimMetrics.stream()
                        .map(m -> new DataPoint(m.getRecordedAt().toString(), m.getScore()))
                        .toList();
                trends.add(new DimensionTrend(dimension, dataPoints));
            } else {
                lockedDimensions.add(dimension);
            }
        }

        return new ProgressSummary(category, trends, lockedDimensions, MIN_SESSIONS_FOR_CHART);
    }

    // ── Response records ──────────────────────────────────────────────────────

    public record ProgressSummary(
            String category,
            List<DimensionTrend> trends,
            List<String> lockedDimensions,
            int minSessionsRequired
    ) {}

    public record DimensionTrend(
            String dimension,
            List<DataPoint> dataPoints
    ) {}

    public record DataPoint(
            String recordedAt,
            Integer score
    ) {}
}
