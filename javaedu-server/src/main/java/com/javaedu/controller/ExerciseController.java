package com.javaedu.controller;

import com.javaedu.dto.exercise.CreateExerciseRequest;
import com.javaedu.dto.exercise.ExerciseDto;
import com.javaedu.dto.exercise.HintDto;
import com.javaedu.dto.exercise.UpdateExerciseRequest;
import com.javaedu.model.Exercise;
import com.javaedu.service.AuthService;
import com.javaedu.service.ExerciseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
@Tag(name = "Exercises", description = "Exercise management endpoints")
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final AuthService authService;

    @GetMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get all exercises for the current teacher")
    public ResponseEntity<List<ExerciseDto>> getAllExercises() {
        var user = authService.getCurrentUser();
        return ResponseEntity.ok(exerciseService.getExercisesByTeacher(user.getId()));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get all exercises for a course")
    public ResponseEntity<List<ExerciseDto>> getExercisesByCourse(@PathVariable Long courseId) {
        var user = authService.getCurrentUser();
        List<ExerciseDto> exercises;
        if (user.getRole() == com.javaedu.model.User.Role.TEACHER ||
            user.getRole() == com.javaedu.model.User.Role.ADMIN) {
            exercises = exerciseService.getExercisesByCourse(courseId);
        } else {
            exercises = exerciseService.getPublishedExercisesByCourse(courseId);
        }
        return ResponseEntity.ok(exercises);
    }

    @GetMapping("/available")
    @Operation(summary = "Get all available exercises for current student")
    public ResponseEntity<List<ExerciseDto>> getAvailableExercises() {
        var user = authService.getCurrentUser();
        return ResponseEntity.ok(exerciseService.getAvailableExercisesForStudent(user.getId()));
    }

    @GetMapping("/{exerciseId}")
    @Operation(summary = "Get exercise by ID")
    public ResponseEntity<ExerciseDto> getExercise(@PathVariable Long exerciseId) {
        var user = authService.getCurrentUser();
        ExerciseDto exercise;
        if (user.getRole() == com.javaedu.model.User.Role.TEACHER ||
            user.getRole() == com.javaedu.model.User.Role.ADMIN) {
            exercise = exerciseService.getExerciseByIdForTeacher(exerciseId);
        } else {
            exercise = exerciseService.getExerciseById(exerciseId);
        }
        return ResponseEntity.ok(exercise);
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Create a new exercise")
    public ResponseEntity<ExerciseDto> createExercise(@Valid @RequestBody CreateExerciseRequest request) {
        return ResponseEntity.ok(exerciseService.createExercise(request));
    }

    @PutMapping("/{exerciseId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Update an exercise")
    public ResponseEntity<ExerciseDto> updateExercise(
            @PathVariable Long exerciseId,
            @RequestBody UpdateExerciseRequest request) {
        return ResponseEntity.ok(exerciseService.updateExercise(exerciseId, request));
    }

    @DeleteMapping("/{exerciseId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Delete an exercise")
    public ResponseEntity<Void> deleteExercise(@PathVariable Long exerciseId) {
        exerciseService.deleteExercise(exerciseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{exerciseId}/hints")
    @Operation(summary = "Get hints for an exercise (progressive reveal for students)")
    public ResponseEntity<Map<String, Object>> getHints(
            @PathVariable Long exerciseId,
            @RequestParam(defaultValue = "1") int upTo) {
        List<HintDto> hints = exerciseService.getHintsForStudent(exerciseId, upTo);
        int totalHints = exerciseService.getHintCount(exerciseId);
        return ResponseEntity.ok(Map.of(
                "hints", hints,
                "totalHints", totalHints,
                "hasMore", upTo < totalHints
        ));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get exercises by category")
    public ResponseEntity<List<ExerciseDto>> getExercisesByCategory(@PathVariable Exercise.Category category) {
        return ResponseEntity.ok(exerciseService.getExercisesByCategory(category));
    }

    @GetMapping("/difficulty/{difficulty}")
    @Operation(summary = "Get exercises by difficulty")
    public ResponseEntity<List<ExerciseDto>> getExercisesByDifficulty(@PathVariable Exercise.Difficulty difficulty) {
        return ResponseEntity.ok(exerciseService.getExercisesByDifficulty(difficulty));
    }
}
