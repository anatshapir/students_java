package com.javaedu.service;

import com.javaedu.dto.submission.SubmissionDto;
import com.javaedu.dto.submission.SubmitCodeRequest;
import com.javaedu.exception.BadRequestException;
import com.javaedu.exception.ResourceNotFoundException;
import com.javaedu.model.*;
import com.javaedu.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final StudentAnalyticsRepository analyticsRepository;
    private final TestRunnerService testRunnerService;
    private final GradingService gradingService;

    @Transactional
    public SubmissionDto submitCode(Long userId, SubmitCodeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Exercise exercise = exerciseRepository.findById(request.getExerciseId())
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", "id", request.getExerciseId()));

        if (!exercise.getIsPublished()) {
            throw new BadRequestException("Exercise is not available for submission");
        }

        if (exercise.getDueDate() != null && exercise.getDueDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Exercise submission deadline has passed");
        }

        Submission submission = Submission.builder()
                .exercise(exercise)
                .user(user)
                .code(request.getCode())
                .status(Submission.Status.PENDING)
                .build();

        submission = submissionRepository.save(submission);
        log.info("Created submission {} for exercise {} by user {}", submission.getId(), exercise.getTitle(), user.getEmail());

        updateAnalytics(user, exercise);

        processSubmissionAsync(submission.getId());

        return SubmissionDto.fromEntity(submission);
    }

    @Async
    public void processSubmissionAsync(Long submissionId) {
        try {
            processSubmission(submissionId);
        } catch (Exception e) {
            log.error("Error processing submission {}", submissionId, e);
        }
    }

    @Transactional
    public void processSubmission(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", submissionId));

        log.info("Processing submission {}", submissionId);
        long startTime = System.currentTimeMillis();

        try {
            submission.setStatus(Submission.Status.COMPILING);
            submissionRepository.save(submission);

            TestRunnerService.CompilationResult compilationResult = testRunnerService.compile(submission.getCode());

            if (!compilationResult.isSuccess()) {
                submission.setStatus(Submission.Status.COMPILATION_ERROR);
                submission.setCompilerOutput(compilationResult.getErrors());
                submissionRepository.save(submission);
                return;
            }

            submission.setStatus(Submission.Status.RUNNING);
            submissionRepository.save(submission);

            List<TestResult> testResults = testRunnerService.runTests(submission, compilationResult.getCompiledClass());

            submission.getTestResults().addAll(testResults);
            submission.setStatus(Submission.Status.COMPLETED);
            submission.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            submission = submissionRepository.save(submission);

            gradingService.gradeSubmission(submission);

            log.info("Completed processing submission {} in {}ms", submissionId, submission.getExecutionTimeMs());

        } catch (Exception e) {
            log.error("Error running tests for submission {}", submissionId, e);
            submission.setStatus(Submission.Status.RUNTIME_ERROR);
            submission.setCompilerOutput("Runtime error: " + e.getMessage());
            submission.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            submissionRepository.save(submission);
        }
    }

    private void updateAnalytics(User user, Exercise exercise) {
        StudentAnalytics analytics = analyticsRepository.findByUserIdAndExerciseId(user.getId(), exercise.getId())
                .orElse(StudentAnalytics.builder()
                        .user(user)
                        .exercise(exercise)
                        .attempts(0)
                        .build());

        analytics.setAttempts(analytics.getAttempts() + 1);
        if (analytics.getFirstSubmissionAt() == null) {
            analytics.setFirstSubmissionAt(LocalDateTime.now());
        }

        analyticsRepository.save(analytics);
    }

    @Transactional(readOnly = true)
    public SubmissionDto getSubmissionById(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", submissionId));
        return SubmissionDto.fromEntity(submission);
    }

    @Transactional(readOnly = true)
    public List<SubmissionDto> getSubmissionsByUser(Long userId) {
        return submissionRepository.findByUserId(userId).stream()
                .map(SubmissionDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubmissionDto> getSubmissionsByExercise(Long exerciseId) {
        return submissionRepository.findByExerciseId(exerciseId).stream()
                .map(SubmissionDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubmissionDto> getSubmissionsByUserAndExercise(Long userId, Long exerciseId) {
        return submissionRepository.findByUserIdAndExerciseId(userId, exerciseId).stream()
                .map(SubmissionDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubmissionDto getLatestSubmission(Long userId, Long exerciseId) {
        return submissionRepository.findFirstByUserIdAndExerciseIdOrderBySubmittedAtDesc(userId, exerciseId)
                .map(SubmissionDto::fromEntity)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<SubmissionDto> getSubmissionsByCourse(Long courseId, Pageable pageable) {
        return submissionRepository.findByCourseId(courseId, pageable)
                .map(SubmissionDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public int getSubmissionCount(Long userId, Long exerciseId) {
        return submissionRepository.countByUserIdAndExerciseId(userId, exerciseId);
    }
}
