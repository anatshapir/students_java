package com.javaedu.service;

import com.javaedu.model.*;
import com.javaedu.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningEngineService {

    private final ErrorPatternRepository errorPatternRepository;
    private final StudentAnalyticsRepository analyticsRepository;
    private final TestResultRepository testResultRepository;
    private final SubmissionRepository submissionRepository;
    private final ExerciseRepository exerciseRepository;
    private final HintRepository hintRepository;

    private static final List<ErrorPatternMatcher> COMMON_ERROR_PATTERNS = List.of(
            new ErrorPatternMatcher(
                    "NullPointerException",
                    "null pointer",
                    "Make sure to initialize your variables before using them. Check if any object could be null."
            ),
            new ErrorPatternMatcher(
                    "ArrayIndexOutOfBoundsException",
                    "array.*index|index.*bound",
                    "Your array access is going beyond the array's size. Remember arrays are 0-indexed."
            ),
            new ErrorPatternMatcher(
                    "StringIndexOutOfBoundsException",
                    "string.*index|substring",
                    "Check your string operations - you might be accessing characters beyond the string length."
            ),
            new ErrorPatternMatcher(
                    "InfiniteLoop",
                    "timeout|timed out",
                    "Your code might have an infinite loop. Check your loop conditions and make sure they can become false."
            ),
            new ErrorPatternMatcher(
                    "StackOverflow",
                    "stack.*overflow",
                    "You have infinite recursion. Make sure your recursive function has a proper base case."
            ),
            new ErrorPatternMatcher(
                    "ClassCastException",
                    "class.*cast|cannot.*cast",
                    "You're trying to cast an object to an incompatible type. Check your type hierarchy."
            ),
            new ErrorPatternMatcher(
                    "NumberFormatException",
                    "number.*format|parse.*int|parse.*double",
                    "The string you're trying to convert to a number isn't a valid number format."
            )
    );

    @Transactional
    public void analyzeSubmission(Submission submission) {
        if (submission.getStatus() != Submission.Status.COMPLETED) {
            return;
        }

        for (TestResult result : submission.getTestResults()) {
            if (!result.getPassed() && result.getErrorMessage() != null) {
                categorizeError(submission.getExercise(), result.getErrorMessage());
            }
        }
    }

    private void categorizeError(Exercise exercise, String errorMessage) {
        for (ErrorPatternMatcher matcher : COMMON_ERROR_PATTERNS) {
            if (matcher.matches(errorMessage)) {
                updateErrorPattern(exercise, matcher.patternName(), matcher.suggestedHint());
                return;
            }
        }

        String genericPattern = extractGenericPattern(errorMessage);
        if (genericPattern != null) {
            updateErrorPattern(exercise, genericPattern, null);
        }
    }

    private void updateErrorPattern(Exercise exercise, String patternName, String suggestedHint) {
        ErrorPattern pattern = errorPatternRepository
                .findByExerciseIdAndPattern(exercise.getId(), patternName)
                .orElse(ErrorPattern.builder()
                        .exercise(exercise)
                        .pattern(patternName)
                        .occurrenceCount(0)
                        .suggestedHint(suggestedHint)
                        .build());

        pattern.setOccurrenceCount(pattern.getOccurrenceCount() + 1);
        errorPatternRepository.save(pattern);
    }

    private String extractGenericPattern(String errorMessage) {
        if (errorMessage.contains("Exception")) {
            int idx = errorMessage.indexOf("Exception");
            int start = Math.max(0, errorMessage.lastIndexOf(' ', idx) + 1);
            return errorMessage.substring(start, idx + "Exception".length());
        }
        return null;
    }

    @Transactional(readOnly = true)
    public ExerciseAnalytics getExerciseAnalytics(Long exerciseId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new RuntimeException("Exercise not found"));

        int totalSubmissions = submissionRepository.findByExerciseId(exerciseId).size();
        int uniqueStudents = submissionRepository.countDistinctUsersByExerciseId(exerciseId);
        int completedStudents = analyticsRepository.countCompletedByExerciseId(exerciseId);

        Double avgAttempts = analyticsRepository.findAverageAttemptsByExerciseId(exerciseId);
        Double avgTimeToComplete = analyticsRepository.findAverageTimeToCompleteByExerciseId(exerciseId);

        List<ErrorPattern> commonErrors = errorPatternRepository
                .findByExerciseIdOrderByOccurrenceCountDesc(exerciseId);

        return new ExerciseAnalytics(
                exerciseId,
                exercise.getTitle(),
                totalSubmissions,
                uniqueStudents,
                completedStudents,
                avgAttempts != null ? avgAttempts : 0,
                avgTimeToComplete != null ? avgTimeToComplete : 0,
                commonErrors.stream()
                        .limit(5)
                        .map(ep -> new CommonError(ep.getPattern(), ep.getOccurrenceCount(), ep.getSuggestedHint()))
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public StudentProgress getStudentProgress(Long userId, Long courseId) {
        List<StudentAnalytics> analytics = analyticsRepository.findByUserIdAndCourseId(userId, courseId);

        int totalExercises = analytics.size();
        int completed = (int) analytics.stream().filter(a -> a.getCompletedAt() != null).count();
        int totalAttempts = analytics.stream().mapToInt(StudentAnalytics::getAttempts).sum();
        int totalTimeSpent = analytics.stream().mapToInt(StudentAnalytics::getTimeSpentMinutes).sum();
        int totalHintsUsed = analytics.stream().mapToInt(StudentAnalytics::getHintsUsed).sum();

        return new StudentProgress(
                userId,
                courseId,
                totalExercises,
                completed,
                totalAttempts,
                totalTimeSpent,
                totalHintsUsed,
                analytics.stream()
                        .map(a -> new ExerciseProgress(
                                a.getExercise().getId(),
                                a.getExercise().getTitle(),
                                a.getAttempts(),
                                a.getCompletedAt() != null,
                                a.getHintsUsed()
                        ))
                        .toList()
        );
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void generateHintSuggestions() {
        log.info("Running scheduled hint suggestion generation");

        List<Exercise> exercises = exerciseRepository.findAll();

        for (Exercise exercise : exercises) {
            List<ErrorPattern> frequentErrors = errorPatternRepository
                    .findFrequentPatterns(exercise.getId(), 5);

            for (ErrorPattern error : frequentErrors) {
                if (error.getSuggestedHint() != null && !hasHintForPattern(exercise, error.getPattern())) {
                    Hint hint = Hint.builder()
                            .exercise(exercise)
                            .content("Common mistake: " + error.getSuggestedHint())
                            .orderNum(exercise.getHints().size())
                            .isAiGenerated(true)
                            .penaltyPercentage(0)
                            .build();
                    hintRepository.save(hint);

                    log.info("Generated hint for exercise {}: {}", exercise.getTitle(), error.getPattern());
                }
            }
        }
    }

    private boolean hasHintForPattern(Exercise exercise, String pattern) {
        return exercise.getHints().stream()
                .anyMatch(h -> h.getContent().toLowerCase().contains(pattern.toLowerCase()));
    }

    @Transactional(readOnly = true)
    public List<StrugglingStudent> identifyStrugglingStudents(Long courseId) {
        List<StrugglingStudent> struggling = new ArrayList<>();

        List<StudentAnalytics> allAnalytics = analyticsRepository.findAll().stream()
                .filter(a -> a.getExercise().getCourse().getId().equals(courseId))
                .toList();

        Map<Long, List<StudentAnalytics>> byStudent = new HashMap<>();
        for (StudentAnalytics a : allAnalytics) {
            byStudent.computeIfAbsent(a.getUser().getId(), k -> new ArrayList<>()).add(a);
        }

        for (Map.Entry<Long, List<StudentAnalytics>> entry : byStudent.entrySet()) {
            List<StudentAnalytics> studentAnalytics = entry.getValue();
            User student = studentAnalytics.get(0).getUser();

            int totalAttempts = studentAnalytics.stream().mapToInt(StudentAnalytics::getAttempts).sum();
            long incompleteCount = studentAnalytics.stream()
                    .filter(a -> a.getCompletedAt() == null && a.getAttempts() > 3)
                    .count();

            if (incompleteCount >= 2 || totalAttempts > studentAnalytics.size() * 5) {
                struggling.add(new StrugglingStudent(
                        student.getId(),
                        student.getName(),
                        student.getEmail(),
                        (int) incompleteCount,
                        totalAttempts
                ));
            }
        }

        return struggling;
    }

    private record ErrorPatternMatcher(String patternName, String regex, String suggestedHint) {
        boolean matches(String errorMessage) {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(errorMessage);
            return matcher.find();
        }
    }

    public record ExerciseAnalytics(
            Long exerciseId,
            String exerciseTitle,
            int totalSubmissions,
            int uniqueStudents,
            int completedStudents,
            double averageAttempts,
            double averageTimeToComplete,
            List<CommonError> commonErrors
    ) {}

    public record CommonError(String pattern, int count, String suggestedHint) {}

    public record StudentProgress(
            Long userId,
            Long courseId,
            int totalExercises,
            int completedExercises,
            int totalAttempts,
            int totalTimeSpentMinutes,
            int totalHintsUsed,
            List<ExerciseProgress> exercises
    ) {}

    public record ExerciseProgress(
            Long exerciseId,
            String title,
            int attempts,
            boolean completed,
            int hintsUsed
    ) {}

    public record StrugglingStudent(
            Long userId,
            String name,
            String email,
            int incompleteExercises,
            int totalAttempts
    ) {}
}
