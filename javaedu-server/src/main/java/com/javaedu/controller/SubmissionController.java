package com.javaedu.controller;

import com.javaedu.dto.submission.SubmissionDto;
import com.javaedu.dto.submission.SubmitCodeRequest;
import com.javaedu.service.AuthService;
import com.javaedu.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Tag(name = "Submissions", description = "Code submission endpoints")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final AuthService authService;

    @PostMapping
    @Operation(summary = "Submit code for an exercise")
    public ResponseEntity<SubmissionDto> submitCode(@Valid @RequestBody SubmitCodeRequest request) {
        var user = authService.getCurrentUser();
        return ResponseEntity.ok(submissionService.submitCode(user.getId(), request));
    }

    @GetMapping("/{submissionId}")
    @Operation(summary = "Get submission by ID")
    public ResponseEntity<SubmissionDto> getSubmission(@PathVariable Long submissionId) {
        return ResponseEntity.ok(submissionService.getSubmissionById(submissionId));
    }

    @GetMapping("/my")
    @Operation(summary = "Get current user's submissions")
    public ResponseEntity<List<SubmissionDto>> getMySubmissions() {
        var user = authService.getCurrentUser();
        return ResponseEntity.ok(submissionService.getSubmissionsByUser(user.getId()));
    }

    @GetMapping("/my/exercise/{exerciseId}")
    @Operation(summary = "Get current user's submissions for a specific exercise")
    public ResponseEntity<List<SubmissionDto>> getMySubmissionsForExercise(@PathVariable Long exerciseId) {
        var user = authService.getCurrentUser();
        return ResponseEntity.ok(submissionService.getSubmissionsByUserAndExercise(user.getId(), exerciseId));
    }

    @GetMapping("/my/exercise/{exerciseId}/latest")
    @Operation(summary = "Get current user's latest submission for an exercise")
    public ResponseEntity<SubmissionDto> getMyLatestSubmission(@PathVariable Long exerciseId) {
        var user = authService.getCurrentUser();
        SubmissionDto submission = submissionService.getLatestSubmission(user.getId(), exerciseId);
        if (submission == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(submission);
    }

    @GetMapping("/exercise/{exerciseId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get all submissions for an exercise (teacher only)")
    public ResponseEntity<List<SubmissionDto>> getSubmissionsForExercise(@PathVariable Long exerciseId) {
        return ResponseEntity.ok(submissionService.getSubmissionsByExercise(exerciseId));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get all submissions for a course (teacher only)")
    public ResponseEntity<Page<SubmissionDto>> getSubmissionsForCourse(
            @PathVariable Long courseId,
            Pageable pageable) {
        return ResponseEntity.ok(submissionService.getSubmissionsByCourse(courseId, pageable));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get all submissions for a user (teacher only)")
    public ResponseEntity<List<SubmissionDto>> getSubmissionsForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(submissionService.getSubmissionsByUser(userId));
    }

    @GetMapping("/my/exercise/{exerciseId}/count")
    @Operation(summary = "Get submission count for current user on an exercise")
    public ResponseEntity<Integer> getSubmissionCount(@PathVariable Long exerciseId) {
        var user = authService.getCurrentUser();
        return ResponseEntity.ok(submissionService.getSubmissionCount(user.getId(), exerciseId));
    }
}
