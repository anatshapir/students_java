package com.javaedu.service;

import com.javaedu.exception.BadRequestException;
import com.javaedu.model.Course;
import com.javaedu.model.Exercise;
import com.javaedu.model.Grade;
import com.javaedu.model.User;
import com.javaedu.repository.CourseRepository;
import com.javaedu.repository.GradeRepository;
import com.javaedu.repository.SubmissionRepository;
import com.javaedu.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GoogleClassroomServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private GoogleClassroomService googleClassroomService;

    @BeforeEach
    void setUp() {
        googleClassroomService = new GoogleClassroomService(
                courseRepository, userRepository, gradeRepository, submissionRepository, webClientBuilder);

        // Set the @Value field via reflection
        ReflectionTestUtils.setField(googleClassroomService, "classroomApiUrl", "https://classroom.googleapis.com");

        // Set up WebClient chain for GET requests
        lenient().when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        lenient().when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        lenient().when(webClientBuilder.build()).thenReturn(webClient);
        lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
    }

    @Test
    void getCourses_ReturnsCourses() {
        Map<String, Object> response = new HashMap<>();
        response.put("courses", List.of(
                Map.of("id", "course1", "name", "Java 101", "section", "A", "descriptionHeading", "Intro to Java"),
                Map.of("id", "course2", "name", "Java 102", "section", "B", "descriptionHeading", "Advanced Java")
        ));

        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        List<GoogleClassroomService.ClassroomCourse> result = googleClassroomService.getCourses("token");

        assertEquals(2, result.size());
        assertEquals("course1", result.get(0).id());
        assertEquals("Java 101", result.get(0).name());
        assertEquals("A", result.get(0).section());
        assertEquals("course2", result.get(1).id());
        assertEquals("Java 102", result.get(1).name());
    }

    @Test
    void getCourses_NoCourses_ReturnsEmptyList() {
        Map<String, Object> response = new HashMap<>();
        // No "courses" key

        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        List<GoogleClassroomService.ClassroomCourse> result = googleClassroomService.getCourses("token");

        assertTrue(result.isEmpty());
    }

    @Test
    void getCourses_NullResponse_ReturnsEmptyList() {
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.empty());

        List<GoogleClassroomService.ClassroomCourse> result = googleClassroomService.getCourses("token");

        assertTrue(result.isEmpty());
    }

    @Test
    void getStudents_ReturnsStudents() {
        Map<String, Object> response = new HashMap<>();
        response.put("students", List.of(
                Map.of(
                        "userId", "student1",
                        "profile", Map.of(
                                "emailAddress", "student1@school.edu",
                                "name", Map.of("fullName", "Student One")
                        )
                ),
                Map.of(
                        "userId", "student2",
                        "profile", Map.of(
                                "emailAddress", "student2@school.edu",
                                "name", Map.of("fullName", "Student Two")
                        )
                )
        ));

        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        List<GoogleClassroomService.ClassroomStudent> result = googleClassroomService.getStudents("token", "course1");

        assertEquals(2, result.size());
        assertEquals("student1", result.get(0).userId());
        assertEquals("student1@school.edu", result.get(0).email());
        assertEquals("Student One", result.get(0).name());
    }

    @Test
    void getStudents_NoStudents_ReturnsEmptyList() {
        Map<String, Object> response = new HashMap<>();

        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));

        List<GoogleClassroomService.ClassroomStudent> result = googleClassroomService.getStudents("token", "course1");

        assertTrue(result.isEmpty());
    }

    @Test
    void syncClassroomToCourse_NewStudents_CreatesAndAdds() {
        User teacher = User.builder().id(1L).name("Teacher").role(User.Role.TEACHER).build();
        Course course = Course.builder()
                .id(1L)
                .name("Test Course")
                .teacher(teacher)
                .students(new HashSet<>())
                .build();

        Map<String, Object> response = new HashMap<>();
        response.put("students", List.of(
                Map.of(
                        "userId", "gstudent1",
                        "profile", Map.of(
                                "emailAddress", "student1@school.edu",
                                "name", Map.of("fullName", "New Student")
                        )
                )
        ));

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));
        when(userRepository.findByGoogleId("gstudent1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("student1@school.edu")).thenReturn(Optional.empty());

        User newUser = User.builder()
                .id(2L)
                .email("student1@school.edu")
                .name("New Student")
                .googleId("gstudent1")
                .role(User.Role.STUDENT)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        googleClassroomService.syncClassroomToCourse("token", "classroom1", 1L);

        verify(userRepository).save(any(User.class));
        verify(courseRepository).save(course);
        assertEquals("classroom1", course.getGoogleClassroomId());
    }

    @Test
    void syncClassroomToCourse_ExistingUserByGoogleId_AddsToCourseCourse() {
        User teacher = User.builder().id(1L).name("Teacher").role(User.Role.TEACHER).build();
        Course course = Course.builder()
                .id(1L)
                .name("Test Course")
                .teacher(teacher)
                .students(new HashSet<>())
                .build();

        User existingUser = User.builder()
                .id(2L)
                .email("student@school.edu")
                .name("Existing Student")
                .googleId("gstudent1")
                .build();

        Map<String, Object> response = new HashMap<>();
        response.put("students", List.of(
                Map.of(
                        "userId", "gstudent1",
                        "profile", Map.of(
                                "emailAddress", "student@school.edu",
                                "name", Map.of("fullName", "Existing Student")
                        )
                )
        ));

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));
        when(userRepository.findByGoogleId("gstudent1")).thenReturn(Optional.of(existingUser));
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        googleClassroomService.syncClassroomToCourse("token", "classroom1", 1L);

        verify(userRepository, never()).save(any(User.class));
        assertTrue(course.getStudents().contains(existingUser));
    }

    @Test
    void syncClassroomToCourse_ExistingUserByEmail_LinksGoogleId() {
        User teacher = User.builder().id(1L).name("Teacher").role(User.Role.TEACHER).build();
        Course course = Course.builder()
                .id(1L)
                .name("Test Course")
                .teacher(teacher)
                .students(new HashSet<>())
                .build();

        User existingUser = User.builder()
                .id(2L)
                .email("student@school.edu")
                .name("Existing Student")
                .build();

        Map<String, Object> response = new HashMap<>();
        response.put("students", List.of(
                Map.of(
                        "userId", "gstudent1",
                        "profile", Map.of(
                                "emailAddress", "student@school.edu",
                                "name", Map.of("fullName", "Existing Student")
                        )
                )
        ));

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(response));
        when(userRepository.findByGoogleId("gstudent1")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("student@school.edu")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        googleClassroomService.syncClassroomToCourse("token", "classroom1", 1L);

        verify(userRepository).save(existingUser);
        assertEquals("gstudent1", existingUser.getGoogleId());
    }

    @Test
    void syncClassroomToCourse_CourseNotFound_ThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () ->
                googleClassroomService.syncClassroomToCourse("token", "classroom1", 999L));
    }

    @Test
    void exportGradesToClassroom_CourseNotLinked_ThrowsException() {
        User teacher = User.builder().id(1L).name("Teacher").role(User.Role.TEACHER).build();
        Course course = Course.builder()
                .id(1L)
                .name("Test Course")
                .teacher(teacher)
                .googleClassroomId(null)  // Not linked
                .build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThrows(BadRequestException.class, () ->
                googleClassroomService.exportGradesToClassroom("token", 1L));
    }

    @Test
    void exportGradesToClassroom_CourseNotFound_ThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () ->
                googleClassroomService.exportGradesToClassroom("token", 999L));
    }

    @Test
    void exportGradesToClassroom_WithGrades_ExportsSuccessfully() {
        User teacher = User.builder().id(1L).name("Teacher").role(User.Role.TEACHER).build();
        User student = User.builder()
                .id(2L)
                .name("Student")
                .googleId("gstudent1")
                .build();

        Exercise exercise = Exercise.builder()
                .id(1L)
                .title("Exercise 1")
                .description("Test exercise")
                .points(100)
                .build();

        Course course = Course.builder()
                .id(1L)
                .name("Test Course")
                .teacher(teacher)
                .googleClassroomId("classroom1")
                .students(new HashSet<>(Set.of(student)))
                .exercises(new HashSet<>(Set.of(exercise)))
                .build();

        Grade grade = Grade.builder()
                .id(1L)
                .score(85)
                .maxScore(100)
                .build();

        // Mock POST for creating coursework
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of("id", "coursework1")));

        // Mock PATCH for submitting grades
        when(webClient.patch()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString(), anyString(), anyString(), anyString())).thenReturn(requestBodySpec);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(gradeRepository.findBestGradeByUserIdAndExerciseId(2L, 1L)).thenReturn(Optional.of(grade));

        googleClassroomService.exportGradesToClassroom("token", 1L);

        verify(webClient).post();  // Create coursework
        verify(webClient).patch(); // Submit grade
    }
}
