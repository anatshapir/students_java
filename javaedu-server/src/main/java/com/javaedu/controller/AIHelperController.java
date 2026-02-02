package com.javaedu.controller;

import com.javaedu.dto.ai.GenerateExerciseRequest;
import com.javaedu.dto.ai.GeneratedExerciseResponse;
import com.javaedu.service.AIHelperService;
import com.javaedu.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Helper", description = "AI-powered tutoring assistance")
public class AIHelperController {

    private final AIHelperService aiHelperService;
    private final AuthService authService;

    @PostMapping("/ask")
    @Operation(summary = "Ask the AI tutor a question")
    public ResponseEntity<AIHelperService.AIResponse> askQuestion(@Valid @RequestBody AskQuestionRequest request) {
        var user = authService.getCurrentUser();
        return ResponseEntity.ok(aiHelperService.askQuestion(
                user,
                request.getExerciseId(),
                request.getQuestion(),
                request.getCurrentCode()
        ));
    }

    @PostMapping("/feedback/{interactionId}")
    @Operation(summary = "Provide feedback on an AI response")
    public ResponseEntity<Void> provideFeedback(
            @PathVariable Long interactionId,
            @RequestParam boolean helpful) {
        aiHelperService.markInteractionHelpful(interactionId, helpful);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/generate-exercise")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Generate a complete exercise using AI",
               description = "Uses AI to generate an exercise with title, description, starter code, solution, and test cases from a text prompt or image")
    public ResponseEntity<GeneratedExerciseResponse> generateExercise(
            @Valid @RequestBody GenerateExerciseRequest request) {
        var user = authService.getCurrentUser();
        return ResponseEntity.ok(aiHelperService.generateExercise(user, request));
    }

    @Data
    public static class AskQuestionRequest {
        private Long exerciseId;

        @NotBlank(message = "Question is required")
        private String question;

        private String currentCode;
    }
}
