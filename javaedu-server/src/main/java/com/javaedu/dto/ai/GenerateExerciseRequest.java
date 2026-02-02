package com.javaedu.dto.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateExerciseRequest {

    @NotBlank(message = "Prompt is required")
    private String prompt;

    private String image;  // Base64-encoded image

    private String imageMediaType;  // "image/png", "image/jpeg", etc.

    private String difficulty;  // EASY | MEDIUM | HARD

    private String category;

    @Min(value = 1, message = "Number of test cases must be at least 1")
    @Max(value = 10, message = "Number of test cases cannot exceed 10")
    @Builder.Default
    private Integer numberOfTestCases = 4;
}
