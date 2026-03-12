package com.javaedu.service;

import com.javaedu.exception.ResourceNotFoundException;
import com.javaedu.model.Course;
import com.javaedu.model.Exercise;
import com.javaedu.model.Grade;
import com.javaedu.model.User;
import com.javaedu.repository.CourseRepository;
import com.javaedu.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradeExportService {

    private final CourseRepository courseRepository;
    private final GradeRepository gradeRepository;

    @Transactional(readOnly = true)
    public String exportGradesToCsv(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        List<Exercise> exercises = course.getExercises().stream()
                .sorted(Comparator.comparing(Exercise::getTitle))
                .toList();

        List<User> students = course.getStudents().stream()
                .sorted(Comparator.comparing(User::getName))
                .toList();

        if (exercises.isEmpty() || students.isEmpty()) {
            return "No data to export";
        }

        // Build grade lookup: (userId, exerciseId) -> best grade
        Map<String, Grade> gradeMap = new HashMap<>();
        for (Exercise exercise : exercises) {
            for (User student : students) {
                gradeRepository.findBestGradeByUserIdAndExerciseId(student.getId(), exercise.getId())
                        .ifPresent(grade -> gradeMap.put(student.getId() + "_" + exercise.getId(), grade));
            }
        }

        StringBuilder csv = new StringBuilder();

        // Header row
        csv.append(escapeCsv("Student Name"));
        csv.append(',').append(escapeCsv("Email"));
        for (Exercise exercise : exercises) {
            csv.append(',').append(escapeCsv(exercise.getTitle()));
            csv.append(',').append(escapeCsv(exercise.getTitle() + " (%)"));
        }
        csv.append(',').append(escapeCsv("Total Score"));
        csv.append(',').append(escapeCsv("Total Max"));
        csv.append(',').append(escapeCsv("Average (%)"));
        csv.append('\n');

        // Data rows
        for (User student : students) {
            csv.append(escapeCsv(student.getName()));
            csv.append(',').append(escapeCsv(student.getEmail()));

            int totalScore = 0;
            int totalMax = 0;
            int gradedCount = 0;

            for (Exercise exercise : exercises) {
                Grade grade = gradeMap.get(student.getId() + "_" + exercise.getId());
                if (grade != null) {
                    csv.append(',').append(grade.getScore()).append('/').append(grade.getMaxScore());
                    csv.append(',').append(String.format("%.1f", grade.getPercentage()));
                    totalScore += grade.getScore();
                    totalMax += grade.getMaxScore();
                    gradedCount++;
                } else {
                    csv.append(",—,—");
                }
            }

            csv.append(',').append(totalScore);
            csv.append(',').append(totalMax);
            if (gradedCount > 0 && totalMax > 0) {
                csv.append(',').append(String.format("%.1f", (double) totalScore / totalMax * 100));
            } else {
                csv.append(",—");
            }
            csv.append('\n');
        }

        log.info("Exported CSV grades for course '{}': {} students, {} exercises",
                course.getName(), students.size(), exercises.size());

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
