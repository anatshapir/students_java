package com.javaedu.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "order_num", nullable = false)
    private Integer orderNum;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "is_ai_generated")
    @Builder.Default
    private Boolean isAiGenerated = false;

    @Column(name = "penalty_percentage")
    @Builder.Default
    private Integer penaltyPercentage = 0;
}
