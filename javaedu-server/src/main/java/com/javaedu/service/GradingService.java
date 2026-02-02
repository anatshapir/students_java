package com.javaedu.service;

import com.javaedu.model.*;
import com.javaedu.repository.GradeRepository;
import com.javaedu.repository.StudentAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradingService {

    private final GradeRepository gradeRepository;
    private final StudentAnalyticsRepository analyticsRepository;

    @Transactional
    public Grade gradeSubmission(Submission submission) {
        List<TestResult> testResults = submission.getTestResults();
        Exercise exercise = submission.getExercise();

        int totalPoints = 0;
        int earnedPoints = 0;

        for (TestResult result : testResults) {
            int testPoints = result.getTestCase().getPoints();
            totalPoints += testPoints;
            if (result.getPassed()) {
                earnedPoints += testPoints;
            }
        }

        if (totalPoints == 0) {
            totalPoints = exercise.getPoints();
        }

        StringBuilder feedback = new StringBuilder();
        feedback.append(String.format("Passed %d of %d tests.%n",
                testResults.stream().filter(TestResult::getPassed).count(),
                testResults.size()));

        for (TestResult result : testResults) {
            if (!result.getTestCase().getIsHidden()) {
                feedback.append(String.format("%n- %s: %s",
                        result.getTestCase().getName(),
                        result.getPassed() ? "PASSED" : "FAILED"));
                if (!result.getPassed() && result.getErrorMessage() != null) {
                    feedback.append(String.format(" (%s)", result.getErrorMessage()));
                }
            }
        }

        Grade grade = Grade.builder()
                .submission(submission)
                .score(earnedPoints)
                .maxScore(totalPoints)
                .feedback(feedback.toString())
                .isAutoGraded(true)
                .build();

        grade = gradeRepository.save(grade);
        submission.setGrade(grade);

        if (grade.getPercentage() >= 70) {
            markExerciseCompleted(submission.getUser(), exercise);
        }

        log.info("Graded submission {}: {}/{} ({}%)",
                submission.getId(), earnedPoints, totalPoints, grade.getPercentage());

        return grade;
    }

    private void markExerciseCompleted(User user, Exercise exercise) {
        analyticsRepository.findByUserIdAndExerciseId(user.getId(), exercise.getId())
                .ifPresent(analytics -> {
                    if (analytics.getCompletedAt() == null) {
                        analytics.setCompletedAt(LocalDateTime.now());
                        analyticsRepository.save(analytics);
                        log.info("Marked exercise {} as completed for user {}", exercise.getTitle(), user.getEmail());
                    }
                });
    }

    @Transactional
    public Grade manualGrade(Submission submission, int score, String feedback, User gradedBy) {
        Grade existingGrade = gradeRepository.findBySubmissionId(submission.getId()).orElse(null);

        if (existingGrade != null) {
            existingGrade.setScore(score);
            existingGrade.setFeedback(feedback);
            existingGrade.setIsAutoGraded(false);
            existingGrade.setGradedBy(gradedBy);
            return gradeRepository.save(existingGrade);
        }

        Grade grade = Grade.builder()
                .submission(submission)
                .score(score)
                .maxScore(submission.getExercise().getPoints())
                .feedback(feedback)
                .isAutoGraded(false)
                .gradedBy(gradedBy)
                .build();

        return gradeRepository.save(grade);
    }

    @Transactional(readOnly = true)
    public Double getAverageGradeForExercise(Long exerciseId) {
        return gradeRepository.findAverageScoreByExerciseId(exerciseId);
    }

    @Transactional(readOnly = true)
    public List<Grade> getGradesForStudent(Long userId) {
        return gradeRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Grade getBestGrade(Long userId, Long exerciseId) {
        return gradeRepository.findBestGradeByUserIdAndExerciseId(userId, exerciseId).orElse(null);
    }
}
