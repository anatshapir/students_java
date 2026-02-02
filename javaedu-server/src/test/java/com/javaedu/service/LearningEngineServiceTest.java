package com.javaedu.service;

import com.javaedu.model.*;
import com.javaedu.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearningEngineServiceTest {

    @Mock
    private ErrorPatternRepository errorPatternRepository;

    @Mock
    private StudentAnalyticsRepository analyticsRepository;

    @Mock
    private TestResultRepository testResultRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private HintRepository hintRepository;

    @InjectMocks
    private LearningEngineService learningEngineService;

    private User student;
    private Course course;
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        User teacher = User.builder()
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

        exercise = Exercise.builder()
                .id(1L)
                .title("Hello World")
                .description("Print Hello World")
                .course(course)
                .points(100)
                .hints(new ArrayList<>())
                .build();
    }

    @Test
    void analyzeSubmission_WithFailedTests_CategorizeErrors() {
        TestCase testCase = TestCase.builder()
                .id(1L)
                .name("Test 1")
                .exercise(exercise)
                .build();

        TestResult failedResult = TestResult.builder()
                .id(1L)
                .testCase(testCase)
                .passed(false)
                .errorMessage("NullPointerException at line 5")
                .build();

        Submission submission = Submission.builder()
                .id(1L)
                .exercise(exercise)
                .user(student)
                .status(Submission.Status.COMPLETED)
                .testResults(List.of(failedResult))
                .build();

        when(errorPatternRepository.findByExerciseIdAndPattern(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(errorPatternRepository.save(any(ErrorPattern.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        learningEngineService.analyzeSubmission(submission);

        verify(errorPatternRepository).save(any(ErrorPattern.class));
    }

    @Test
    void analyzeSubmission_WithPassedTests_DoesNotCategorize() {
        TestCase testCase = TestCase.builder()
                .id(1L)
                .name("Test 1")
                .exercise(exercise)
                .build();

        TestResult passedResult = TestResult.builder()
                .id(1L)
                .testCase(testCase)
                .passed(true)
                .build();

        Submission submission = Submission.builder()
                .id(1L)
                .exercise(exercise)
                .user(student)
                .status(Submission.Status.COMPLETED)
                .testResults(List.of(passedResult))
                .build();

        learningEngineService.analyzeSubmission(submission);

        verify(errorPatternRepository, never()).save(any(ErrorPattern.class));
    }

    @Test
    void analyzeSubmission_PendingStatus_DoesNothing() {
        Submission submission = Submission.builder()
                .id(1L)
                .exercise(exercise)
                .user(student)
                .status(Submission.Status.PENDING)
                .testResults(new ArrayList<>())
                .build();

        learningEngineService.analyzeSubmission(submission);

        verify(errorPatternRepository, never()).save(any(ErrorPattern.class));
    }

    @Test
    void analyzeSubmission_ArrayIndexError_Categorized() {
        TestCase testCase = TestCase.builder()
                .id(1L)
                .name("Test 1")
                .exercise(exercise)
                .build();

        TestResult failedResult = TestResult.builder()
                .id(1L)
                .testCase(testCase)
                .passed(false)
                .errorMessage("ArrayIndexOutOfBoundsException: Index 5 out of bounds for length 3")
                .build();

        Submission submission = Submission.builder()
                .id(1L)
                .exercise(exercise)
                .user(student)
                .status(Submission.Status.COMPLETED)
                .testResults(List.of(failedResult))
                .build();

        when(errorPatternRepository.findByExerciseIdAndPattern(anyLong(), eq("ArrayIndexOutOfBoundsException")))
                .thenReturn(Optional.empty());
        when(errorPatternRepository.save(any(ErrorPattern.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        learningEngineService.analyzeSubmission(submission);

        verify(errorPatternRepository).save(argThat(pattern ->
                pattern.getPattern().equals("ArrayIndexOutOfBoundsException")));
    }

    @Test
    void analyzeSubmission_ExistingPattern_IncrementsCount() {
        TestCase testCase = TestCase.builder()
                .id(1L)
                .name("Test 1")
                .exercise(exercise)
                .build();

        TestResult failedResult = TestResult.builder()
                .id(1L)
                .testCase(testCase)
                .passed(false)
                .errorMessage("NullPointerException")
                .build();

        Submission submission = Submission.builder()
                .id(1L)
                .exercise(exercise)
                .user(student)
                .status(Submission.Status.COMPLETED)
                .testResults(List.of(failedResult))
                .build();

        ErrorPattern existingPattern = ErrorPattern.builder()
                .id(1L)
                .exercise(exercise)
                .pattern("NullPointerException")
                .occurrenceCount(5)
                .build();

        when(errorPatternRepository.findByExerciseIdAndPattern(anyLong(), eq("NullPointerException")))
                .thenReturn(Optional.of(existingPattern));
        when(errorPatternRepository.save(any(ErrorPattern.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        learningEngineService.analyzeSubmission(submission);

        assertEquals(6, existingPattern.getOccurrenceCount());
        verify(errorPatternRepository).save(existingPattern);
    }

    @Test
    void getExerciseAnalytics_ReturnsAnalytics() {
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        when(submissionRepository.findByExerciseId(1L)).thenReturn(List.of(
                Submission.builder().id(1L).build(),
                Submission.builder().id(2L).build()
        ));
        when(submissionRepository.countDistinctUsersByExerciseId(1L)).thenReturn(2);
        when(analyticsRepository.countCompletedByExerciseId(1L)).thenReturn(1);
        when(analyticsRepository.findAverageAttemptsByExerciseId(1L)).thenReturn(3.5);
        when(analyticsRepository.findAverageTimeToCompleteByExerciseId(1L)).thenReturn(25.0);
        when(errorPatternRepository.findByExerciseIdOrderByOccurrenceCountDesc(1L))
                .thenReturn(List.of(
                        ErrorPattern.builder()
                                .pattern("NullPointerException")
                                .occurrenceCount(10)
                                .suggestedHint("Check for null values")
                                .build()
                ));

        LearningEngineService.ExerciseAnalytics result = learningEngineService.getExerciseAnalytics(1L);

        assertNotNull(result);
        assertEquals(1L, result.exerciseId());
        assertEquals("Hello World", result.exerciseTitle());
        assertEquals(2, result.totalSubmissions());
        assertEquals(2, result.uniqueStudents());
        assertEquals(1, result.completedStudents());
        assertEquals(3.5, result.averageAttempts());
        assertEquals(25.0, result.averageTimeToComplete());
        assertEquals(1, result.commonErrors().size());
        assertEquals("NullPointerException", result.commonErrors().get(0).pattern());
    }

    @Test
    void getExerciseAnalytics_ExerciseNotFound_ThrowsException() {
        when(exerciseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                learningEngineService.getExerciseAnalytics(999L));
    }

    @Test
    void getExerciseAnalytics_NullAverages_ReturnsZero() {
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(exercise));
        when(submissionRepository.findByExerciseId(1L)).thenReturn(List.of());
        when(submissionRepository.countDistinctUsersByExerciseId(1L)).thenReturn(0);
        when(analyticsRepository.countCompletedByExerciseId(1L)).thenReturn(0);
        when(analyticsRepository.findAverageAttemptsByExerciseId(1L)).thenReturn(null);
        when(analyticsRepository.findAverageTimeToCompleteByExerciseId(1L)).thenReturn(null);
        when(errorPatternRepository.findByExerciseIdOrderByOccurrenceCountDesc(1L))
                .thenReturn(List.of());

        LearningEngineService.ExerciseAnalytics result = learningEngineService.getExerciseAnalytics(1L);

        assertEquals(0, result.averageAttempts());
        assertEquals(0, result.averageTimeToComplete());
    }

    @Test
    void getStudentProgress_ReturnsProgress() {
        StudentAnalytics analytics1 = StudentAnalytics.builder()
                .id(1L)
                .user(student)
                .exercise(exercise)
                .attempts(3)
                .timeSpentMinutes(30)
                .hintsUsed(1)
                .completedAt(LocalDateTime.now())
                .build();

        Exercise exercise2 = Exercise.builder()
                .id(2L)
                .title("Arrays")
                .course(course)
                .build();

        StudentAnalytics analytics2 = StudentAnalytics.builder()
                .id(2L)
                .user(student)
                .exercise(exercise2)
                .attempts(5)
                .timeSpentMinutes(45)
                .hintsUsed(2)
                .completedAt(null)
                .build();

        when(analyticsRepository.findByUserIdAndCourseId(2L, 1L))
                .thenReturn(List.of(analytics1, analytics2));

        LearningEngineService.StudentProgress result = learningEngineService.getStudentProgress(2L, 1L);

        assertNotNull(result);
        assertEquals(2L, result.userId());
        assertEquals(1L, result.courseId());
        assertEquals(2, result.totalExercises());
        assertEquals(1, result.completedExercises());
        assertEquals(8, result.totalAttempts());
        assertEquals(75, result.totalTimeSpentMinutes());
        assertEquals(3, result.totalHintsUsed());
        assertEquals(2, result.exercises().size());
    }

    @Test
    void identifyStrugglingStudents_IdentifiesCorrectly() {
        StudentAnalytics struggling1 = StudentAnalytics.builder()
                .id(1L)
                .user(student)
                .exercise(exercise)
                .attempts(10)
                .completedAt(null)
                .build();

        Exercise exercise2 = Exercise.builder()
                .id(2L)
                .title("Exercise 2")
                .course(course)
                .build();

        StudentAnalytics struggling2 = StudentAnalytics.builder()
                .id(2L)
                .user(student)
                .exercise(exercise2)
                .attempts(8)
                .completedAt(null)
                .build();

        when(analyticsRepository.findAll()).thenReturn(List.of(struggling1, struggling2));

        List<LearningEngineService.StrugglingStudent> result =
                learningEngineService.identifyStrugglingStudents(1L);

        assertEquals(1, result.size());
        assertEquals(student.getId(), result.get(0).userId());
        assertEquals(2, result.get(0).incompleteExercises());
        assertEquals(18, result.get(0).totalAttempts());
    }

    @Test
    void identifyStrugglingStudents_NoStrugglingStudents_ReturnsEmpty() {
        StudentAnalytics completed = StudentAnalytics.builder()
                .id(1L)
                .user(student)
                .exercise(exercise)
                .attempts(2)
                .completedAt(LocalDateTime.now())
                .build();

        when(analyticsRepository.findAll()).thenReturn(List.of(completed));

        List<LearningEngineService.StrugglingStudent> result =
                learningEngineService.identifyStrugglingStudents(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void generateHintSuggestions_CreatesHints() {
        ErrorPattern frequentError = ErrorPattern.builder()
                .id(1L)
                .exercise(exercise)
                .pattern("NullPointerException")
                .occurrenceCount(10)
                .suggestedHint("Initialize your variables")
                .build();

        when(exerciseRepository.findAll()).thenReturn(List.of(exercise));
        when(errorPatternRepository.findFrequentPatterns(1L, 5))
                .thenReturn(List.of(frequentError));
        when(hintRepository.save(any(Hint.class))).thenAnswer(inv -> inv.getArgument(0));

        learningEngineService.generateHintSuggestions();

        verify(hintRepository).save(argThat(hint ->
                hint.getContent().contains("Initialize your variables") &&
                hint.getIsAiGenerated()));
    }

    @Test
    void generateHintSuggestions_ExistingHint_SkipsCreation() {
        Hint existingHint = Hint.builder()
                .id(1L)
                .content("Fix NullPointerException by checking values")
                .build();
        exercise.getHints().add(existingHint);

        ErrorPattern frequentError = ErrorPattern.builder()
                .id(1L)
                .exercise(exercise)
                .pattern("NullPointerException")
                .occurrenceCount(10)
                .suggestedHint("Check for null")
                .build();

        when(exerciseRepository.findAll()).thenReturn(List.of(exercise));
        when(errorPatternRepository.findFrequentPatterns(1L, 5))
                .thenReturn(List.of(frequentError));

        learningEngineService.generateHintSuggestions();

        verify(hintRepository, never()).save(any(Hint.class));
    }
}
