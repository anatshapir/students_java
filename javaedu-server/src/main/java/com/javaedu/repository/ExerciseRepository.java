package com.javaedu.repository;

import com.javaedu.model.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {

    List<Exercise> findByCourseId(Long courseId);

    List<Exercise> findByCourseIdAndIsPublishedTrue(Long courseId);

    List<Exercise> findByCategory(Exercise.Category category);

    List<Exercise> findByDifficulty(Exercise.Difficulty difficulty);

    @Query("SELECT e FROM Exercise e WHERE e.course.id = :courseId AND e.isPublished = true ORDER BY e.dueDate ASC")
    List<Exercise> findPublishedByCourseIdOrderByDueDate(Long courseId);

    @Query("SELECT e FROM Exercise e WHERE e.dueDate BETWEEN :start AND :end AND e.isPublished = true")
    List<Exercise> findByDueDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT e FROM Exercise e JOIN e.course c JOIN c.students s WHERE s.id = :studentId AND e.isPublished = true")
    List<Exercise> findAvailableForStudent(Long studentId);

    @Query("SELECT COUNT(e) FROM Exercise e WHERE e.course.id = :courseId")
    long countByCourseId(Long courseId);

    @Query("SELECT e FROM Exercise e WHERE e.course.teacher.id = :teacherId ORDER BY e.createdAt DESC")
    List<Exercise> findByTeacherId(Long teacherId);
}
