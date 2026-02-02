package com.javaedu.controller;

import com.javaedu.exception.BadRequestException;
import com.javaedu.exception.ResourceNotFoundException;
import com.javaedu.model.Course;
import com.javaedu.model.EnrollmentRequest;
import com.javaedu.model.User;
import com.javaedu.repository.CourseRepository;
import com.javaedu.repository.EnrollmentRequestRepository;
import com.javaedu.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Enrollment", description = "Student enrollment endpoints")
@Slf4j
public class EnrollmentController {

    private final CourseRepository courseRepository;
    private final EnrollmentRequestRepository enrollmentRequestRepository;
    private final AuthService authService;

    @GetMapping("/courses/{courseId}/enrollment-code")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get or generate enrollment code for a course")
    public ResponseEntity<EnrollmentCodeResponse> getEnrollmentCode(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        // Generate enrollment code if not exists
        if (course.getEnrollmentCode() == null) {
            course.setEnrollmentCode(generateEnrollmentCode());
            courseRepository.save(course);
        }

        return ResponseEntity.ok(new EnrollmentCodeResponse(
                course.getEnrollmentCode(),
                "/enroll/" + course.getEnrollmentCode()
        ));
    }

    @PostMapping("/courses/{courseId}/enrollment-code/regenerate")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Regenerate enrollment code for a course")
    public ResponseEntity<EnrollmentCodeResponse> regenerateEnrollmentCode(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        course.setEnrollmentCode(generateEnrollmentCode());
        courseRepository.save(course);

        return ResponseEntity.ok(new EnrollmentCodeResponse(
                course.getEnrollmentCode(),
                "/enroll/" + course.getEnrollmentCode()
        ));
    }

