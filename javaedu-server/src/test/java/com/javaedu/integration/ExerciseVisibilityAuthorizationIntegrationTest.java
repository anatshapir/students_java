package com.javaedu.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.javaedu.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExerciseVisibilityAuthorizationIntegrationTest extends BaseIntegrationTest {

    private String teacherToken;
    private String studentToken;
    private Long courseId;

    @BeforeEach
    void setUpUsersAndCourse() throws Exception {
        teacherToken = registerAndLogin("Teacher", "teacher@test.com", "password123", User.Role.TEACHER);
        studentToken = registerAndLogin("Student", "student@test.com", "password123", User.Role.STUDENT);

        JsonNode course = createCourseViaApi(teacherToken, "Java 101", "Intro");
        courseId = course.get("id").asLong();

        // Enroll student
        Long studentId = getUserIdByEmail("student@test.com");
        enrollStudentInCourse(teacherToken, courseId, studentId);
    }

    @Test
    void student_seesOnlyPublishedExercises() throws Exception {
        createExerciseViaApi(teacherToken, courseId, "Published Ex", null, true);
        createExerciseViaApi(teacherToken, courseId, "Unpublished Ex", null, false);

        performGet(studentToken, "/api/exercises/course/" + courseId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Published Ex")));
    }

    @Test
    void teacher_seesAllExercises_publishedAndUnpublished() throws Exception {
        createExerciseViaApi(teacherToken, courseId, "Published Ex", null, true);
        createExerciseViaApi(teacherToken, courseId, "Unpublished Ex", null, false);

        performGet(teacherToken, "/api/exercises/course/" + courseId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void student_cannotAccessGetAllExercises_returns403() throws Exception {
        performGet(studentToken, "/api/exercises")
                .andExpect(status().isForbidden());
    }

    @Test
    void teacher_canAccessGetAllExercises() throws Exception {
        createExerciseViaApi(teacherToken, courseId, "My Exercise", null, true);

        performGet(teacherToken, "/api/exercises")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void unauthenticated_cannotAccessExercises_returns401() throws Exception {
        mockMvc.perform(get("/api/exercises"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_cannotAccessCourses_returns401() throws Exception {
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isUnauthorized());
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
    void student_cannotAccessTeacherDashboard_returns403() throws Exception {
        performGet(studentToken, "/api/dashboard/teacher/overview")
                .andExpect(status().isForbidden());
    }

    @Test
    void student_cannotAccessCourseAnalytics_returns403() throws Exception {
        performGet(studentToken, "/api/dashboard/teacher/course/" + courseId)
                .andExpect(status().isForbidden());
    }

    @Test
    void student_cannotAccessExerciseAnalytics_returns403() throws Exception {
        JsonNode exercise = createExerciseViaApi(teacherToken, courseId, "Test Ex", null, true);
        Long exerciseId = exercise.get("id").asLong();

        performGet(studentToken, "/api/dashboard/teacher/exercise/" + exerciseId + "/analytics")
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidJwtToken_returns401() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .header("Authorization", "Bearer invalid.garbage.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedAuthorizationHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .header("Authorization", "NotBearer sometoken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void student_canAccessAvailableExercises() throws Exception {
        createExerciseViaApi(teacherToken, courseId, "Available Ex", null, true);

        performGet(studentToken, "/api/exercises/available")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void student_cannotAccessStudentDashboard_returns403() throws Exception {
        // All /api/dashboard/** endpoints require TEACHER role in SecurityConfig
        performGet(studentToken, "/api/dashboard/student/progress")
                .andExpect(status().isForbidden());
    }

    @Test
    void student_cannotDeleteCourse_returns403() throws Exception {
        performDelete(studentToken, "/api/courses/" + courseId)
                .andExpect(status().isForbidden());
    }
}
