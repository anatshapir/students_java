package com.javaedu.dto.submission;

import com.javaedu.model.TestResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResultDto {

    private Long id;
    private Long testCaseId;
    private String testCaseName;
    private Boolean passed;
    private String actualOutput;
    private String errorMessage;
    private Long executionTimeMs;
    private Boolean isHidden;

    public static TestResultDto fromEntity(TestResult testResult) {
        return TestResultDto.builder()
                .id(testResult.getId())
                .testCaseId(testResult.getTestCase().getId())
                .testCaseName(testResult.getTestCase().getName())
                .passed(testResult.getPassed())
                .actualOutput(testResult.getTestCase().getIsHidden() ? null : testResult.getActualOutput())
                .errorMessage(testResult.getTestCase().getIsHidden() ? null : testResult.getErrorMessage())
                .executionTimeMs(testResult.getExecutionTimeMs())
                .isHidden(testResult.getTestCase().getIsHidden())
                .build();
    }
}
