package com.javaedu.repository;

import com.javaedu.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByTeacherId(Long teacherId);

    @Query("SELECT c FROM Course c JOIN c.students s WHERE s.id = :studentId")
    List<Course> findByStudentId(Long studentId);

    Optional<Course> findByGoogleClassroomId(String googleClassroomId);

    Optional<Course> findByGithubOrg(String githubOrg);

    List<Course> findByIsActiveTrue();

    @Query("SELECT c FROM Course c WHERE c.teacher.id = :teacherId AND c.isActive = true")
    List<Course> findActiveByTeacherId(Long teacherId);

    Optional<Course> findByEnrollmentCode(String enrollmentCode);
}
