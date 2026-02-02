package com.javaedu.repository;

import com.javaedu.model.ErrorPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ErrorPatternRepository extends JpaRepository<ErrorPattern, Long> {

    List<ErrorPattern> findByExerciseId(Long exerciseId);

    Optional<ErrorPattern> findByExerciseIdAndPattern(Long exerciseId, String pattern);

    @Query("SELECT ep FROM ErrorPattern ep WHERE ep.exercise.id = :exerciseId ORDER BY ep.occurrenceCount DESC")
    List<ErrorPattern> findByExerciseIdOrderByOccurrenceCountDesc(Long exerciseId);

    @Query("SELECT ep FROM ErrorPattern ep WHERE ep.exercise.id = :exerciseId AND ep.occurrenceCount >= :minCount")
    List<ErrorPattern> findFrequentPatterns(Long exerciseId, int minCount);
}
