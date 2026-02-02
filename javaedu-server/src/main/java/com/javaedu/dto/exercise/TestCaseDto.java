package com.javaedu.dto.exercise;

import com.javaedu.model.TestCase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseDto {

    private Long id;
    private String name;
    private String testCode;
    private String input;
    private String expectedOutput;
    private Boolean isHidden;
    private Integer points;
    private Integer orderNum;
    private Integer timeoutSeconds;

    public static TestCaseDto fromEntity(TestCase testCase) {
        return TestCaseDto.builder()
                .id(testCase.getId())
                .name(testCase.getName())
                .testCode(testCase.getTestCode())
                .input(testCase.getInput())
                .expectedOutput(testCase.getExpectedOutput())
                .isHidden(testCase.getIsHidden())
                .points(testCase.getPoints())
                .orderNum(testCase.getOrderNum())
                .timeoutSeconds(testCase.getTimeoutSeconds())
                .build();
    }
}
