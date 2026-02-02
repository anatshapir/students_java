package com.javaedu.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "error_patterns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String pattern;

    @Column(name = "occurrence_count", nullable = false)
    @Builder.Default
    private Integer occurrenceCount = 1;

    @Column(name = "suggested_hint", columnDefinition = "TEXT")
    private String suggestedHint;

    @Column(name = "error_type")
    private String errorType;
}
