package com.javaedu.controller;

import com.javaedu.service.LearningEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Analytics endpoints")
public class AnalyticsController {

    private final LearningEngineService learningEngineService;

    @GetMapping("/exercises/{exerciseId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get analytics for a specific exercise")
    public ResponseEntity<ExerciseAnalyticsResponse> getExerciseAnalytics(
            @PathVariable Long exerciseId) {
        var analytics = learningEngineService.getExerciseAnalytics(exerciseId);

        List<CommonErrorResponse> commonErrors = analytics.commonErrors().stream()
                .map(e -> new CommonErrorResponse(e.pattern(), e.count(), e.suggestedHint()))
                .toList();

        return ResponseEntity.ok(new ExerciseAnalyticsResponse(
                analytics.exerciseId(),
                analytics.exerciseTitle(),
                analytics.totalSubmissions(),
                analytics.uniqueStudents(),
                analytics.completedStudents(),
                analytics.averageAttempts(),
                analytics.averageTimeToComplete(),
                commonErrors
        ));
    }

    @GetMapping("/courses/{courseId}/struggling")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get struggling students for a course")
    public ResponseEntity<List<LearningEngineService.StrugglingStudent>> getStrugglingStudents(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(learningEngineService.identifyStrugglingStudents(courseId));
    }

    public record ExerciseAnalyticsResponse(
            Long exerciseId,
            String exerciseTitle,
            int totalSubmissions,
            int uniqueStudents,
            int completedStudents,
            double averageAttempts,
            double averageTimeToComplete,
            List<CommonErrorResponse> commonErrors
    ) {}

    // Matches frontend CommonError type: occurrenceCount (not count)
    public record CommonErrorResponse(
            String pattern,
            int occurrenceCount,
            String suggestedHint
    ) {}
}
