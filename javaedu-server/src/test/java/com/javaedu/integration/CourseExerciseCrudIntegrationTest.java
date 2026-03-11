package com.javaedu.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.javaedu.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CourseExerciseCrudIntegrationTest extends BaseIntegrationTest {

    private String teacherToken;
    private String studentToken;
    private Long courseId;

    @BeforeEach
    void setUpUsers() throws Exception {
        teacherToken = registerAndLogin("Teacher", "teacher@test.com", "password123", User.Role.TEACHER);
        studentToken = registerAndLogin("Student", "student@test.com", "password123", User.Role.STUDENT);

        JsonNode course = createCourseViaApi(teacherToken, "Java 101", "Intro to Java");
        courseId = course.get("id").asLong();
    }

    // --- Course CRUD ---

    @Test
    void teacher_createsCourse_success() throws Exception {
        JsonNode course = createCourseViaApi(teacherToken, "Advanced Java", "Advanced topics");

        assert course.get("id").asLong() > 0;
        assert "Advanced Java".equals(course.get("name").asText());
        assert "Advanced topics".equals(course.get("description").asText());
    }

    @Test
    void teacher_getsCourses_returnsOwnCourses() throws Exception {
        createCourseViaApi(teacherToken, "Course 2", "Desc 2");

        performGet(teacherToken, "/api/courses")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Java 101")));
    }

    @Test
    void teacher_updatesCourse_success() throws Exception {
        performPut(teacherToken, "/api/courses/" + courseId, Map.of(
                "name", "Updated Course",
                "description", "Updated description"
        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Course")))
                .andExpect(jsonPath("$.description", is("Updated description")));
    }

    @Test
    void teacher_deletesCourse_success() throws Exception {
        performDelete(teacherToken, "/api/courses/" + courseId)
                .andExpect(status().isNoContent());

        performGet(teacherToken, "/api/courses/" + courseId)
                .andExpect(status().isNotFound());
    }

    @Test
    void student_cannotCreateCourse_returns403() throws Exception {
        performPost(studentToken, "/api/courses", Map.of(
                "name", "Student Course",
                "description", "Should fail"
        ))
                .andExpect(status().isForbidden());
    }

    @Test
    void getNonexistentCourse_returns404() throws Exception {
        performGet(teacherToken, "/api/courses/99999")
                .andExpect(status().isNotFound());
    }

    // --- Exercise CRUD ---

    @Test
    void teacher_createsExerciseWithTestCasesAndHints_success() throws Exception {
        List<Map<String, Object>> testCases = createTestCases(
                "Test addition", "Solution s = new Solution(); assert s.solve() == 42 : \"Expected 42\";",
                "Test negative", "Solution s = new Solution(); assert s.solve() != 0 : \"Should not be 0\";"
        );

        Map<String, Object> request = new java.util.HashMap<>();
        request.put("courseId", courseId);
        request.put("title", "Math Exercise");
        request.put("description", "Solve math problems");
        request.put("starterCode", "public class Solution { }");
        request.put("difficulty", "EASY");
        request.put("points", 100);
        request.put("category", "BASICS");
        request.put("isPublished", true);
        request.put("testCases", testCases);
        request.put("hints", List.of(Map.of(
                "content", "Think about math",
                "orderNum", 0,
                "penaltyPercentage", 10
        )));

        performPost(teacherToken, "/api/exercises", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Math Exercise")))
                .andExpect(jsonPath("$.testCases", hasSize(2)))
                .andExpect(jsonPath("$.hints", hasSize(1)));
    }

    @Test
    void teacher_updatesExercise_success() throws Exception {
        JsonNode exercise = createExerciseViaApi(teacherToken, courseId, "Original Title", null, false);
        Long exerciseId = exercise.get("id").asLong();

        performPut(teacherToken, "/api/exercises/" + exerciseId, Map.of(
                "title", "Updated Title",
                "isPublished", true
        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.isPublished", is(true)));
    }

    @Test
    void teacher_deletesExercise_success() throws Exception {
        JsonNode exercise = createExerciseViaApi(teacherToken, courseId, "To Delete", null, true);
        Long exerciseId = exercise.get("id").asLong();

        performDelete(teacherToken, "/api/exercises/" + exerciseId)
                .andExpect(status().isNoContent());

        performGet(teacherToken, "/api/exercises/" + exerciseId)
                .andExpect(status().isNotFound());
    }

    @Test
    void exercise_appearsInCourseListing() throws Exception {
        createExerciseViaApi(teacherToken, courseId, "Exercise A", null, true);
        createExerciseViaApi(teacherToken, courseId, "Exercise B", null, true);

        performGet(teacherToken, "/api/exercises/course/" + courseId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void student_cannotCreateExercise_returns403() throws Exception {
        performPost(studentToken, "/api/exercises", Map.of(
                "courseId", courseId,
                "title", "Student Exercise",
                "description", "Should fail",
                "difficulty", "EASY"
        ))
                .andExpect(status().isForbidden());
    }

    @Test
    void student_cannotUpdateExercise_returns403() throws Exception {
        JsonNode exercise = createExerciseViaApi(teacherToken, courseId, "Teacher Exercise", null, true);
        Long exerciseId = exercise.get("id").asLong();

        performPut(studentToken, "/api/exercises/" + exerciseId, Map.of(
                "title", "Hacked Title"
        ))
                .andExpect(status().isForbidden());
    }

    @Test
    void student_cannotDeleteExercise_returns403() throws Exception {
        JsonNode exercise = createExerciseViaApi(teacherToken, courseId, "Protected Exercise", null, true);
        Long exerciseId = exercise.get("id").asLong();

        performDelete(studentToken, "/api/exercises/" + exerciseId)
                .andExpect(status().isForbidden());
    }

    @Test
    void getNonexistentExercise_returns404() throws Exception {
        performGet(teacherToken, "/api/exercises/99999")
                .andExpect(status().isNotFound());
    }
}
