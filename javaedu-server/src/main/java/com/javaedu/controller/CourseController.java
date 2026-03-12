package com.javaedu.controller;

import com.javaedu.exception.ResourceNotFoundException;
import com.javaedu.model.Course;
import com.javaedu.model.User;
import com.javaedu.repository.CourseRepository;
import com.javaedu.repository.UserRepository;
import com.javaedu.service.AuthService;
import com.javaedu.service.GoogleClassroomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import com.javaedu.service.GradeExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Course management endpoints")
public class CourseController {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final GoogleClassroomService googleClassroomService;
    private final GradeExportService gradeExportService;

    @GetMapping
    @Operation(summary = "Get courses for current user")
    public ResponseEntity<List<CourseDto>> getCourses(
            @RequestParam(required = false) String filter) {
        User user = authService.getCurrentUser();
        List<Course> courses;

        if (user.getRole() == User.Role.TEACHER || user.getRole() == User.Role.ADMIN) {
            courses = courseRepository.findByTeacherId(user.getId());
        } else {
            courses = courseRepository.findByStudentId(user.getId());
        }

        // Apply filter
        if ("active".equalsIgnoreCase(filter)) {
            courses = courses.stream().filter(c -> Boolean.TRUE.equals(c.getIsActive())).toList();
        } else if ("archived".equalsIgnoreCase(filter)) {
            courses = courses.stream().filter(c -> !Boolean.TRUE.equals(c.getIsActive())).toList();
        }

        return ResponseEntity.ok(courses.stream().map(CourseDto::fromEntity).toList());
    }

    @GetMapping("/{courseId}")
    @Operation(summary = "Get course by ID")
    public ResponseEntity<CourseDto> getCourse(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        return ResponseEntity.ok(CourseDto.fromEntity(course));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Create a new course")
    public ResponseEntity<CourseDto> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        User teacher = authService.getCurrentUser();

        Course course = Course.builder()
                .name(request.getName())
                .description(request.getDescription())
                .teacher(teacher)
                .isActive(true)
                .enrollmentCode(generateEnrollmentCode())
                .build();

        course = courseRepository.save(course);
        return ResponseEntity.ok(CourseDto.fromEntity(course));
    }

    @PostMapping("/wizard")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Create a course via wizard (supports Google Classroom import)")
    @Transactional
    public ResponseEntity<CourseDto> createCourseWizard(@Valid @RequestBody CourseWizardRequest request) {
        User teacher = authService.getCurrentUser();

        Course course = Course.builder()
                .name(request.getName())
                .description(request.getDescription())
                .teacher(teacher)
                .isActive(true)
                .enrollmentCode(generateEnrollmentCode())
                .build();

        // If linked to Google Classroom
        if (request.getGoogleClassroomId() != null && !request.getGoogleClassroomId().isEmpty()) {
            course.setGoogleClassroomId(request.getGoogleClassroomId());
            course.setAutoSyncEnabled(Boolean.TRUE.equals(request.getAutoSyncEnabled()));
        }

        course = courseRepository.save(course);

        // Add students manually specified
        if (request.getStudentEmails() != null && !request.getStudentEmails().isEmpty()) {
            for (String email : request.getStudentEmails()) {
                User student = userRepository.findByEmail(email.trim())
                        .orElseGet(() -> {
                            User newStudent = User.builder()
                                    .email(email.trim())
                                    .name(email.trim().split("@")[0])
                                    .role(User.Role.STUDENT)
                                    .isActive(true)
                                    .build();
                            return userRepository.save(newStudent);
                        });
                course.getStudents().add(student);
            }
            course = courseRepository.save(course);
        }

        // Sync from Google Classroom if specified
        if (request.getGoogleClassroomId() != null && request.getSyncStudentsNow() != null
                && request.getSyncStudentsNow()) {
            String accessToken = googleClassroomService.getValidAccessToken(teacher);
            googleClassroomService.syncClassroomToCourse(accessToken, request.getGoogleClassroomId(), course.getId());
            // Refresh course to get updated student count
            course = courseRepository.findById(course.getId()).orElse(course);
        }

        return ResponseEntity.ok(CourseDto.fromEntity(course));
    }

