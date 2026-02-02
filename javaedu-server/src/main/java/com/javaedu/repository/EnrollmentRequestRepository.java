package com.javaedu.repository;

import com.javaedu.model.EnrollmentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRequestRepository extends JpaRepository<EnrollmentRequest, Long> {

    List<EnrollmentRequest> findByCourseId(Long courseId);

    List<EnrollmentRequest> findByCourseIdAndStatus(Long courseId, EnrollmentRequest.Status status);

    List<EnrollmentRequest> findByUserId(Long userId);

    Optional<EnrollmentRequest> findByCourseIdAndUserId(Long courseId, Long userId);

    @Query("SELECT er FROM EnrollmentRequest er WHERE er.course.teacher.id = :teacherId AND er.status = :status")
    List<EnrollmentRequest> findByTeacherIdAndStatus(Long teacherId, EnrollmentRequest.Status status);

    @Query("SELECT COUNT(er) FROM EnrollmentRequest er WHERE er.course.teacher.id = :teacherId AND er.status = 'PENDING'")
    int countPendingByTeacherId(Long teacherId);

    boolean existsByCourseIdAndUserIdAndStatus(Long courseId, Long userId, EnrollmentRequest.Status status);
}
