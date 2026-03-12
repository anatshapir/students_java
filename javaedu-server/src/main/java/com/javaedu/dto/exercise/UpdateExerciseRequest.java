package com.javaedu.dto.exercise;

import com.javaedu.model.Exercise;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateExerciseRequest {

    private String title;
    private String description;
    private String starterCode;
    private String solutionCode;
    private Exercise.Difficulty difficulty;

    @Positive(message = "Points must be positive")
    private Integer points;

    private Exercise.Category category;
    private LocalDateTime dueDate;
    private Boolean isPublished;
    private List<CreateExerciseRequest.TestCaseRequest> testCases;
    private List<CreateExerciseRequest.HintRequest> hints;
}
