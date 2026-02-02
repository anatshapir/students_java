package com.javaedu.repository;

import com.javaedu.model.Hint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HintRepository extends JpaRepository<Hint, Long> {

    List<Hint> findByExerciseIdOrderByOrderNumAsc(Long exerciseId);

    int countByExerciseId(Long exerciseId);
}
