package com.javaedu.dto.exercise;

import com.javaedu.model.Exercise;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateExerciseRequest {

    @NotNull(message = "Course ID is required")
    private Long courseId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private String starterCode;
    private String solutionCode;

    @NotNull(message = "Difficulty is required")
    private Exercise.Difficulty difficulty;

    @Positive(message = "Points must be positive")
    private Integer points = 100;

    private Exercise.Category category;
    private LocalDateTime dueDate;
    private Boolean isPublished = false;
    private List<TestCaseRequest> testCases;
    private List<HintRequest> hints;

    @Data
    public static class TestCaseRequest {
        @NotBlank(message = "Test case name is required")
        private String name;

        @NotBlank(message = "Test code is required")
        private String testCode;

        private String input;
        private String expectedOutput;
        private Boolean isHidden = false;
        private Integer points = 10;
        private Integer orderNum = 0;
        private Integer timeoutSeconds = 5;
    }

    @Data
    public static class HintRequest {
        @NotBlank(message = "Hint content is required")
        private String content;

        private Integer orderNum = 0;
        private Integer penaltyPercentage = 0;
    }
}
