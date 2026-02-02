package com.javaedu.dto.exercise;

import com.javaedu.model.Exercise;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseDto {

    private Long id;
    private Long courseId;
    private String courseName;
    private String title;
    private String description;
    private String starterCode;
    private Exercise.Difficulty difficulty;
    private Integer points;
    private Exercise.Category category;
    private LocalDateTime dueDate;
    private Boolean isPublished;
    private LocalDateTime createdAt;
    private List<TestCaseDto> testCases;
    private List<HintDto> hints;
    private Integer totalTestCases;
    private Integer visibleTestCases;

    public static ExerciseDto fromEntity(Exercise exercise) {
        return ExerciseDto.builder()
                .id(exercise.getId())
                .courseId(exercise.getCourse().getId())
                .courseName(exercise.getCourse().getName())
                .title(exercise.getTitle())
                .description(exercise.getDescription())
                .starterCode(exercise.getStarterCode())
                .difficulty(exercise.getDifficulty())
                .points(exercise.getPoints())
                .category(exercise.getCategory())
                .dueDate(exercise.getDueDate())
                .isPublished(exercise.getIsPublished())
                .createdAt(exercise.getCreatedAt())
                .totalTestCases(exercise.getTestCases().size())
                .visibleTestCases((int) exercise.getTestCases().stream().filter(tc -> !tc.getIsHidden()).count())
                .build();
    }

    public static ExerciseDto fromEntityWithDetails(Exercise exercise) {
        ExerciseDto dto = fromEntity(exercise);
        dto.setTestCases(exercise.getTestCases().stream()
                .filter(tc -> !tc.getIsHidden())
                .map(TestCaseDto::fromEntity)
                .toList());
        dto.setHints(exercise.getHints().stream()
                .map(HintDto::fromEntity)
                .toList());
        return dto;
    }
}
