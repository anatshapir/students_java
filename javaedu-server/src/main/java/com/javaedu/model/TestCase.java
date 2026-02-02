package com.javaedu.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(nullable = false)
    private String name;

    @Column(name = "test_code", columnDefinition = "TEXT", nullable = false)
    private String testCode;

    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(name = "expected_output", columnDefinition = "TEXT")
    private String expectedOutput;

    @Column(name = "is_hidden")
    @Builder.Default
    private Boolean isHidden = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer points = 10;

    @Column(name = "order_num")
    @Builder.Default
    private Integer orderNum = 0;

    @Column(name = "timeout_seconds")
    @Builder.Default
    private Integer timeoutSeconds = 5;
}
