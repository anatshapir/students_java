package com.javaedu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaedu.dto.submission.SubmitCodeRequest;
import com.javaedu.model.*;
import com.javaedu.repository.CourseRepository;
import com.javaedu.repository.ExerciseRepository;
import com.javaedu.repository.StudentAnalyticsRepository;
import com.javaedu.repository.SubmissionRepository;
import com.javaedu.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private StudentAnalyticsRepository studentAnalyticsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String studentToken;
    private String teacherToken;
    private User student;
    private User teacher;
    private Course course;
    private Exercise exercise;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up in correct order (respect foreign key constraints)
        studentAnalyticsRepository.deleteAll();
        submissionRepository.deleteAll();
        exerciseRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();

        // Create teacher
        teacher = User.builder()
                .name("Test Teacher")
                .email("teacher@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.TEACHER)
                .build();
        teacher = userRepository.save(teacher);

        // Create student
        student = User.builder()
                .name("Test Student")
                .email("student@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.STUDENT)
                .build();
        student = userRepository.save(student);

        // Create course
        course = Course.builder()
                .name("Java 101")
                .description("Introduction to Java")
                .teacher(teacher)
                .isActive(true)
                .build();
        course = courseRepository.save(course);

        // Create exercise
        exercise = Exercise.builder()
                .title("Hello World")
                .description("Write a class that prints Hello World")
                .course(course)
                .difficulty(Exercise.Difficulty.BEGINNER)
                .points(100)
                .isPublished(true)
                .build();
        exercise = exerciseRepository.save(exercise);

        // Get tokens
        studentToken = getAuthToken("student@test.com", "password123");
        teacherToken = getAuthToken("teacher@test.com", "password123");
    }

    private String getAuthToken(String email, String password) throws Exception {
        String loginJson = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    void submitCode_WithValidCode_ReturnsSubmission() throws Exception {
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setExerciseId(exercise.getId());
        request.setCode("""
            public class HelloWorld {
                public static void main(String[] args) {
                    System.out.println("Hello World");
                }
            }
            """);

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.exerciseId", is(exercise.getId().intValue())))
                // Status can be PENDING, COMPLETED, or COMPILATION_ERROR depending on processing timing
                .andExpect(jsonPath("$.status", notNullValue()));
    }

    @Test
    void submitCode_WithoutToken_ReturnsUnauthorized() throws Exception {
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setExerciseId(exercise.getId());
        request.setCode("public class Test { }");

        mockMvc.perform(post("/api/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitCode_WithMissingExerciseId_ReturnsBadRequest() throws Exception {
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setCode("public class Test { }");

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitCode_WithMissingCode_ReturnsBadRequest() throws Exception {
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setExerciseId(exercise.getId());

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitCode_WithNonExistentExercise_ReturnsNotFound() throws Exception {
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setExerciseId(99999L);
        request.setCode("public class Test { }");

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMySubmissions_ReturnsUserSubmissions() throws Exception {
        // First create a submission
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setExerciseId(exercise.getId());
        request.setCode("public class Test { }");

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then get submissions
        mockMvc.perform(get("/api/submissions/my")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getMySubmissionsForExercise_ReturnsExerciseSubmissions() throws Exception {
        // Create submission
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setExerciseId(exercise.getId());
        request.setCode("public class Test { }");

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Get submissions for exercise
        mockMvc.perform(get("/api/submissions/my/exercise/" + exercise.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].exerciseId", is(exercise.getId().intValue())));
    }

    @Test
    void getSubmissionCount_ReturnsCount() throws Exception {
        // Create submissions
        for (int i = 0; i < 3; i++) {
            SubmitCodeRequest request = new SubmitCodeRequest();
            request.setExerciseId(exercise.getId());
            request.setCode("public class Test" + i + " { }");

            mockMvc.perform(post("/api/submissions")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/submissions/my/exercise/" + exercise.getId() + "/count")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    void getSubmissionsForExercise_AsTeacher_ReturnsAllSubmissions() throws Exception {
        // Create submission as student
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setExerciseId(exercise.getId());
        request.setCode("public class Test { }");

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Get all submissions as teacher
        mockMvc.perform(get("/api/submissions/exercise/" + exercise.getId())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getSubmissionsForExercise_AsStudent_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/submissions/exercise/" + exercise.getId())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSubmissionsForUser_AsTeacher_ReturnsUserSubmissions() throws Exception {
        // Create submission as student
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setExerciseId(exercise.getId());
        request.setCode("public class Test { }");

        mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Get student's submissions as teacher
        mockMvc.perform(get("/api/submissions/user/" + student.getId())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getSubmission_ReturnsSubmissionDetails() throws Exception {
        // Create submission
        SubmitCodeRequest request = new SubmitCodeRequest();
        request.setExerciseId(exercise.getId());
        request.setCode("public class Test { }");

        MvcResult result = mockMvc.perform(post("/api/submissions")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Long submissionId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asLong();

        // Get submission by ID
        mockMvc.perform(get("/api/submissions/" + submissionId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(submissionId.intValue())))
                .andExpect(jsonPath("$.code", is("public class Test { }")));
    }
}
