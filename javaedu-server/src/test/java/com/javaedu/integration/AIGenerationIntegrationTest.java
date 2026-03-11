package com.javaedu.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.javaedu.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AIGenerationIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private WebClient.Builder webClientBuilder;

    private String teacherToken;
    private String studentToken;
    private Long courseId;
    private Long exerciseId;

    @BeforeEach
    void setUpUsersAndMock() throws Exception {
        teacherToken = registerAndLogin("Teacher", "teacher@test.com", "password123", User.Role.TEACHER);
        studentToken = registerAndLogin("Student", "student@test.com", "password123", User.Role.STUDENT);

        JsonNode course = createCourseViaApi(teacherToken, "Java 101", "Intro");
        courseId = course.get("id").asLong();

        Long studentId = getUserIdByEmail("student@test.com");
        enrollStudentInCourse(teacherToken, courseId, studentId);

        JsonNode exercise = createExerciseViaApi(teacherToken, courseId, "Test Ex", null, true);
        exerciseId = exercise.get("id").asLong();

        setupWebClientMock();
    }

    private WebClient.ResponseSpec currentResponseSpec;

    @SuppressWarnings("unchecked")
    private void setupMockChain(Map<String, Object> responseBody) {
        WebClient mockWebClient = Mockito.mock(WebClient.class);
        WebClient.RequestBodyUriSpec mockRequestBodyUriSpec = Mockito.mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec mockRequestBodySpec = Mockito.mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec mockRequestHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        currentResponseSpec = Mockito.mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(any(String.class), any(String[].class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(mockWebClient);

        when(mockWebClient.post()).thenReturn(mockRequestBodyUriSpec);
        when(mockRequestBodyUriSpec.uri(anyString())).thenReturn(mockRequestBodySpec);
        when(mockRequestBodySpec.bodyValue(any())).thenReturn(mockRequestHeadersSpec);
        when(mockRequestHeadersSpec.retrieve()).thenReturn(currentResponseSpec);

        when(currentResponseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(responseBody));
    }

    private void setupWebClientMock() {
        Map<String, Object> askResponse = Map.of(
                "content", List.of(Map.of("type", "text", "text", "Great question! Think about how arrays work in Java.")),
                "usage", Map.of("input_tokens", 100, "output_tokens", 50)
        );
        setupMockChain(askResponse);
    }

    private void setupGenerateExerciseMock() {
        String generatedJson = """
                {
                  "title": "Array Reversal",
                  "description": "Write a method to reverse an array",
                  "starterCode": "public class Solution { public int[] reverse(int[] arr) { return null; } }",
                  "solutionCode": "public class Solution { public int[] reverse(int[] arr) { int[] r = new int[arr.length]; for(int i=0;i<arr.length;i++) r[i]=arr[arr.length-1-i]; return r; } }",
                  "difficulty": "MEDIUM",
                  "category": "ALGORITHMS",
                  "points": 100,
                  "testCases": [
                    {"name": "Test basic", "testCode": "assert true;", "isHidden": false, "points": 50, "description": "Basic test"}
                  ],
                  "hints": ["Think about swapping elements"]
                }
                """;

        Map<String, Object> genResponse = Map.of(
                "content", List.of(Map.of("type", "text", "text", generatedJson)),
                "usage", Map.of("input_tokens", 200, "output_tokens", 300)
        );
        setupMockChain(genResponse);
    }

    @Test
    void generateExercise_withTextPrompt_returnsStructuredResponse() throws Exception {
        setupGenerateExerciseMock();

        performPost(teacherToken, "/api/ai/generate-exercise", Map.of(
                "prompt", "Create an exercise about array reversal",
                "numberOfTestCases", 3
        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", notNullValue()))
                .andExpect(jsonPath("$.description", notNullValue()))
                .andExpect(jsonPath("$.starterCode", notNullValue()))
                .andExpect(jsonPath("$.testCases", notNullValue()));
    }

    @Test
    void generateExercise_withDifficultyOverride() throws Exception {
        setupGenerateExerciseMock();

        performPost(teacherToken, "/api/ai/generate-exercise", Map.of(
                "prompt", "Create a sorting exercise",
                "difficulty", "HARD",
                "category", "DATA_STRUCTURES",
                "numberOfTestCases", 2
        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficulty", is("HARD")))
                .andExpect(jsonPath("$.category", is("DATA_STRUCTURES")));
    }

    @Test
    void student_cannotGenerateExercise_returns403() throws Exception {
        performPost(studentToken, "/api/ai/generate-exercise", Map.of(
                "prompt", "Generate something",
                "numberOfTestCases", 2
        ))
                .andExpect(status().isForbidden());
    }

    @Test
    void aiAskQuestion_returnsResponseWithRemainingRequests() throws Exception {
        performPost(studentToken, "/api/ai/ask", Map.of(
                "exerciseId", exerciseId,
                "question", "How do arrays work in Java?",
                "currentCode", "public class Solution { }"
        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response", notNullValue()))
                .andExpect(jsonPath("$.remainingRequests", greaterThanOrEqualTo(0)));
    }

    @Test
    void aiAskQuestion_withoutExerciseId_stillWorks() throws Exception {
        performPost(studentToken, "/api/ai/ask", Map.of(
                "question", "What is polymorphism?"
        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response", notNullValue()));
    }

    @Test
    void aiFeedback_marksInteractionHelpful() throws Exception {
        // First create an interaction
        performPost(studentToken, "/api/ai/ask", Map.of(
                "question", "How do I sort an array?"
        ))
                .andExpect(status().isOk());

        // Get the interaction ID from DB
        var interactions = aiInteractionRepository.findAll();
        assert !interactions.isEmpty();
        Long interactionId = interactions.get(0).getId();

        // Provide feedback
        performPost(studentToken, "/api/ai/feedback/" + interactionId + "?helpful=true", null)
                .andExpect(status().isOk());

        // Verify feedback was saved
        var updated = aiInteractionRepository.findById(interactionId).orElseThrow();
        assert Boolean.TRUE.equals(updated.getWasHelpful());
    }

    @Test
    void oversizedImage_returns400() throws Exception {
        // Create a large base64 string (>5MB when decoded)
        byte[] largeData = new byte[6 * 1024 * 1024];
        String largeBase64 = Base64.getEncoder().encodeToString(largeData);

        Map<String, Object> request = new java.util.HashMap<>();
        request.put("prompt", "Generate exercise from image");
        request.put("image", largeBase64);
        request.put("imageMediaType", "image/png");
        request.put("numberOfTestCases", 2);

        performPost(teacherToken, "/api/ai/generate-exercise", request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateExercise_withImage_callsVisionPath() throws Exception {
        setupGenerateExerciseMock();

        // Small valid base64 image
        byte[] smallData = new byte[100];
        String smallBase64 = Base64.getEncoder().encodeToString(smallData);

        Map<String, Object> request = new java.util.HashMap<>();
        request.put("prompt", "Generate exercise from this image");
        request.put("image", smallBase64);
        request.put("imageMediaType", "image/png");
        request.put("numberOfTestCases", 2);

        performPost(teacherToken, "/api/ai/generate-exercise", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", notNullValue()));
    }

    @Test
    void aiAskQuestion_rateLimitExceeded_returns400() throws Exception {
        // Make 10 requests (the hourly limit)
        for (int i = 0; i < 10; i++) {
            performPost(studentToken, "/api/ai/ask", Map.of(
                    "question", "Question " + i
            )).andExpect(status().isOk());
        }

        // 11th request should be rate limited
        performPost(studentToken, "/api/ai/ask", Map.of(
                "question", "One more question"
        )).andExpect(status().isBadRequest());
    }
}
