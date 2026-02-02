package com.javaedu.repository;

import com.javaedu.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByExerciseId(Long exerciseId);

    List<TestCase> findByExerciseIdAndIsHiddenFalse(Long exerciseId);

    List<TestCase> findByExerciseIdOrderByOrderNumAsc(Long exerciseId);

    int countByExerciseId(Long exerciseId);
}
