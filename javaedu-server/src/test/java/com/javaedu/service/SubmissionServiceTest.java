package com.javaedu.service;

import com.javaedu.dto.submission.SubmissionDto;
import com.javaedu.dto.submission.SubmitCodeRequest;
import com.javaedu.exception.BadRequestException;
import com.javaedu.exception.ResourceNotFoundException;
import com.javaedu.model.*;
import com.javaedu.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentAnalyticsRepository analyticsRepository;

    @Mock
    private TestRunnerService testRunnerService;

    @Mock
    private GradingService gradingService;

    @InjectMocks
    private SubmissionService submissionService;

    private User testUser;
    private Exercise testExercise;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test Student")
                .email("student@test.com")
                .role(User.Role.STUDENT)
                .build();

        User teacher = User.builder()
                .id(2L)
                .name("Test Teacher")
                .email("teacher@test.com")
                .role(User.Role.TEACHER)
                .build();

        testCourse = Course.builder()
                .id(1L)
                .name("Java 101")
                .teacher(teacher)
                .build();

        testExercise = Exercise.builder()
                .id(1L)
                .title("Hello World")
                .description("Print Hello World")
                .course(testCourse)
                .isPublished(true)
                .points(100)
                .testCases(new ArrayList<>())
                .build();
    }

    @Test
    void submitCode_WithValidRequest_CreatesSubmission() {
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setExerciseId(1L);
        request.setCode("public class HelloWorld { }");

        Submission savedSubmission = Submission.builder()
                .id(1L)
                .exercise(testExercise)
                .user(testUser)
                .code(request.getCode())
                .status(Submission.Status.PENDING)
                .testResults(new ArrayList<>())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(testExercise));
        when(submissionRepository.save(any(Submission.class))).thenReturn(savedSubmission);
        when(analyticsRepository.findByUserIdAndExerciseId(1L, 1L)).thenReturn(Optional.empty());
        when(analyticsRepository.save(any(StudentAnalytics.class))).thenReturn(null);

        SubmissionDto result = submissionService.submitCode(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(Submission.Status.PENDING, result.getStatus());
        verify(submissionRepository).save(any(Submission.class));
    }

    @Test
    void submitCode_WithNonExistentUser_ThrowsException() {
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setExerciseId(1L);
        request.setCode("public class Test { }");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            submissionService.submitCode(999L, request));
    }

    @Test
    void submitCode_WithNonExistentExercise_ThrowsException() {
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setExerciseId(999L);
        request.setCode("public class Test { }");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(exerciseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            submissionService.submitCode(1L, request));
    }

    @Test
    void submitCode_WithUnpublishedExercise_ThrowsException() {
        testExercise.setIsPublished(false);

        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setExerciseId(1L);
        request.setCode("public class Test { }");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(testExercise));

        assertThrows(BadRequestException.class, () ->
            submissionService.submitCode(1L, request));
    }

    @Test
    void submitCode_WithExpiredDeadline_ThrowsException() {
        testExercise.setDueDate(LocalDateTime.now().minusDays(1));

        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setExerciseId(1L);
        request.setCode("public class Test { }");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(testExercise));

        assertThrows(BadRequestException.class, () ->
            submissionService.submitCode(1L, request));
    }

    @Test
    void submitCode_WithFutureDeadline_Succeeds() {
        testExercise.setDueDate(LocalDateTime.now().plusDays(7));

        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setExerciseId(1L);
        request.setCode("public class Test { }");

        Submission savedSubmission = Submission.builder()
                .id(1L)
                .exercise(testExercise)
                .user(testUser)
                .code(request.getCode())
                .status(Submission.Status.PENDING)
                .testResults(new ArrayList<>())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(testExercise));
        when(submissionRepository.save(any(Submission.class))).thenReturn(savedSubmission);
        when(analyticsRepository.findByUserIdAndExerciseId(1L, 1L)).thenReturn(Optional.empty());

        SubmissionDto result = submissionService.submitCode(1L, request);

        assertNotNull(result);
    }

    @Test
    void getSubmissionById_WithValidId_ReturnsSubmission() {
        Submission submission = Submission.builder()
                .id(1L)
                .exercise(testExercise)
                .user(testUser)
                .code("public class Test { }")
                .status(Submission.Status.COMPLETED)
                .testResults(new ArrayList<>())
                .build();

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));

        SubmissionDto result = submissionService.getSubmissionById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getSubmissionById_WithInvalidId_ThrowsException() {
        when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            submissionService.getSubmissionById(999L));
    }

    @Test
    void getSubmissionsByUser_ReturnsUserSubmissions() {
        List<Submission> submissions = List.of(
                Submission.builder()
                        .id(1L)
                        .exercise(testExercise)
                        .user(testUser)
                        .code("code1")
                        .status(Submission.Status.COMPLETED)
                        .testResults(new ArrayList<>())
                        .build(),
                Submission.builder()
                        .id(2L)
                        .exercise(testExercise)
                        .user(testUser)
                        .code("code2")
                        .status(Submission.Status.PENDING)
                        .testResults(new ArrayList<>())
                        .build()
        );

        when(submissionRepository.findByUserId(1L)).thenReturn(submissions);

        List<SubmissionDto> result = submissionService.getSubmissionsByUser(1L);

        assertEquals(2, result.size());
    }

    @Test
    void getSubmissionCount_ReturnsCorrectCount() {
        when(submissionRepository.countByUserIdAndExerciseId(1L, 1L)).thenReturn(5);

        int count = submissionService.getSubmissionCount(1L, 1L);

        assertEquals(5, count);
    }

    @Test
    void getLatestSubmission_ReturnsLatest() {
        Submission latestSubmission = Submission.builder()
                .id(3L)
                .exercise(testExercise)
                .user(testUser)
                .code("latest code")
                .status(Submission.Status.COMPLETED)
                .testResults(new ArrayList<>())
                .build();

        when(submissionRepository.findFirstByUserIdAndExerciseIdOrderBySubmittedAtDesc(1L, 1L))
                .thenReturn(Optional.of(latestSubmission));

        SubmissionDto result = submissionService.getLatestSubmission(1L, 1L);

        assertNotNull(result);
        assertEquals(3L, result.getId());
    }

    @Test
    void getLatestSubmission_WhenNoSubmissions_ReturnsNull() {
        when(submissionRepository.findFirstByUserIdAndExerciseIdOrderBySubmittedAtDesc(1L, 1L))
                .thenReturn(Optional.empty());

        SubmissionDto result = submissionService.getLatestSubmission(1L, 1L);

        assertNull(result);
    }
}
