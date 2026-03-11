package com.javaedu.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaedu.model.User;
import com.javaedu.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected CourseRepository courseRepository;

    @Autowired
    protected ExerciseRepository exerciseRepository;

    @Autowired
    protected SubmissionRepository submissionRepository;

    @Autowired
    protected GradeRepository gradeRepository;

    @Autowired
    protected TestResultRepository testResultRepository;

    @Autowired
    protected StudentAnalyticsRepository studentAnalyticsRepository;

    @Autowired
    protected AIInteractionRepository aiInteractionRepository;

    @Autowired
    protected EnrollmentRequestRepository enrollmentRequestRepository;

    @Autowired
    protected ErrorPatternRepository errorPatternRepository;

    @Autowired
    protected HintRepository hintRepository;

    @BeforeEach
    void cleanDatabase() {
        errorPatternRepository.deleteAll();
        aiInteractionRepository.deleteAll();
        studentAnalyticsRepository.deleteAll();
        testResultRepository.deleteAll();
        gradeRepository.deleteAll();
        submissionRepository.deleteAll();
        hintRepository.deleteAll();
        enrollmentRequestRepository.deleteAll();
        exerciseRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected String registerAndLogin(String name, String email, String password, User.Role role) throws Exception {
        Map<String, Object> registerRequest = Map.of(
                "name", name,
                "email", email,
                "password", password,
                "role", role.name()
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return parseResponse(result).get("accessToken").asText();
    }

    protected JsonNode createCourseViaApi(String token, String name, String description) throws Exception {
        Map<String, String> request = Map.of(
                "name", name,
                "description", description
        );

        MvcResult result = mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return parseResponse(result);
    }

    protected JsonNode createExerciseViaApi(String token, Long courseId, String title,
                                            List<Map<String, Object>> testCases,
                                            boolean published) throws Exception {
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("courseId", courseId);
        request.put("title", title);
        request.put("description", "Description for " + title);
        request.put("starterCode", "public class Solution { }");
        request.put("solutionCode", "public class Solution { public int solve() { return 42; } }");
        request.put("difficulty", "MEDIUM");
        request.put("points", 100);
        request.put("category", "BASICS");
        request.put("isPublished", published);
        if (testCases != null) {
            request.put("testCases", testCases);
        }

        MvcResult result = mockMvc.perform(post("/api/exercises")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return parseResponse(result);
    }

    protected JsonNode submitCodeViaApi(String token, Long exerciseId, String code) throws Exception {
        Map<String, Object> request = Map.of(
                "exerciseId", exerciseId,
                "code", code
        );

        MvcResult result = mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return parseResponse(result);
    }

    protected void enrollStudentInCourse(String teacherToken, Long courseId, Long studentId) throws Exception {
        mockMvc.perform(post("/api/courses/" + courseId + "/students/" + studentId)
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());
    }

    protected ResultActions performGet(String token, String url) throws Exception {
        return mockMvc.perform(get(url)
                .header("Authorization", "Bearer " + token));
    }

    protected ResultActions performPost(String token, String url, Object body) throws Exception {
        var builder = post(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON);

        if (body != null) {
            builder.content(objectMapper.writeValueAsString(body));
        }

        return mockMvc.perform(builder);
    }

    protected ResultActions performPut(String token, String url, Object body) throws Exception {
        var builder = put(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON);

        if (body != null) {
            builder.content(objectMapper.writeValueAsString(body));
        }

        return mockMvc.perform(builder);
    }

    protected ResultActions performDelete(String token, String url) throws Exception {
        return mockMvc.perform(delete(url)
                .header("Authorization", "Bearer " + token));
    }

    protected JsonNode parseResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected JsonNode waitForSubmissionCompletion(String token, Long submissionId, long maxWaitMs)
            throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < maxWaitMs) {
            MvcResult result = performGet(token, "/api/submissions/" + submissionId)
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode submission = parseResponse(result);
            String status = submission.get("status").asText();

            if (!"PENDING".equals(status) && !"COMPILING".equals(status) && !"RUNNING".equals(status)) {
                return submission;
            }

            Thread.sleep(200);
        }

        // Return whatever we have after timeout
        MvcResult result = performGet(token, "/api/submissions/" + submissionId)
                .andExpect(status().isOk())
                .andReturn();
        return parseResponse(result);
    }

    protected List<Map<String, Object>> createTestCases(String... nameAndCodePairs) {
        List<Map<String, Object>> testCases = new java.util.ArrayList<>();
        for (int i = 0; i < nameAndCodePairs.length; i += 2) {
            Map<String, Object> tc = new java.util.HashMap<>();
            tc.put("name", nameAndCodePairs[i]);
            tc.put("testCode", nameAndCodePairs[i + 1]);
            tc.put("isHidden", false);
            tc.put("points", 50);
            tc.put("orderNum", i / 2);
            tc.put("timeoutSeconds", 5);
            testCases.add(tc);
        }
        return testCases;
    }

    protected Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }
}
