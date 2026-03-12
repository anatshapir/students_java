package com.javaedu.service;

import com.javaedu.dto.exercise.CreateExerciseRequest;
import com.javaedu.dto.exercise.ExerciseDto;
import com.javaedu.dto.exercise.UpdateExerciseRequest;
import com.javaedu.exception.ResourceNotFoundException;
import com.javaedu.model.*;
import com.javaedu.repository.CourseRepository;
import com.javaedu.repository.ExerciseRepository;
import com.javaedu.repository.HintRepository;
import com.javaedu.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaedu.dto.exercise.HintDto;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final CourseRepository courseRepository;
    private final TestCaseRepository testCaseRepository;
    private final HintRepository hintRepository;

    @Transactional(readOnly = true)
    public List<ExerciseDto> getExercisesByTeacher(Long teacherId) {
        return exerciseRepository.findByTeacherId(teacherId).stream()
                .map(ExerciseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExerciseDto> getExercisesByCourse(Long courseId) {
        return exerciseRepository.findByCourseId(courseId).stream()
                .map(ExerciseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExerciseDto> getPublishedExercisesByCourse(Long courseId) {
        return exerciseRepository.findByCourseIdAndIsPublishedTrue(courseId).stream()
                .map(ExerciseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExerciseDto> getAvailableExercisesForStudent(Long studentId) {
        return exerciseRepository.findAvailableForStudent(studentId).stream()
                .map(ExerciseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExerciseDto getExerciseById(Long exerciseId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", "id", exerciseId));
        return ExerciseDto.fromEntityWithDetails(exercise);
    }

    @Transactional(readOnly = true)
    public ExerciseDto getExerciseByIdForTeacher(Long exerciseId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", "id", exerciseId));

        ExerciseDto dto = ExerciseDto.fromEntity(exercise);
        dto.setTestCases(exercise.getTestCases().stream()
                .map(tc -> {
                    var testCaseDto = com.javaedu.dto.exercise.TestCaseDto.fromEntity(tc);
                    return testCaseDto;
                })
                .toList());
        dto.setHints(exercise.getHints().stream()
                .map(com.javaedu.dto.exercise.HintDto::fromEntity)
                .toList());
        return dto;
    }

    @Transactional
    public ExerciseDto createExercise(CreateExerciseRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));

        Exercise exercise = Exercise.builder()
                .course(course)
                .title(request.getTitle())
                .description(request.getDescription())
                .starterCode(request.getStarterCode())
                .solutionCode(request.getSolutionCode())
                .difficulty(request.getDifficulty())
                .points(request.getPoints())
                .category(request.getCategory())
                .dueDate(request.getDueDate())
                .isPublished(request.getIsPublished())
                .build();

        exercise = exerciseRepository.save(exercise);

        if (request.getTestCases() != null) {
            for (CreateExerciseRequest.TestCaseRequest tcRequest : request.getTestCases()) {
                TestCase testCase = TestCase.builder()
                        .exercise(exercise)
                        .name(tcRequest.getName())
                        .testCode(tcRequest.getTestCode())
                        .input(tcRequest.getInput())
                        .expectedOutput(tcRequest.getExpectedOutput())
                        .isHidden(tcRequest.getIsHidden())
                        .points(tcRequest.getPoints())
                        .orderNum(tcRequest.getOrderNum())
                        .timeoutSeconds(tcRequest.getTimeoutSeconds())
                        .build();
                testCaseRepository.save(testCase);
                exercise.getTestCases().add(testCase);
            }
        }

        if (request.getHints() != null) {
            for (CreateExerciseRequest.HintRequest hintRequest : request.getHints()) {
                Hint hint = Hint.builder()
                        .exercise(exercise)
                        .content(hintRequest.getContent())
                        .orderNum(hintRequest.getOrderNum())
                        .penaltyPercentage(hintRequest.getPenaltyPercentage())
                        .build();
                hintRepository.save(hint);
                exercise.getHints().add(hint);
            }
        }

        log.info("Created exercise: {} for course: {}", exercise.getTitle(), course.getName());
        return ExerciseDto.fromEntityWithDetails(exercise);
    }

    @Transactional
    public ExerciseDto updateExercise(Long exerciseId, UpdateExerciseRequest request) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", "id", exerciseId));

        if (request.getTitle() != null) exercise.setTitle(request.getTitle());
        if (request.getDescription() != null) exercise.setDescription(request.getDescription());
        if (request.getStarterCode() != null) exercise.setStarterCode(request.getStarterCode());
        if (request.getSolutionCode() != null) exercise.setSolutionCode(request.getSolutionCode());
        if (request.getDifficulty() != null) exercise.setDifficulty(request.getDifficulty());
        if (request.getPoints() != null) exercise.setPoints(request.getPoints());
        if (request.getCategory() != null) exercise.setCategory(request.getCategory());
        if (request.getDueDate() != null) exercise.setDueDate(request.getDueDate());
        if (request.getIsPublished() != null) exercise.setIsPublished(request.getIsPublished());

        if (request.getTestCases() != null) {
            testCaseRepository.deleteAll(exercise.getTestCases());
            exercise.getTestCases().clear();

            for (CreateExerciseRequest.TestCaseRequest tcRequest : request.getTestCases()) {
                TestCase testCase = TestCase.builder()
                        .exercise(exercise)
                        .name(tcRequest.getName())
                        .testCode(tcRequest.getTestCode())
                        .input(tcRequest.getInput())
                        .expectedOutput(tcRequest.getExpectedOutput())
                        .isHidden(tcRequest.getIsHidden())
                        .points(tcRequest.getPoints())
                        .orderNum(tcRequest.getOrderNum())
                        .timeoutSeconds(tcRequest.getTimeoutSeconds())
                        .build();
                testCaseRepository.save(testCase);
                exercise.getTestCases().add(testCase);
            }
        }

        if (request.getHints() != null) {
            hintRepository.deleteAll(exercise.getHints());
            exercise.getHints().clear();

            for (CreateExerciseRequest.HintRequest hintRequest : request.getHints()) {
                Hint hint = Hint.builder()
                        .exercise(exercise)
                        .content(hintRequest.getContent())
                        .orderNum(hintRequest.getOrderNum())
                        .penaltyPercentage(hintRequest.getPenaltyPercentage())
                        .build();
                hintRepository.save(hint);
                exercise.getHints().add(hint);
            }
        }

        exercise = exerciseRepository.save(exercise);
        log.info("Updated exercise: {}", exercise.getTitle());
        return ExerciseDto.fromEntityWithDetails(exercise);
    }

    @Transactional
    public void deleteExercise(Long exerciseId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResourceNotFoundException("Exercise", "id", exerciseId));
        exerciseRepository.delete(exercise);
        log.info("Deleted exercise: {}", exercise.getTitle());
    }

    /**
     * Returns hints for a student up to the requested hint number (progressive reveal).
     * Students request hint N and get hints 1..N.
     */
    @Transactional(readOnly = true)
    public List<HintDto> getHintsForStudent(Long exerciseId, int upToOrder) {
        List<Hint> allHints = hintRepository.findByExerciseIdOrderByOrderNumAsc(exerciseId);
        return allHints.stream()
                .filter(h -> h.getOrderNum() <= upToOrder)
                .map(HintDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public int getHintCount(Long exerciseId) {
        return (int) hintRepository.countByExerciseId(exerciseId);
    }

    @Transactional(readOnly = true)
    public List<ExerciseDto> getExercisesByCategory(Exercise.Category category) {
        return exerciseRepository.findByCategory(category).stream()
                .map(ExerciseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExerciseDto> getExercisesByDifficulty(Exercise.Difficulty difficulty) {
        return exerciseRepository.findByDifficulty(difficulty).stream()
                .map(ExerciseDto::fromEntity)
                .toList();
    }
}
