package com.javaedu.dto.submission;

import com.javaedu.model.Submission;
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
public class SubmissionDto {

    private Long id;
    private Long exerciseId;
    private String exerciseTitle;
    private Long userId;
    private String userName;
    private String code;
    private LocalDateTime submittedAt;
    private Submission.Status status;
    private Long executionTimeMs;
    private String compilerOutput;
    private List<TestResultDto> testResults;
    private GradeDto grade;
    private Integer passedTests;
    private Integer totalTests;

    public static SubmissionDto fromEntity(Submission submission) {
        return SubmissionDto.builder()
                .id(submission.getId())
                .exerciseId(submission.getExercise().getId())
                .exerciseTitle(submission.getExercise().getTitle())
                .userId(submission.getUser().getId())
                .userName(submission.getUser().getName())
                .code(submission.getCode())
                .submittedAt(submission.getSubmittedAt())
                .status(submission.getStatus())
                .executionTimeMs(submission.getExecutionTimeMs())
                .compilerOutput(submission.getCompilerOutput())
                .testResults(submission.getTestResults().stream()
                        .map(TestResultDto::fromEntity)
                        .toList())
                .grade(submission.getGrade() != null ? GradeDto.fromEntity(submission.getGrade()) : null)
                .passedTests((int) submission.getTestResults().stream().filter(tr -> tr.getPassed()).count())
                .totalTests(submission.getTestResults().size())
                .build();
    }
}
