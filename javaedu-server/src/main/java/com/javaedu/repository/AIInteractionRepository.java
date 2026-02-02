package com.javaedu.repository;

import com.javaedu.model.AIInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AIInteractionRepository extends JpaRepository<AIInteraction, Long> {

    List<AIInteraction> findByUserId(Long userId);

    List<AIInteraction> findByUserIdAndExerciseId(Long userId, Long exerciseId);

    @Query("SELECT COUNT(ai) FROM AIInteraction ai WHERE ai.user.id = :userId AND ai.timestamp > :since")
    int countByUserIdSince(Long userId, LocalDateTime since);

    @Query("SELECT ai FROM AIInteraction ai WHERE ai.exercise.id = :exerciseId ORDER BY ai.timestamp DESC")
    List<AIInteraction> findByExerciseIdOrderByTimestampDesc(Long exerciseId);

    @Query("SELECT COUNT(ai) FROM AIInteraction ai WHERE ai.user.id = :userId AND ai.timestamp > :hourAgo")
    int countInteractionsInLastHour(Long userId, LocalDateTime hourAgo);
}
