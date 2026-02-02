package com.javaedu.dto.exercise;

import com.javaedu.model.Hint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HintDto {

    private Long id;
    private Integer orderNum;
    private String content;
    private Boolean isAiGenerated;
    private Integer penaltyPercentage;

    public static HintDto fromEntity(Hint hint) {
        return HintDto.builder()
                .id(hint.getId())
                .orderNum(hint.getOrderNum())
                .content(hint.getContent())
                .isAiGenerated(hint.getIsAiGenerated())
                .penaltyPercentage(hint.getPenaltyPercentage())
                .build();
    }
}
