package com.javaedu.service;

import com.javaedu.exception.BadRequestException;
import com.javaedu.model.Course;
import com.javaedu.model.User;
import com.javaedu.repository.CourseRepository;
import com.javaedu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${github.api-url:https://api.github.com}")
    private String githubApiUrl;

    public User authenticateWithGitHub(String accessToken) {
        WebClient client = webClientBuilder
                .baseUrl(githubApiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();

        Map<String, Object> userInfo = client.get()
                .uri("/user")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (userInfo == null) {
            throw new BadRequestException("Failed to get GitHub user info");
        }

        String githubId = String.valueOf(userInfo.get("id"));
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");

        if (name == null) {
            name = (String) userInfo.get("login");
        }

        if (email == null) {
            List<Map<String, Object>> emails = client.get()
                    .uri("/user/emails")
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();

            if (emails != null && !emails.isEmpty()) {
                email = emails.stream()
                        .filter(e -> Boolean.TRUE.equals(e.get("primary")))
                        .map(e -> (String) e.get("email"))
                        .findFirst()
                        .orElse((String) emails.get(0).get("email"));
            }
        }

        if (email == null) {
            throw new BadRequestException("Could not retrieve email from GitHub");
        }

        final String finalEmail = email;
        final String finalName = name;

        return userRepository.findByGithubId(githubId)
                .orElseGet(() -> userRepository.findByEmail(finalEmail)
                        .map(user -> {
                            user.setGithubId(githubId);
                            return userRepository.save(user);
                        })
                        .orElseGet(() -> {
                            User newUser = User.builder()
                                    .email(finalEmail)
                                    .name(finalName)
                                    .githubId(githubId)
                                    .role(User.Role.STUDENT)
                                    .isActive(true)
                                    .build();
                            return userRepository.save(newUser);
                        }));
    }

    public List<GitHubMember> getOrganizationMembers(String accessToken, String orgName) {
        WebClient client = webClientBuilder
                .baseUrl(githubApiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();

        List<Map<String, Object>> members = client.get()
                .uri("/orgs/{org}/members", orgName)
                .retrieve()
                .bodyToMono(List.class)
                .block();

        if (members == null) {
            return List.of();
        }

        return members.stream()
                .map(m -> new GitHubMember(
                        String.valueOf(m.get("id")),
                        (String) m.get("login"),
                        (String) m.get("avatar_url")
                ))
                .toList();
    }

    public void syncOrganizationToCourse(String accessToken, String orgName, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BadRequestException("Course not found"));

        List<GitHubMember> members = getOrganizationMembers(accessToken, orgName);

        for (GitHubMember member : members) {
            userRepository.findByGithubId(member.id())
                    .ifPresent(user -> {
                        if (!course.getStudents().contains(user)) {
                            course.getStudents().add(user);
                        }
                    });
        }

        course.setGithubOrg(orgName);
        courseRepository.save(course);

        log.info("Synced {} members from GitHub org {} to course {}",
                members.size(), orgName, course.getName());
    }

    public record GitHubMember(String id, String login, String avatarUrl) {}
}