    @PutMapping("/{courseId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Update a course")
    public ResponseEntity<CourseDto> updateCourse(
            @PathVariable Long courseId,
            @RequestBody UpdateCourseRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (request.getName() != null) course.setName(request.getName());
        if (request.getDescription() != null) course.setDescription(request.getDescription());
        if (request.getIsActive() != null) course.setIsActive(request.getIsActive());
        if (request.getAutoSyncEnabled() != null) course.setAutoSyncEnabled(request.getAutoSyncEnabled());

        course = courseRepository.save(course);
        return ResponseEntity.ok(CourseDto.fromEntity(course));
    }

    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Delete a course")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        courseRepository.delete(course);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{courseId}/students")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get students enrolled in a course")
    public ResponseEntity<List<StudentDto>> getStudents(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        List<StudentDto> students = course.getStudents().stream()
                .map(StudentDto::fromEntity)
                .toList();

        return ResponseEntity.ok(students);
    }

    @PostMapping("/{courseId}/students/{studentId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Enroll a student in a course")
    public ResponseEntity<Void> enrollStudent(
            @PathVariable Long courseId,
            @PathVariable Long studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        course.getStudents().add(student);
        courseRepository.save(course);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/students/bulk")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Enroll multiple students by email")
    @Transactional
    public ResponseEntity<BulkEnrollResponse> bulkEnrollStudents(
            @PathVariable Long courseId,
            @RequestBody BulkEnrollRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        int added = 0;
        int existing = 0;

        for (String email : request.getEmails()) {
            User student = userRepository.findByEmail(email.trim())
                    .orElseGet(() -> {
                        User newStudent = User.builder()
                                .email(email.trim())
                                .name(email.trim().split("@")[0])
                                .role(User.Role.STUDENT)
                                .isActive(true)
                                .build();
                        return userRepository.save(newStudent);
                    });

            if (!course.getStudents().contains(student)) {
                course.getStudents().add(student);
                added++;
            } else {
                existing++;
            }
        }

        courseRepository.save(course);

        return ResponseEntity.ok(new BulkEnrollResponse(added, existing));
    }

    @DeleteMapping("/{courseId}/students/{studentId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Remove a student from a course")
    public ResponseEntity<Void> removeStudent(
            @PathVariable Long courseId,
            @PathVariable Long studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        course.getStudents().remove(student);
        courseRepository.save(course);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{courseId}/enroll")
    @Operation(summary = "Self-enroll in a course (for students)")
    public ResponseEntity<Void> selfEnroll(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        User student = authService.getCurrentUser();

        if (!course.getStudents().contains(student)) {
            course.getStudents().add(student);
            courseRepository.save(course);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{courseId}/sync-status")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get sync status for a course")
    public ResponseEntity<SyncStatusResponse> getSyncStatus(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        boolean isLinked = course.getGoogleClassroomId() != null;

        return ResponseEntity.ok(new SyncStatusResponse(
                isLinked,
                course.getGoogleClassroomId(),
                course.getLastSyncedAt(),
                course.getAutoSyncEnabled()
        ));
    }

    @PostMapping("/{courseId}/sync")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Trigger manual sync with Google Classroom")
    @Transactional
    public ResponseEntity<SyncResultResponse> syncCourse(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (course.getGoogleClassroomId() == null) {
            return ResponseEntity.badRequest().body(new SyncResultResponse(
                    false, 0, 0, "Course is not linked to Google Classroom"
            ));
        }

        User teacher = authService.getCurrentUser();
        String accessToken = googleClassroomService.getValidAccessToken(teacher);

        GoogleClassroomService.SyncResult result = googleClassroomService.syncClassroomToCourse(
                accessToken, course.getGoogleClassroomId(), courseId);

        return ResponseEntity.ok(new SyncResultResponse(
                true, result.added(), result.removed(), null
        ));
    }

    @GetMapping("/{courseId}/sync/preview")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Preview sync changes before applying")
    public ResponseEntity<GoogleClassroomService.SyncPreview> previewSync(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (course.getGoogleClassroomId() == null) {
            return ResponseEntity.badRequest().build();
        }

        User teacher = authService.getCurrentUser();
        String accessToken = googleClassroomService.getValidAccessToken(teacher);

        return ResponseEntity.ok(googleClassroomService.previewSync(
                accessToken, course.getGoogleClassroomId(), courseId));
    }

    @PostMapping("/{courseId}/sync/apply")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Apply selected sync changes")
    @Transactional
    public ResponseEntity<SyncResultResponse> applySyncChanges(
            @PathVariable Long courseId,
            @RequestBody ApplySyncRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (course.getGoogleClassroomId() == null) {
            return ResponseEntity.badRequest().body(new SyncResultResponse(
                    false, 0, 0, "Course is not linked to Google Classroom"
            ));
        }

        User teacher = authService.getCurrentUser();
        String accessToken = googleClassroomService.getValidAccessToken(teacher);

        GoogleClassroomService.SyncResult result = googleClassroomService.applySyncChanges(
                accessToken, course.getGoogleClassroomId(), courseId,
                request.getEmailsToAdd(), request.getIdsToRemove());

        return ResponseEntity.ok(new SyncResultResponse(
                true, result.added(), result.removed(), null
        ));
    }

    @PostMapping("/{courseId}/link-google")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Link course to Google Classroom")
    @Transactional
    public ResponseEntity<CourseDto> linkToGoogleClassroom(
            @PathVariable Long courseId,
            @RequestBody LinkGoogleRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        course.setGoogleClassroomId(request.getGoogleClassroomId());
        course.setAutoSyncEnabled(Boolean.TRUE.equals(request.getAutoSyncEnabled()));

        // Optionally sync students immediately
        if (Boolean.TRUE.equals(request.getSyncNow())) {
            User teacher = authService.getCurrentUser();
            String accessToken = googleClassroomService.getValidAccessToken(teacher);
            googleClassroomService.syncClassroomToCourse(accessToken, request.getGoogleClassroomId(), courseId);
            course = courseRepository.findById(courseId).orElse(course);
        } else {
            course = courseRepository.save(course);
        }

        return ResponseEntity.ok(CourseDto.fromEntity(course));
    }

    @PostMapping("/{courseId}/unlink-google")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Unlink course from Google Classroom")
    public ResponseEntity<CourseDto> unlinkFromGoogleClassroom(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        course.setGoogleClassroomId(null);
        course.setAutoSyncEnabled(false);
        course.setLastSyncedAt(null);

        course = courseRepository.save(course);
        return ResponseEntity.ok(CourseDto.fromEntity(course));
    }

    @GetMapping("/{courseId}/grades/export/preview")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Preview grade export to Google Classroom")
    public ResponseEntity<GoogleClassroomService.GradeExportPreview> previewGradeExport(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(googleClassroomService.previewGradeExport(courseId));
    }

    @GetMapping("/{courseId}/grades/export/csv")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Download grades as CSV file")
    public ResponseEntity<byte[]> exportGradesCsv(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        String csv = gradeExportService.exportGradesToCsv(courseId);
        String filename = course.getName().replaceAll("[^a-zA-Z0-9._-]", "_") + "_grades.csv";

        // Add BOM for Excel UTF-8 compatibility
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] csvBytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(csvBytes, 0, result, bom.length, csvBytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(result);
    }

    @PostMapping("/{courseId}/grades/export")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Export grades to Google Classroom")
    public ResponseEntity<Void> exportGrades(@PathVariable Long courseId) {
        User teacher = authService.getCurrentUser();
        String accessToken = googleClassroomService.getValidAccessToken(teacher);

        googleClassroomService.exportGradesToClassroom(accessToken, courseId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/archive")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Archive a course")
    public ResponseEntity<CourseDto> archiveCourse(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        course.setIsActive(false);
        course = courseRepository.save(course);

        return ResponseEntity.ok(CourseDto.fromEntity(course));
    }

    @PostMapping("/{courseId}/unarchive")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Unarchive a course")
    public ResponseEntity<CourseDto> unarchiveCourse(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        course.setIsActive(true);
        course = courseRepository.save(course);

        return ResponseEntity.ok(CourseDto.fromEntity(course));
    }

    private String generateEnrollmentCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Request/Response DTOs
    @Data
    public static class CreateCourseRequest {
        @NotBlank(message = "Course name is required")
        private String name;
        private String description;
    }

    @Data
    public static class CourseWizardRequest {
        @NotBlank(message = "Course name is required")
        private String name;
        private String description;
        private String googleClassroomId;
        private Boolean autoSyncEnabled;
        private Boolean syncStudentsNow;
        private List<String> studentEmails;
    }

    @Data
    public static class UpdateCourseRequest {
        private String name;
        private String description;
        private Boolean isActive;
        private Boolean autoSyncEnabled;
    }

    @Data
    public static class BulkEnrollRequest {
        private List<String> emails;
    }

    public record BulkEnrollResponse(int added, int alreadyEnrolled) {}

    @Data
    public static class ApplySyncRequest {
        private List<String> emailsToAdd;
        private List<Long> idsToRemove;
    }

    @Data
    public static class LinkGoogleRequest {
        private String googleClassroomId;
        private Boolean autoSyncEnabled;
        private Boolean syncNow;
    }

    public record SyncStatusResponse(
            boolean isLinked,
            String googleClassroomId,
            LocalDateTime lastSyncedAt,
            Boolean autoSyncEnabled
    ) {}

    public record SyncResultResponse(
            boolean success,
            int added,
            int removed,
            String error
    ) {}

    public record CourseDto(
            Long id,
            String name,
            String description,
            String teacherName,
            int studentCount,
            int exerciseCount,
            Boolean isActive,
            String googleClassroomId,
            Boolean autoSyncEnabled,
            LocalDateTime lastSyncedAt,
            String enrollmentCode
    ) {
        public static CourseDto fromEntity(Course course) {
            return new CourseDto(
                    course.getId(),
                    course.getName(),
                    course.getDescription(),
                    course.getTeacher().getName(),
                    course.getStudents().size(),
                    course.getExercises().size(),
                    course.getIsActive(),
                    course.getGoogleClassroomId(),
                    course.getAutoSyncEnabled(),
                    course.getLastSyncedAt(),
                    course.getEnrollmentCode()
            );
        }
    }

    public record StudentDto(Long id, String name, String email, String googleId) {
        public static StudentDto fromEntity(User user) {
            return new StudentDto(user.getId(), user.getName(), user.getEmail(), user.getGoogleId());
        }
    }
}
