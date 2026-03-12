package com.javaedu.controller;

import com.javaedu.dto.submission.GradeDto;
import com.javaedu.model.Course;
import com.javaedu.model.Grade;
import com.javaedu.model.User;
import com.javaedu.repository.CourseRepository;
import com.javaedu.repository.GradeRepository;
import com.javaedu.repository.SubmissionRepository;
import com.javaedu.repository.UserRepository;
import com.javaedu.service.AuthService;
import com.javaedu.service.LearningEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard and analytics endpoints")
public class DashboardController {

    private final AuthService authService;
    private final CourseRepository courseRepository;
    private final SubmissionRepository submissionRepository;
    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;
    private final LearningEngineService learningEngineService;

    // --- Endpoints matching frontend API client ---

    @GetMapping("/{courseId}/stats")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get dashboard stats for a course")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        int totalStudents = course.getStudents().size();
        int totalExercises = course.getExercises().size();

        var recentPage = submissionRepository.findByCourseId(courseId, PageRequest.of(0, 5));
        List<SubmissionSummary> recentSubmissions = recentPage.getContent().stream()
                .map(s -> new SubmissionSummary(
                        s.getId(),
                        s.getExercise().getId(),
                        s.getExercise().getTitle(),
                        s.getUser().getId(),
                        s.getUser().getName(),
                        s.getCode(),
                        s.getStatus().name(),
                        s.getGrade() != null ? s.getGrade().getScore() : null,
                        s.getGrade() != null ? s.getGrade().getMaxScore() : null,
                        s.getSubmittedAt().toString()
                ))
                .toList();

        int totalSubmissions = (int) submissionRepository.findByCourseId(courseId, PageRequest.of(0, 1)).getTotalElements();

        // Compute completion rate
        int completedStudents = 0;
        if (totalExercises > 0 && totalStudents > 0) {
            for (var student : course.getStudents()) {
                var progress = learningEngineService.getStudentProgress(student.getId(), courseId);
                if (progress.completedExercises() == progress.totalExercises() && progress.totalExercises() > 0) {
                    completedStudents++;
                }
            }
        }
        double completionRate = totalStudents > 0 ? (double) completedStudents / totalStudents * 100 : 0;

        List<LearningEngineService.StrugglingStudent> struggling =
                learningEngineService.identifyStrugglingStudents(courseId);

        List<StrugglingStudentSummary> strugglingStudents = struggling.stream()
                .map(s -> new StrugglingStudentSummary(s.userId(), s.name(), s.email(),
                        s.incompleteExercises(), s.totalAttempts()))
                .toList();

