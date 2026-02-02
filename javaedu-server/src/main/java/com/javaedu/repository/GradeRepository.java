package com.javaedu.repository;

import com.javaedu.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    Optional<Grade> findBySubmissionId(Long submissionId);

    @Query("SELECT g FROM Grade g WHERE g.submission.user.id = :userId")
    List<Grade> findByUserId(Long userId);

    @Query("SELECT g FROM Grade g WHERE g.submission.exercise.id = :exerciseId")
    List<Grade> findByExerciseId(Long exerciseId);

    @Query("SELECT g FROM Grade g WHERE g.submission.user.id = :userId AND g.submission.exercise.id = :exerciseId " +
           "ORDER BY g.score DESC")
    List<Grade> findByUserIdAndExerciseId(Long userId, Long exerciseId);

    @Query("SELECT g FROM Grade g WHERE g.submission.user.id = :userId AND g.submission.exercise.id = :exerciseId " +
           "ORDER BY g.score DESC LIMIT 1")
    Optional<Grade> findBestGradeByUserIdAndExerciseId(Long userId, Long exerciseId);

    @Query("SELECT AVG(g.score * 100.0 / g.maxScore) FROM Grade g WHERE g.submission.exercise.id = :exerciseId")
    Double findAverageScoreByExerciseId(Long exerciseId);

    @Query("SELECT g FROM Grade g WHERE g.submission.exercise.course.id = :courseId AND g.submission.user.id = :userId")
    List<Grade> findByCourseIdAndUserId(Long courseId, Long userId);
}
