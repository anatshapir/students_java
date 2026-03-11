package com.javaedu.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.javaedu.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DashboardAnalyticsIntegrationTest extends BaseIntegrationTest {

    private String teacherToken;
    private String studentToken;
    private Long courseId;
    private Long exerciseId;

    @BeforeEach
    void setUpData() throws Exception {
        teacherToken = registerAndLogin("Teacher", "teacher@test.com", "password123", User.Role.TEACHER);
        studentToken = registerAndLogin("Student", "student@test.com", "password123", User.Role.STUDENT);

        JsonNode course = createCourseViaApi(teacherToken, "Java 101", "Intro");
        courseId = course.get("id").asLong();

        Long studentId = getUserIdByEmail("student@test.com");
        enrollStudentInCourse(teacherToken, courseId, studentId);

        List<Map<String, Object>> testCases = createTestCases(
                "Test solve", "Solution s = new Solution(); assert s.solve() == 42 : \"Expected 42\";"
        );

        JsonNode exercise = createExerciseViaApi(teacherToken, courseId, "Exercise 1", testCases, true);
        exerciseId = exercise.get("id").asLong();
    }

    @Test
    void teacherDashboard_returnsCorrectOverview() throws Exception {
        performGet(teacherToken, "/api/dashboard/teacher/overview")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCourses", is(1)))
                .andExpect(jsonPath("$.totalStudents", is(1)))
                .andExpect(jsonPath("$.totalExercises", is(1)))
                .andExpect(jsonPath("$.courses", hasSize(1)))
                .andExpect(jsonPath("$.courses[0].name", is("Java 101")));
    }

    @Test
    void teacherDashboard_withMultipleCoursesAndStudents() throws Exception {
        // Create second course and second student
        createCourseViaApi(teacherToken, "Java 201", "Advanced");
        String student2Token = registerAndLogin("Student2", "student2@test.com", "password123", User.Role.STUDENT);
        Long student2Id = getUserIdByEmail("student2@test.com");
        enrollStudentInCourse(teacherToken, courseId, student2Id);

        performGet(teacherToken, "/api/dashboard/teacher/overview")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCourses", is(2)))
                .andExpect(jsonPath("$.totalStudents", is(2)));
    }

    @Test
    void courseAnalytics_returnsExerciseStats() throws Exception {
        // Submit some code
        submitCodeViaApi(studentToken, exerciseId,
                "public class Solution { public int solve() { return 42; } }");

        performGet(teacherToken, "/api/dashboard/teacher/course/" + courseId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId", is(courseId.intValue())))
                .andExpect(jsonPath("$.courseName", is("Java 101")))
                .andExpect(jsonPath("$.studentCount", is(1)))
                .andExpect(jsonPath("$.exerciseAnalytics", hasSize(1)));
    }

    @Test
    void exerciseAnalytics_returnsSubmissionStats() throws Exception {
        submitCodeViaApi(studentToken, exerciseId,
                "public class Solution { public int solve() { return 1; } }");
        submitCodeViaApi(studentToken, exerciseId,
                "public class Solution { public int solve() { return 42; } }");

        performGet(teacherToken, "/api/dashboard/teacher/exercise/" + exerciseId + "/analytics")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exerciseId", is(exerciseId.intValue())))
                .andExpect(jsonPath("$.totalSubmissions", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.uniqueStudents", greaterThanOrEqualTo(1)));
    }

    @Test
    void studentDashboard_accessedByTeacher_returnsTeacherProgress() throws Exception {
        // Note: /api/dashboard/** requires TEACHER role in SecurityConfig
        // The student progress endpoint returns data for the current authenticated user
        performGet(teacherToken, "/api/dashboard/student/progress")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolledCourses", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.totalSubmissions", greaterThanOrEqualTo(0)));
    }

    @Test
    void studentCourseProgress_accessedByTeacher() throws Exception {
        // Note: /api/dashboard/** requires TEACHER role in SecurityConfig
        performGet(teacherToken, "/api/dashboard/student/course/" + courseId + "/progress")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId", is(courseId.intValue())));
    }

    @Test
    void emptyState_teacherDashboard_returnsZeros() throws Exception {
        // Create fresh teacher with no data
        String emptyTeacherToken = registerAndLogin("EmptyTeacher", "empty@test.com", "password123", User.Role.TEACHER);

        performGet(emptyTeacherToken, "/api/dashboard/teacher/overview")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCourses", is(0)))
                .andExpect(jsonPath("$.totalStudents", is(0)))
                .andExpect(jsonPath("$.totalExercises", is(0)))
                .andExpect(jsonPath("$.courses", hasSize(0)));
    }

    @Test
    void emptyState_studentDashboard_accessedByTeacher_returnsZeros() throws Exception {
        // /api/dashboard/** requires TEACHER role
        String emptyTeacher2Token = registerAndLogin("EmptyTeacher2", "emptyteacher2@test.com", "password123", User.Role.TEACHER);

        performGet(emptyTeacher2Token, "/api/dashboard/student/progress")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolledCourses", is(0)))
                .andExpect(jsonPath("$.totalSubmissions", is(0)));
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
    void student_cannotAccessStudentDashboard_returns403() throws Exception {
        // All /api/dashboard/** endpoints require TEACHER role in SecurityConfig
        performGet(studentToken, "/api/dashboard/student/progress")
                .andExpect(status().isForbidden());
    }
}
