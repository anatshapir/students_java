package com.javaedu.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedExerciseResponse {

    private String title;
    private String description;
    private String starterCode;
    private String solutionCode;
    private String difficulty;
    private String category;
    private Integer points;
    private List<GeneratedTestCase> testCases;
    private List<String> hints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratedTestCase {
        private String name;
        private String testCode;
        private Boolean isHidden;
        private Integer points;
        private String description;
    }
}
