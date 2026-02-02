package com.javaedu.service;

import com.javaedu.model.*;
import com.javaedu.repository.GradeRepository;
import com.javaedu.repository.StudentAnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradingServiceTest {

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private StudentAnalyticsRepository analyticsRepository;

    @InjectMocks
    private GradingService gradingService;

    private User student;
    private User teacher;
    private Course course;
    private Exercise exercise;
    private Submission submission;
    private List<TestCase> testCases;
    private List<TestResult> testResults;

    @BeforeEach
    void setUp() {
        teacher = User.builder()
                .id(1L)
                .name("Teacher")
                .email("teacher@test.com")
                .role(User.Role.TEACHER)
                .build();

        student = User.builder()
                .id(2L)
                .name("Student")
                .email("student@test.com")
                .role(User.Role.STUDENT)
                .build();

        course = Course.builder()
                .id(1L)
                .name("Java 101")
                .teacher(teacher)
                .build();

        testCases = new ArrayList<>();
        testCases.add(TestCase.builder()
                .id(1L)
                .name("Test 1")
                .points(30)
                .isHidden(false)
                .build());
        testCases.add(TestCase.builder()
                .id(2L)
                .name("Test 2")
                .points(40)
                .isHidden(false)
                .build());
        testCases.add(TestCase.builder()
                .id(3L)
                .name("Hidden Test")
                .points(30)
                .isHidden(true)
                .build());

        exercise = Exercise.builder()
                .id(1L)
                .title("Test Exercise")
                .course(course)
                .points(100)
                .testCases(testCases)
                .build();

        submission = Submission.builder()
                .id(1L)
                .exercise(exercise)
                .user(student)
                .code("public class Test {}")
                .status(Submission.Status.COMPLETED)
                .build();

        testResults = new ArrayList<>();
    }

    @Test
    void gradeSubmission_AllTestsPassed_ReturnsFullScore() {
        // All tests pass
        for (TestCase tc : testCases) {
            testResults.add(TestResult.builder()
                    .submission(submission)
                    .testCase(tc)
                    .passed(true)
                    .build());
        }
        submission.setTestResults(testResults);

        Grade savedGrade = Grade.builder()
                .id(1L)
                .submission(submission)
                .score(100)
                .maxScore(100)
                .feedback("Passed 3 of 3 tests.")
                .isAutoGraded(true)
                .build();

        when(gradeRepository.save(any(Grade.class))).thenReturn(savedGrade);
        when(analyticsRepository.findByUserIdAndExerciseId(any(), any())).thenReturn(Optional.empty());

        Grade result = gradingService.gradeSubmission(submission);

        assertNotNull(result);
        assertEquals(100, result.getScore());
        assertEquals(100, result.getMaxScore());
        assertTrue(result.getIsAutoGraded());
        verify(gradeRepository).save(any(Grade.class));
    }

    @Test
    void gradeSubmission_SomeTestsFailed_ReturnsPartialScore() {
        // First test passes, second fails, third passes
        testResults.add(TestResult.builder()
                .submission(submission)
                .testCase(testCases.get(0))
                .passed(true)
                .build());
        testResults.add(TestResult.builder()
                .submission(submission)
                .testCase(testCases.get(1))
                .passed(false)
                .errorMessage("Expected 5 but got 4")
                .build());
        testResults.add(TestResult.builder()
                .submission(submission)
                .testCase(testCases.get(2))
                .passed(true)
                .build());
        submission.setTestResults(testResults);

        when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> {
            Grade g = invocation.getArgument(0);
            g.setId(1L);
            return g;
        });
        // Note: analyticsRepository not stubbed because score 60% < 70% threshold

        Grade result = gradingService.gradeSubmission(submission);

        assertNotNull(result);
        assertEquals(60, result.getScore()); // 30 + 30 = 60 points
        assertEquals(100, result.getMaxScore());
    }

    @Test
    void gradeSubmission_AllTestsFailed_ReturnsZero() {
        for (TestCase tc : testCases) {
            testResults.add(TestResult.builder()
                    .submission(submission)
                    .testCase(tc)
                    .passed(false)
                    .errorMessage("Test failed")
                    .build());
        }
        submission.setTestResults(testResults);

        when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> {
            Grade g = invocation.getArgument(0);
            g.setId(1L);
            return g;
        });
        // Note: analyticsRepository not stubbed because score 0% < 70% threshold

        Grade result = gradingService.gradeSubmission(submission);

        assertNotNull(result);
        assertEquals(0, result.getScore());
    }

    @Test
    void gradeSubmission_FeedbackIncludesTestDetails() {
        testResults.add(TestResult.builder()
                .submission(submission)
                .testCase(testCases.get(0))
                .passed(true)
                .build());
        testResults.add(TestResult.builder()
                .submission(submission)
                .testCase(testCases.get(1))
                .passed(false)
                .errorMessage("Wrong answer")
                .build());
        submission.setTestResults(testResults);

        when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> {
            Grade g = invocation.getArgument(0);
            g.setId(1L);
            return g;
        });
        // Note: analyticsRepository not stubbed because score 30/70 = 43% < 70% threshold

        Grade result = gradingService.gradeSubmission(submission);

        assertNotNull(result.getFeedback());
        assertTrue(result.getFeedback().contains("Passed 1 of 2 tests"));
        assertTrue(result.getFeedback().contains("Test 1: PASSED"));
        assertTrue(result.getFeedback().contains("Test 2: FAILED"));
    }

    @Test
    void manualGrade_UpdatesExistingGrade() {
        Grade existingGrade = Grade.builder()
                .id(1L)
                .submission(submission)
                .score(50)
                .maxScore(100)
                .feedback("Auto graded")
                .isAutoGraded(true)
                .build();

        when(gradeRepository.findBySubmissionId(1L)).thenReturn(Optional.of(existingGrade));
        when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Grade result = gradingService.manualGrade(submission, 80, "Good effort!", teacher);

        assertEquals(80, result.getScore());
        assertEquals("Good effort!", result.getFeedback());
        assertFalse(result.getIsAutoGraded());
        assertEquals(teacher, result.getGradedBy());
    }

    @Test
    void manualGrade_CreatesNewGrade_WhenNoneExists() {
        submission.setTestResults(testResults);

        when(gradeRepository.findBySubmissionId(1L)).thenReturn(Optional.empty());
        when(gradeRepository.save(any(Grade.class))).thenAnswer(invocation -> {
            Grade g = invocation.getArgument(0);
            g.setId(1L);
            return g;
        });

        Grade result = gradingService.manualGrade(submission, 75, "Nice work", teacher);

        assertNotNull(result);
        assertEquals(75, result.getScore());
        assertEquals(100, result.getMaxScore());
        assertEquals("Nice work", result.getFeedback());
        assertFalse(result.getIsAutoGraded());
    }

    @Test
    void getAverageGradeForExercise_ReturnsAverage() {
        when(gradeRepository.findAverageScoreByExerciseId(1L)).thenReturn(85.5);

        Double average = gradingService.getAverageGradeForExercise(1L);

        assertEquals(85.5, average);
    }

    @Test
    void getGradesForStudent_ReturnsStudentGrades() {
        List<Grade> grades = List.of(
                Grade.builder().id(1L).score(90).maxScore(100).build(),
                Grade.builder().id(2L).score(85).maxScore(100).build()
        );

        when(gradeRepository.findByUserId(2L)).thenReturn(grades);

        List<Grade> result = gradingService.getGradesForStudent(2L);

        assertEquals(2, result.size());
    }

    @Test
    void getBestGrade_ReturnsBestGrade() {
        Grade bestGrade = Grade.builder()
                .id(1L)
                .score(95)
                .maxScore(100)
                .build();

        when(gradeRepository.findBestGradeByUserIdAndExerciseId(2L, 1L))
                .thenReturn(Optional.of(bestGrade));

        Grade result = gradingService.getBestGrade(2L, 1L);

        assertNotNull(result);
        assertEquals(95, result.getScore());
    }
}
