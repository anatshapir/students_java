package com.javaedu.repository;

import com.javaedu.model.StudentAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentAnalyticsRepository extends JpaRepository<StudentAnalytics, Long> {

    Optional<StudentAnalytics> findByUserIdAndExerciseId(Long userId, Long exerciseId);

    List<StudentAnalytics> findByUserId(Long userId);

    List<StudentAnalytics> findByExerciseId(Long exerciseId);

    @Query("SELECT AVG(sa.attempts) FROM StudentAnalytics sa WHERE sa.exercise.id = :exerciseId")
    Double findAverageAttemptsByExerciseId(Long exerciseId);

    @Query("SELECT AVG(sa.timeSpentMinutes) FROM StudentAnalytics sa WHERE sa.exercise.id = :exerciseId AND sa.completedAt IS NOT NULL")
    Double findAverageTimeToCompleteByExerciseId(Long exerciseId);

    @Query("SELECT sa FROM StudentAnalytics sa WHERE sa.user.id = :userId AND sa.exercise.course.id = :courseId")
    List<StudentAnalytics> findByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT COUNT(sa) FROM StudentAnalytics sa WHERE sa.exercise.id = :exerciseId AND sa.completedAt IS NOT NULL")
    int countCompletedByExerciseId(Long exerciseId);
}