    @GetMapping("/enroll/{code}")
    @Operation(summary = "Get course info by enrollment code")
    public ResponseEntity<EnrollmentCourseInfo> getCourseByCode(@PathVariable String code) {
        Course course = courseRepository.findByEnrollmentCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "enrollment code", code));

        return ResponseEntity.ok(new EnrollmentCourseInfo(
                course.getId(),
                course.getName(),
                course.getDescription(),
                course.getTeacher().getName()
        ));
    }

    @PostMapping("/enroll/{code}")
    @Operation(summary = "Request enrollment in a course")
    @Transactional
    public ResponseEntity<EnrollmentRequestResponse> requestEnrollment(@PathVariable String code) {
        Course course = courseRepository.findByEnrollmentCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "enrollment code", code));

        User student = authService.getCurrentUser();

        // Check if already enrolled
        if (course.getStudents().contains(student)) {
            throw new BadRequestException("Already enrolled in this course");
        }

        // Check if already has pending request
        if (enrollmentRequestRepository.existsByCourseIdAndUserIdAndStatus(
                course.getId(), student.getId(), EnrollmentRequest.Status.PENDING)) {
            throw new BadRequestException("Enrollment request already pending");
        }

        // Create enrollment request
        EnrollmentRequest request = EnrollmentRequest.builder()
                .course(course)
                .user(student)
                .status(EnrollmentRequest.Status.PENDING)
                .build();

        enrollmentRequestRepository.save(request);

        log.info("Student {} requested enrollment in course {}", student.getEmail(), course.getName());

        return ResponseEntity.ok(new EnrollmentRequestResponse(
                request.getId(),
                "PENDING",
                "Enrollment request submitted. Awaiting teacher approval."
        ));
    }

    @GetMapping("/courses/{courseId}/enrollment-requests")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get pending enrollment requests for a course")
    public ResponseEntity<List<EnrollmentRequestDto>> getEnrollmentRequests(
            @PathVariable Long courseId,
            @RequestParam(required = false, defaultValue = "PENDING") String status) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        EnrollmentRequest.Status requestStatus = EnrollmentRequest.Status.valueOf(status.toUpperCase());
        List<EnrollmentRequest> requests = enrollmentRequestRepository
                .findByCourseIdAndStatus(courseId, requestStatus);

        return ResponseEntity.ok(requests.stream()
                .map(EnrollmentRequestDto::fromEntity)
                .toList());
    }

    @GetMapping("/enrollment-requests/pending")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get all pending enrollment requests for current teacher")
    public ResponseEntity<List<EnrollmentRequestDto>> getAllPendingRequests() {
        User teacher = authService.getCurrentUser();

        List<EnrollmentRequest> requests = enrollmentRequestRepository
                .findByTeacherIdAndStatus(teacher.getId(), EnrollmentRequest.Status.PENDING);

        return ResponseEntity.ok(requests.stream()
                .map(EnrollmentRequestDto::fromEntity)
                .toList());
    }

    @GetMapping("/enrollment-requests/pending/count")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get count of pending enrollment requests")
    public ResponseEntity<PendingCountResponse> getPendingCount() {
        User teacher = authService.getCurrentUser();
        int count = enrollmentRequestRepository.countPendingByTeacherId(teacher.getId());
        return ResponseEntity.ok(new PendingCountResponse(count));
    }

    @PostMapping("/enrollment-requests/{requestId}/approve")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Approve an enrollment request")
    @Transactional
    public ResponseEntity<Void> approveRequest(@PathVariable Long requestId) {
        EnrollmentRequest request = enrollmentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("EnrollmentRequest", "id", requestId));

        User teacher = authService.getCurrentUser();

        if (request.getStatus() != EnrollmentRequest.Status.PENDING) {
            throw new BadRequestException("Request is not pending");
        }

        // Add student to course
        Course course = request.getCourse();
        course.getStudents().add(request.getUser());
        courseRepository.save(course);

        // Update request status
        request.setStatus(EnrollmentRequest.Status.APPROVED);
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(teacher);
        enrollmentRequestRepository.save(request);

        log.info("Teacher {} approved enrollment for student {} in course {}",
                teacher.getEmail(), request.getUser().getEmail(), course.getName());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/enrollment-requests/{requestId}/deny")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Deny an enrollment request")
    @Transactional
    public ResponseEntity<Void> denyRequest(@PathVariable Long requestId) {
        EnrollmentRequest request = enrollmentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("EnrollmentRequest", "id", requestId));

        User teacher = authService.getCurrentUser();

        if (request.getStatus() != EnrollmentRequest.Status.PENDING) {
            throw new BadRequestException("Request is not pending");
        }

        request.setStatus(EnrollmentRequest.Status.DENIED);
        request.setResolvedAt(LocalDateTime.now());
        request.setResolvedBy(teacher);
        enrollmentRequestRepository.save(request);

        log.info("Teacher {} denied enrollment for student {} in course {}",
                teacher.getEmail(), request.getUser().getEmail(), request.getCourse().getName());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/enrollment-requests/bulk-approve")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Approve multiple enrollment requests")
    @Transactional
    public ResponseEntity<BulkActionResponse> bulkApprove(@RequestBody BulkActionRequest request) {
        User teacher = authService.getCurrentUser();
        int approved = 0;

        for (Long requestId : request.getRequestIds()) {
            try {
                EnrollmentRequest enrollmentRequest = enrollmentRequestRepository.findById(requestId)
                        .orElse(null);

                if (enrollmentRequest != null &&
                    enrollmentRequest.getStatus() == EnrollmentRequest.Status.PENDING) {

                    Course course = enrollmentRequest.getCourse();
                    course.getStudents().add(enrollmentRequest.getUser());
                    courseRepository.save(course);

                    enrollmentRequest.setStatus(EnrollmentRequest.Status.APPROVED);
                    enrollmentRequest.setResolvedAt(LocalDateTime.now());
                    enrollmentRequest.setResolvedBy(teacher);
                    enrollmentRequestRepository.save(enrollmentRequest);

                    approved++;
                }
            } catch (Exception e) {
                log.warn("Failed to approve request {}: {}", requestId, e.getMessage());
            }
        }

        return ResponseEntity.ok(new BulkActionResponse(approved, request.getRequestIds().size() - approved));
    }

    @PostMapping("/enrollment-requests/bulk-deny")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Deny multiple enrollment requests")
    @Transactional
    public ResponseEntity<BulkActionResponse> bulkDeny(@RequestBody BulkActionRequest request) {
        User teacher = authService.getCurrentUser();
        int denied = 0;

        for (Long requestId : request.getRequestIds()) {
            try {
                EnrollmentRequest enrollmentRequest = enrollmentRequestRepository.findById(requestId)
                        .orElse(null);

                if (enrollmentRequest != null &&
                    enrollmentRequest.getStatus() == EnrollmentRequest.Status.PENDING) {

                    enrollmentRequest.setStatus(EnrollmentRequest.Status.DENIED);
                    enrollmentRequest.setResolvedAt(LocalDateTime.now());
                    enrollmentRequest.setResolvedBy(teacher);
                    enrollmentRequestRepository.save(enrollmentRequest);

                    denied++;
                }
            } catch (Exception e) {
                log.warn("Failed to deny request {}: {}", requestId, e.getMessage());
            }
        }

        return ResponseEntity.ok(new BulkActionResponse(denied, request.getRequestIds().size() - denied));
    }

    private String generateEnrollmentCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // DTOs
    public record EnrollmentCodeResponse(String code, String enrollmentUrl) {}

    public record EnrollmentCourseInfo(Long id, String name, String description, String teacherName) {}

    public record EnrollmentRequestResponse(Long id, String status, String message) {}

    public record PendingCountResponse(int count) {}

    public record EnrollmentRequestDto(
            Long id,
            Long courseId,
            String courseName,
            Long userId,
            String userName,
            String userEmail,
            String status,
            LocalDateTime requestedAt
    ) {
        public static EnrollmentRequestDto fromEntity(EnrollmentRequest request) {
            return new EnrollmentRequestDto(
                    request.getId(),
                    request.getCourse().getId(),
                    request.getCourse().getName(),
                    request.getUser().getId(),
                    request.getUser().getName(),
                    request.getUser().getEmail(),
                    request.getStatus().name(),
                    request.getRequestedAt()
            );
        }
    }

    @Data
    public static class BulkActionRequest {
        private List<Long> requestIds;
    }

    public record BulkActionResponse(int succeeded, int failed) {}
}