        return ResponseEntity.ok(new DashboardStatsResponse(
                totalStudents, totalExercises, totalSubmissions, completionRate,
                recentSubmissions, strugglingStudents
        ));
    }

    @GetMapping("/teacher/overview")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get teacher dashboard overview")
    public ResponseEntity<TeacherDashboard> getTeacherDashboard() {
        User teacher = authService.getCurrentUser();
        List<Course> courses = courseRepository.findByTeacherId(teacher.getId());

        int totalStudents = courses.stream()
                .mapToInt(c -> c.getStudents().size())
                .sum();

        int totalExercises = courses.stream()
                .mapToInt(c -> c.getExercises().size())
                .sum();

        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        int recentSubmissions = submissionRepository
                .findBySubmittedAtBetween(weekAgo, LocalDateTime.now())
                .size();

        List<CourseOverview> courseOverviews = courses.stream()
                .map(c -> new CourseOverview(
                        c.getId(),
                        c.getName(),
                        c.getStudents().size(),
                        c.getExercises().size(),
                        (int) c.getExercises().stream().filter(e -> e.getIsPublished()).count()
                ))
                .toList();

        return ResponseEntity.ok(new TeacherDashboard(
                courses.size(),
                totalStudents,
                totalExercises,
                recentSubmissions,
                courseOverviews
        ));
    }

    @GetMapping("/teacher/course/{courseId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get detailed course analytics")
    public ResponseEntity<CourseAnalytics> getCourseAnalytics(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        List<LearningEngineService.ExerciseAnalytics> exerciseAnalytics = course.getExercises().stream()
                .map(e -> learningEngineService.getExerciseAnalytics(e.getId()))
                .toList();

        List<LearningEngineService.StrugglingStudent> strugglingStudents =
                learningEngineService.identifyStrugglingStudents(courseId);

        Map<String, Integer> submissionsByDay = new HashMap<>();
        LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
        submissionRepository.findBySubmittedAtBetween(weekAgo, LocalDateTime.now())
                .stream()
                .filter(s -> s.getExercise().getCourse().getId().equals(courseId))
                .forEach(s -> {
                    String day = s.getSubmittedAt().toLocalDate().toString();
                    submissionsByDay.merge(day, 1, Integer::sum);
                });

        return ResponseEntity.ok(new CourseAnalytics(
                courseId,
                course.getName(),
                course.getStudents().size(),
                exerciseAnalytics,
                strugglingStudents,
                submissionsByDay
        ));
    }

    @GetMapping("/teacher/exercise/{exerciseId}/analytics")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get exercise-specific analytics")
    public ResponseEntity<LearningEngineService.ExerciseAnalytics> getExerciseAnalytics(
            @PathVariable Long exerciseId) {
        return ResponseEntity.ok(learningEngineService.getExerciseAnalytics(exerciseId));
    }

    @GetMapping("/student/progress")
    @Operation(summary = "Get current student's progress")
    public ResponseEntity<StudentDashboard> getStudentDashboard() {
        User student = authService.getCurrentUser();
        List<Course> courses = courseRepository.findByStudentId(student.getId());

        List<Grade> grades = gradeRepository.findByUserId(student.getId());
        double averageGrade = grades.isEmpty() ? 0 :
                grades.stream()
                        .mapToDouble(Grade::getPercentage)
                        .average()
                        .orElse(0);

        int totalSubmissions = submissionRepository.findByUserId(student.getId()).size();

        List<CourseProgress> courseProgress = courses.stream()
                .map(c -> {
                    LearningEngineService.StudentProgress progress =
                            learningEngineService.getStudentProgress(student.getId(), c.getId());
                    return new CourseProgress(
                            c.getId(),
                            c.getName(),
                            progress.completedExercises(),
                            progress.totalExercises(),
                            progress.totalAttempts()
                    );
                })
                .toList();

        List<GradeDto> recentGrades = grades.stream()
                .sorted((a, b) -> b.getGradedAt().compareTo(a.getGradedAt()))
                .limit(10)
                .map(GradeDto::fromEntity)
                .toList();

        return ResponseEntity.ok(new StudentDashboard(
                courses.size(),
                totalSubmissions,
                averageGrade,
                courseProgress,
                recentGrades
        ));
    }

    @GetMapping("/student/course/{courseId}/progress")
    @Operation(summary = "Get student's progress in a specific course")
    public ResponseEntity<LearningEngineService.StudentProgress> getStudentCourseProgress(
            @PathVariable Long courseId) {
        User student = authService.getCurrentUser();
        return ResponseEntity.ok(learningEngineService.getStudentProgress(student.getId(), courseId));
    }

    public record TeacherDashboard(
            int totalCourses,
            int totalStudents,
            int totalExercises,
            int recentSubmissions,
            List<CourseOverview> courses
    ) {}

    public record CourseOverview(
            Long id,
            String name,
            int studentCount,
            int exerciseCount,
            int publishedExerciseCount
    ) {}

    public record CourseAnalytics(
            Long courseId,
            String courseName,
            int studentCount,
            List<LearningEngineService.ExerciseAnalytics> exerciseAnalytics,
            List<LearningEngineService.StrugglingStudent> strugglingStudents,
            Map<String, Integer> submissionsByDay
    ) {}

    public record StudentDashboard(
            int enrolledCourses,
            int totalSubmissions,
            double averageGrade,
            List<CourseProgress> courseProgress,
            List<GradeDto> recentGrades
    ) {}

    public record CourseProgress(
            Long courseId,
            String courseName,
            int completedExercises,
            int totalExercises,
            int totalAttempts
    ) {}

    // --- Response types matching frontend DashboardStats ---

    public record DashboardStatsResponse(
            int totalStudents,
            int totalExercises,
            int totalSubmissions,
            double averageCompletionRate,
            List<SubmissionSummary> recentSubmissions,
            List<StrugglingStudentSummary> strugglingStudents
    ) {}

    public record SubmissionSummary(
            Long id,
            Long exerciseId,
            String exerciseTitle,
            Long userId,
            String userName,
            String code,
            String status,
            Integer score,
            Integer maxScore,
            String submittedAt
    ) {}

    public record StrugglingStudentSummary(
            Long userId,
            String userName,
            String email,
            int incompleteExercises,
            int totalAttempts
    ) {}
}
