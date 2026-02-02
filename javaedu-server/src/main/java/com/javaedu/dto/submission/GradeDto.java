package com.javaedu.dto.submission;

import com.javaedu.model.Grade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeDto {

    private Long id;
    private Integer score;
    private Integer maxScore;
    private Double percentage;
    private String feedback;
    private LocalDateTime gradedAt;
    private Boolean isAutoGraded;
    private String gradedByName;

    public static GradeDto fromEntity(Grade grade) {
        return GradeDto.builder()
                .id(grade.getId())
                .score(grade.getScore())
                .maxScore(grade.getMaxScore())
                .percentage(grade.getPercentage())
                .feedback(grade.getFeedback())
                .gradedAt(grade.getGradedAt())
                .isAutoGraded(grade.getIsAutoGraded())
                .gradedByName(grade.getGradedBy() != null ? grade.getGradedBy().getName() : null)
                .build();
    }
}
