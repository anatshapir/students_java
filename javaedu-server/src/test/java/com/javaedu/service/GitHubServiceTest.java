package com.javaedu.service;

import com.javaedu.exception.BadRequestException;
import com.javaedu.model.Course;
import com.javaedu.model.User;
import com.javaedu.repository.CourseRepository;
import com.javaedu.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class GitHubServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private GitHubService gitHubService;

    @BeforeEach
    void setUp() {
        gitHubService = new GitHubService(userRepository, courseRepository, webClientBuilder);

        // Set the @Value field via reflection
        ReflectionTestUtils.setField(gitHubService, "githubApiUrl", "https://api.github.com");

        // Set up WebClient chain with lenient stubbing
        lenient().when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        lenient().when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        lenient().when(webClientBuilder.build()).thenReturn(webClient);
        lenient().when(webClient.get()).thenReturn(requestHeadersUriSpec);
    }

    @Test
    void authenticateWithGitHub_NewUser_CreatesUser() {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", 12345);
        userInfo.put("email", "newuser@github.com");
        userInfo.put("name", "New User");

        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(userInfo));

        when(userRepository.findByGithubId("12345")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@github.com")).thenReturn(Optional.empty());

        User savedUser = User.builder()
                .id(1L)
                .email("newuser@github.com")
                .name("New User")
                .githubId("12345")
                .role(User.Role.STUDENT)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = gitHubService.authenticateWithGitHub("test-token");

        assertNotNull(result);
        assertEquals("newuser@github.com", result.getEmail());
        assertEquals("New User", result.getName());
        assertEquals("12345", result.getGithubId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void authenticateWithGitHub_ExistingUserByGitHubId_ReturnsUser() {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", 12345);
        userInfo.put("email", "existing@github.com");
        userInfo.put("name", "Existing User");

        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(userInfo));

        User existingUser = User.builder()
                .id(1L)
                .email("existing@github.com")
                .name("Existing User")
                .githubId("12345")
                .build();
        when(userRepository.findByGithubId("12345")).thenReturn(Optional.of(existingUser));

        User result = gitHubService.authenticateWithGitHub("test-token");

        assertNotNull(result);
        assertEquals(existingUser, result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void authenticateWithGitHub_ExistingUserByEmail_LinksGitHub() {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", 12345);
        userInfo.put("email", "existing@github.com");
        userInfo.put("name", "Existing User");

        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(userInfo));

        User existingUser = User.builder()
                .id(1L)
                .email("existing@github.com")
                .name("Existing User")
                .build();
        when(userRepository.findByGithubId("12345")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@github.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        User result = gitHubService.authenticateWithGitHub("test-token");

        assertNotNull(result);
        assertEquals("12345", result.getGithubId());
        verify(userRepository).save(existingUser);
    }

    @Test
    void authenticateWithGitHub_UsesLoginAsName_WhenNameNull() {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", 12345);
        userInfo.put("email", "user@github.com");
        userInfo.put("name", null);
        userInfo.put("login", "username123");

        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(userInfo));

        when(userRepository.findByGithubId("12345")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@github.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = gitHubService.authenticateWithGitHub("test-token");

        assertNotNull(result);
        assertEquals("username123", result.getName());
    }

    @Test
    void authenticateWithGitHub_FetchesEmailFromEndpoint_WhenEmailNull() {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", 12345);
        userInfo.put("email", null);
        userInfo.put("name", "User");

        List<Map<String, Object>> emails = List.of(
                Map.of("email", "secondary@github.com", "primary", false),
                Map.of("email", "primary@github.com", "primary", true)
        );

        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(userInfo));
        when(responseSpec.bodyToMono(List.class)).thenReturn(Mono.just(emails));

        when(userRepository.findByGithubId("12345")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("primary@github.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = gitHubService.authenticateWithGitHub("test-token");

        assertNotNull(result);
        assertEquals("primary@github.com", result.getEmail());
    }

    @Test
    void authenticateWithGitHub_NoEmail_ThrowsException() {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", 12345);
        userInfo.put("email", null);
        userInfo.put("name", "User");

        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(userInfo));
        when(responseSpec.bodyToMono(List.class)).thenReturn(Mono.just(List.of()));

        assertThrows(BadRequestException.class, () ->
                gitHubService.authenticateWithGitHub("test-token"));
    }

    @Test
    void authenticateWithGitHub_NullUserInfo_ThrowsException() {
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.empty());

        assertThrows(BadRequestException.class, () ->
                gitHubService.authenticateWithGitHub("test-token"));
    }

    @Test
    void getOrganizationMembers_ReturnsMembers() {
        List<Map<String, Object>> members = List.of(
                Map.of("id", 1, "login", "user1", "avatar_url", "https://avatar1.com"),
                Map.of("id", 2, "login", "user2", "avatar_url", "https://avatar2.com")
        );

        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(List.class)).thenReturn(Mono.just(members));

        List<GitHubService.GitHubMember> result = gitHubService.getOrganizationMembers("token", "test-org");

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).id());
        assertEquals("user1", result.get(0).login());
        assertEquals("2", result.get(1).id());
        assertEquals("user2", result.get(1).login());
    }

    @Test
    void getOrganizationMembers_NullResponse_ReturnsEmptyList() {
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(List.class)).thenReturn(Mono.empty());

        List<GitHubService.GitHubMember> result = gitHubService.getOrganizationMembers("token", "test-org");

        assertTrue(result.isEmpty());
    }

    @Test
    void syncOrganizationToCourse_SyncsMembers() {
        User teacher = User.builder().id(1L).name("Teacher").role(User.Role.TEACHER).build();
        Course course = Course.builder()
                .id(1L)
                .name("Test Course")
                .teacher(teacher)
                .students(new HashSet<>())
                .build();

        User existingStudent = User.builder()
                .id(2L)
                .name("Student")
                .githubId("123")
                .build();

        List<Map<String, Object>> members = List.of(
                Map.of("id", 123, "login", "student1", "avatar_url", "https://avatar.com")
        );

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(List.class)).thenReturn(Mono.just(members));
        when(userRepository.findByGithubId("123")).thenReturn(Optional.of(existingStudent));
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        gitHubService.syncOrganizationToCourse("token", "test-org", 1L);

        verify(courseRepository).save(course);
        assertEquals("test-org", course.getGithubOrg());
        assertTrue(course.getStudents().contains(existingStudent));
    }

    @Test
    void syncOrganizationToCourse_CourseNotFound_ThrowsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () ->
                gitHubService.syncOrganizationToCourse("token", "test-org", 999L));
    }
}
