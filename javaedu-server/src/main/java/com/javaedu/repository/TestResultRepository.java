package com.javaedu.repository;

import com.javaedu.model.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    List<TestResult> findBySubmissionId(Long submissionId);

    @Query("SELECT COUNT(tr) FROM TestResult tr WHERE tr.submission.id = :submissionId AND tr.passed = true")
    int countPassedBySubmissionId(Long submissionId);

    @Query("SELECT COUNT(tr) FROM TestResult tr WHERE tr.submission.id = :submissionId")
    int countBySubmissionId(Long submissionId);

    @Query("SELECT tr.errorMessage, COUNT(tr) FROM TestResult tr " +
           "WHERE tr.testCase.exercise.id = :exerciseId AND tr.passed = false AND tr.errorMessage IS NOT NULL " +
           "GROUP BY tr.errorMessage ORDER BY COUNT(tr) DESC")
    List<Object[]> findCommonErrorsByExerciseId(Long exerciseId);
}
