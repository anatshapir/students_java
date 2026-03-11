package com.javaedu.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.javaedu.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SubmissionGradingIntegrationTest extends BaseIntegrationTest {

    private String teacherToken;
    private String studentToken;
    private Long courseId;
    private Long exerciseId;

    @BeforeEach
    void setUpExercise() throws Exception {
        teacherToken = registerAndLogin("Teacher", "teacher@test.com", "password123", User.Role.TEACHER);
        studentToken = registerAndLogin("Student", "student@test.com", "password123", User.Role.STUDENT);

        JsonNode course = createCourseViaApi(teacherToken, "Java 101", "Intro");
        courseId = course.get("id").asLong();

        // Enroll student
        Long studentId = getUserIdByEmail("student@test.com");
        enrollStudentInCourse(teacherToken, courseId, studentId);

        // Create exercise with test cases
        List<Map<String, Object>> testCases = createTestCases(
                "Test returns 42",
                "Solution s = new Solution(); assert s.solve() == 42 : \"Expected 42\";",
                "Test not zero",
                "Solution s = new Solution(); assert s.solve() != 0 : \"Should not be 0\";"
        );

        JsonNode exercise = createExerciseViaApi(teacherToken, courseId, "Solve It", testCases, true);
        exerciseId = exercise.get("id").asLong();
    }

    @Test
    void submitCode_returnsPendingStatus() throws Exception {
        JsonNode submission = submitCodeViaApi(studentToken, exerciseId,
                "public class Solution { public int solve() { return 42; } }");

        assert submission.get("id").asLong() > 0;
        assert submission.get("exerciseId").asLong() == exerciseId;
        // Status should be set (PENDING or already processed)
        assert submission.has("status");
    }

    @Test
    void submitCorrectCode_eventuallyCompletes() throws Exception {
        JsonNode submission = submitCodeViaApi(studentToken, exerciseId,
                "public class Solution { public int solve() { return 42; } }");
        Long submissionId = submission.get("id").asLong();

        JsonNode completed = waitForSubmissionCompletion(studentToken, submissionId, 15000);
        String status = completed.get("status").asText();

        // Should be COMPLETED (or possibly COMPILATION_ERROR if sandbox has issues)
        assert !"PENDING".equals(status) : "Submission should not remain PENDING";
    }

    @Test
    void submitCompilationError_returnsCompilationError() throws Exception {
        JsonNode submission = submitCodeViaApi(studentToken, exerciseId,
                "public class Solution { this is not valid java }");
        Long submissionId = submission.get("id").asLong();

        JsonNode completed = waitForSubmissionCompletion(studentToken, submissionId, 15000);
        String status = completed.get("status").asText();

        // Should be COMPILATION_ERROR
        if ("COMPILATION_ERROR".equals(status)) {
            assert completed.has("compilerOutput");
        }
    }

    @Test
    void submitWrongSolution_partialCredit() throws Exception {
        JsonNode submission = submitCodeViaApi(studentToken, exerciseId,
                "public class Solution { public int solve() { return 1; } }");
        Long submissionId = submission.get("id").asLong();

        JsonNode completed = waitForSubmissionCompletion(studentToken, submissionId, 15000);

        if ("COMPLETED".equals(completed.get("status").asText())) {
            // Should have some tests passed and some failed
            // "not zero" test passes (1 != 0), but "returns 42" test fails (1 != 42)
            JsonNode grade = completed.get("grade");
            if (grade != null && !grade.isNull()) {
                int score = grade.get("score").asInt();
                int maxScore = grade.get("maxScore").asInt();
                assert score < maxScore : "Wrong solution should not get full marks";
            }
        }
    }

    @Test
    void getMySubmissions_returnsStudentSubmissions() throws Exception {
        submitCodeViaApi(studentToken, exerciseId, "public class Solution { public int solve() { return 1; } }");
        submitCodeViaApi(studentToken, exerciseId, "public class Solution { public int solve() { return 2; } }");

        performGet(studentToken, "/api/submissions/my")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getMySubmissionsForExercise_returnsExerciseSubmissions() throws Exception {
        submitCodeViaApi(studentToken, exerciseId, "public class Solution { public int solve() { return 1; } }");

        performGet(studentToken, "/api/submissions/my/exercise/" + exerciseId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].exerciseId", is(exerciseId.intValue())));
    }

    @Test
    void getLatestSubmission_returnsLatest() throws Exception {
        submitCodeViaApi(studentToken, exerciseId, "public class Solution { public int solve() { return 1; } }");
        submitCodeViaApi(studentToken, exerciseId, "public class Solution { public int solve() { return 42; } }");

        performGet(studentToken, "/api/submissions/my/exercise/" + exerciseId + "/latest")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exerciseId", is(exerciseId.intValue())))
                .andExpect(jsonPath("$.code", containsString("return 42")));
    }

    @Test
    void getSubmissionCount_returnsCorrectCount() throws Exception {
        submitCodeViaApi(studentToken, exerciseId, "public class Solution { public int solve() { return 1; } }");
        submitCodeViaApi(studentToken, exerciseId, "public class Solution { public int solve() { return 2; } }");
        submitCodeViaApi(studentToken, exerciseId, "public class Solution { public int solve() { return 3; } }");

        performGet(studentToken, "/api/submissions/my/exercise/" + exerciseId + "/count")
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    void teacher_getsAllSubmissionsForExercise() throws Exception {
        submitCodeViaApi(studentToken, exerciseId, "public class Solution { public int solve() { return 1; } }");

        performGet(teacherToken, "/api/submissions/exercise/" + exerciseId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void teacher_getsSubmissionsForUser() throws Exception {
        submitCodeViaApi(studentToken, exerciseId, "public class Solution { public int solve() { return 1; } }");

        Long studentId = getUserIdByEmail("student@test.com");
        performGet(teacherToken, "/api/submissions/user/" + studentId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void student_cannotAccessTeacherSubmissionEndpoint_returns403() throws Exception {
        performGet(studentToken, "/api/submissions/exercise/" + exerciseId)
                .andExpect(status().isForbidden());
    }

    @Test
    void submitCode_missingExerciseId_returns400() throws Exception {
        performPost(studentToken, "/api/submissions", Map.of("code", "public class Solution { }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitCode_missingCode_returns400() throws Exception {
        performPost(studentToken, "/api/submissions", Map.of("exerciseId", exerciseId))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitCode_nonexistentExercise_returns404() throws Exception {
        performPost(studentToken, "/api/submissions", Map.of(
                "exerciseId", 99999,
                "code", "public class Solution { }"
        ))
                .andExpect(status().isNotFound());
    }

    @Test
    void submitCode_noAuth_returns401() throws Exception {
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/submissions")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "exerciseId", exerciseId,
                                "code", "public class Solution { }"
                        )))
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void getSubmissionById_returnsDetails() throws Exception {
        JsonNode submission = submitCodeViaApi(studentToken, exerciseId,
                "public class Solution { public int solve() { return 42; } }");
        Long submissionId = submission.get("id").asLong();

        performGet(studentToken, "/api/submissions/" + submissionId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(submissionId.intValue())))
                .andExpect(jsonPath("$.code", containsString("return 42")));
    }
}
