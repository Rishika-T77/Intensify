package com.intensify.repository;

import com.intensify.entity.SkillMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SkillMetricRepository extends JpaRepository<SkillMetric, Long> {

    /**
     * Returns the last N skill metric rows for a user in a given category,
     * ordered oldest → newest so charts display chronologically.
     */
    @Query("""
           SELECT m FROM SkillMetric m
           WHERE m.user.id = :userId
             AND UPPER(m.category) = UPPER(:category)
           ORDER BY m.recordedAt ASC
           """)
    List<SkillMetric> findByUserIdAndCategory(
            @Param("userId") Long userId,
            @Param("category") String category
    );

    /** Count distinct sessions for a given user + category + dimension — used to gate chart visibility. */
    @Query("""
           SELECT COUNT(DISTINCT m.session.id) FROM SkillMetric m
           WHERE m.user.id = :userId
             AND UPPER(m.category) = UPPER(:category)
             AND m.dimension = :dimension
           """)
    long countDistinctSessionsByUserCategoryDimension(
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("dimension") String dimension
    );
}
