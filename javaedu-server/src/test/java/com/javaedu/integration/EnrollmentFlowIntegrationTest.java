package com.javaedu.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.javaedu.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EnrollmentFlowIntegrationTest extends BaseIntegrationTest {

    private String teacherToken;
    private String studentToken;
    private String student2Token;
    private Long courseId;

    @BeforeEach
    void setUpUsers() throws Exception {
        teacherToken = registerAndLogin("Teacher", "teacher@test.com", "password123", User.Role.TEACHER);
        studentToken = registerAndLogin("Student", "student@test.com", "password123", User.Role.STUDENT);
        student2Token = registerAndLogin("Student2", "student2@test.com", "password123", User.Role.STUDENT);

        JsonNode course = createCourseViaApi(teacherToken, "Java 101", "Intro to Java");
        courseId = course.get("id").asLong();
    }

    @Test
    void teacher_getsEnrollmentCode() throws Exception {
        performGet(teacherToken, "/api/courses/" + courseId + "/enrollment-code")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", notNullValue()))
                .andExpect(jsonPath("$.enrollmentUrl", notNullValue()));
    }

    @Test
    void teacher_regeneratesEnrollmentCode() throws Exception {
        MvcResult first = performGet(teacherToken, "/api/courses/" + courseId + "/enrollment-code")
                .andExpect(status().isOk())
                .andReturn();
        String firstCode = parseResponse(first).get("code").asText();

        MvcResult second = performPost(teacherToken,
                "/api/courses/" + courseId + "/enrollment-code/regenerate", null)
                .andExpect(status().isOk())
                .andReturn();
        String secondCode = parseResponse(second).get("code").asText();

        assert !firstCode.equals(secondCode) : "Regenerated code should be different";
    }

    @Test
    void student_lookupCourseByCode() throws Exception {
        MvcResult codeResult = performGet(teacherToken, "/api/courses/" + courseId + "/enrollment-code")
                .andReturn();
        String code = parseResponse(codeResult).get("code").asText();

        performGet(studentToken, "/api/enroll/" + code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Java 101")))
                .andExpect(jsonPath("$.teacherName", is("Teacher")));
    }

    @Test
    void student_requestsEnrollment_thenTeacherApproves() throws Exception {
        String code = getEnrollmentCode();

        // Student requests enrollment
        performPost(studentToken, "/api/enroll/" + code, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING")));

        // Teacher sees pending request
        performGet(teacherToken, "/api/courses/" + courseId + "/enrollment-requests")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userName", is("Student")));

        // Get request ID
        MvcResult reqResult = performGet(teacherToken, "/api/courses/" + courseId + "/enrollment-requests")
                .andReturn();
        Long requestId = parseResponse(reqResult).get(0).get("id").asLong();

        // Teacher approves
        performPost(teacherToken, "/api/enrollment-requests/" + requestId + "/approve", null)
                .andExpect(status().isOk());

        // Verify student is in course
        performGet(teacherToken, "/api/courses/" + courseId + "/students")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email", is("student@test.com")));
    }

    @Test
    void teacher_deniesEnrollment_studentNotInCourse() throws Exception {
        String code = getEnrollmentCode();

        performPost(studentToken, "/api/enroll/" + code, null)
                .andExpect(status().isOk());

        MvcResult reqResult = performGet(teacherToken, "/api/courses/" + courseId + "/enrollment-requests")
                .andReturn();
        Long requestId = parseResponse(reqResult).get(0).get("id").asLong();

        performPost(teacherToken, "/api/enrollment-requests/" + requestId + "/deny", null)
                .andExpect(status().isOk());

        // Verify student is NOT in course
        performGet(teacherToken, "/api/courses/" + courseId + "/students")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void duplicateEnrollmentRequest_returns400() throws Exception {
        String code = getEnrollmentCode();

        performPost(studentToken, "/api/enroll/" + code, null)
                .andExpect(status().isOk());

        performPost(studentToken, "/api/enroll/" + code, null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void bulkEnroll_byEmail_createsNewAccounts() throws Exception {
        performPost(teacherToken, "/api/courses/" + courseId + "/students/bulk", Map.of(
                "emails", List.of("new1@test.com", "new2@test.com", "student@test.com")
        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.added", is(3)));

        // Verify new users were created
        assert userRepository.findByEmail("new1@test.com").isPresent();
        assert userRepository.findByEmail("new2@test.com").isPresent();
    }

    @Test
    void bulkApprove_multipleRequests() throws Exception {
        String code = getEnrollmentCode();

        performPost(studentToken, "/api/enroll/" + code, null)
                .andExpect(status().isOk());
        performPost(student2Token, "/api/enroll/" + code, null)
                .andExpect(status().isOk());

        MvcResult reqResult = performGet(teacherToken, "/api/courses/" + courseId + "/enrollment-requests")
                .andReturn();
        JsonNode requests = parseResponse(reqResult);
        Long id1 = requests.get(0).get("id").asLong();
        Long id2 = requests.get(1).get("id").asLong();

        performPost(teacherToken, "/api/enrollment-requests/bulk-approve",
                Map.of("requestIds", List.of(id1, id2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.succeeded", is(2)))
                .andExpect(jsonPath("$.failed", is(0)));

        // Both students should be enrolled
        performGet(teacherToken, "/api/courses/" + courseId + "/students")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void bulkDeny_multipleRequests() throws Exception {
        String code = getEnrollmentCode();

        performPost(studentToken, "/api/enroll/" + code, null)
                .andExpect(status().isOk());
        performPost(student2Token, "/api/enroll/" + code, null)
                .andExpect(status().isOk());

        MvcResult reqResult = performGet(teacherToken, "/api/courses/" + courseId + "/enrollment-requests")
                .andReturn();
        JsonNode requests = parseResponse(reqResult);
        Long id1 = requests.get(0).get("id").asLong();
        Long id2 = requests.get(1).get("id").asLong();

        performPost(teacherToken, "/api/enrollment-requests/bulk-deny",
                Map.of("requestIds", List.of(id1, id2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.succeeded", is(2)));

        // No students enrolled
        performGet(teacherToken, "/api/courses/" + courseId + "/students")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void pendingCount_returnsCorrectCount() throws Exception {
        String code = getEnrollmentCode();

        performPost(studentToken, "/api/enroll/" + code, null)
                .andExpect(status().isOk());
        performPost(student2Token, "/api/enroll/" + code, null)
                .andExpect(status().isOk());

        performGet(teacherToken, "/api/enrollment-requests/pending/count")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(2)));
    }

    @Test
    void getAllPendingRequests_forTeacher() throws Exception {
        String code = getEnrollmentCode();

        performPost(studentToken, "/api/enroll/" + code, null)
                .andExpect(status().isOk());

        performGet(teacherToken, "/api/enrollment-requests/pending")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));
    }

    @Test
    void student_cannotAccessEnrollmentCode_returns403() throws Exception {
        performGet(studentToken, "/api/courses/" + courseId + "/enrollment-code")
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidCode_returns404() throws Exception {
        performGet(studentToken, "/api/enroll/INVALIDCODE")
                .andExpect(status().isNotFound());
    }

    @Test
    void student_cannotApproveEnrollment_returns403() throws Exception {
        performPost(studentToken, "/api/enrollment-requests/1/approve", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void alreadyEnrolledStudent_cannotRequestEnrollment_returns400() throws Exception {
        String code = getEnrollmentCode();

        // Request and approve enrollment
        performPost(studentToken, "/api/enroll/" + code, null)
                .andExpect(status().isOk());

        MvcResult reqResult = performGet(teacherToken, "/api/courses/" + courseId + "/enrollment-requests")
                .andReturn();
        Long requestId = parseResponse(reqResult).get(0).get("id").asLong();

        performPost(teacherToken, "/api/enrollment-requests/" + requestId + "/approve", null)
                .andExpect(status().isOk());

        // Try to request enrollment again while already enrolled
        performPost(studentToken, "/api/enroll/" + code, null)
                .andExpect(status().isBadRequest());
    }

    private String getEnrollmentCode() throws Exception {
        MvcResult codeResult = performGet(teacherToken, "/api/courses/" + courseId + "/enrollment-code")
                .andExpect(status().isOk())
                .andReturn();
        return parseResponse(codeResult).get("code").asText();
    }
}
