package com.javaedu.repository;

import com.javaedu.model.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByUserId(Long userId);

    List<Submission> findByExerciseId(Long exerciseId);

    List<Submission> findByUserIdAndExerciseId(Long userId, Long exerciseId);

    Page<Submission> findByUserIdOrderBySubmittedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT s FROM Submission s WHERE s.user.id = :userId AND s.exercise.id = :exerciseId ORDER BY s.submittedAt DESC")
    List<Submission> findLatestByUserAndExercise(Long userId, Long exerciseId);

    Optional<Submission> findFirstByUserIdAndExerciseIdOrderBySubmittedAtDesc(Long userId, Long exerciseId);

    @Query("SELECT s FROM Submission s WHERE s.exercise.course.id = :courseId ORDER BY s.submittedAt DESC")
    Page<Submission> findByCourseId(Long courseId, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.user.id = :userId AND s.exercise.id = :exerciseId")
    int countByUserIdAndExerciseId(Long userId, Long exerciseId);

    @Query("SELECT s FROM Submission s WHERE s.status = :status")
    List<Submission> findByStatus(Submission.Status status);

    @Query("SELECT s FROM Submission s WHERE s.submittedAt BETWEEN :start AND :end")
    List<Submission> findBySubmittedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT s.user.id) FROM Submission s WHERE s.exercise.id = :exerciseId")
    int countDistinctUsersByExerciseId(Long exerciseId);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.user.id = :userId AND s.exercise.course.id = :courseId")
    long countByUserIdAndCourseId(Long userId, Long courseId);
}
