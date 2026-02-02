package com.javaedu.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "student_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "time_spent_minutes")
    @Builder.Default
    private Integer timeSpentMinutes = 0;

    @Column(name = "hints_used")
    @Builder.Default
    private Integer hintsUsed = 0;

    @Column(name = "ai_interactions_count")
    @Builder.Default
    private Integer aiInteractionsCount = 0;

    @Column(name = "first_submission_at")
    private java.time.LocalDateTime firstSubmissionAt;

    @Column(name = "completed_at")
    private java.time.LocalDateTime completedAt;
}
