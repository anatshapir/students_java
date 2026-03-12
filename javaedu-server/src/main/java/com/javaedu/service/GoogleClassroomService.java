package com.javaedu.service;

import com.javaedu.exception.BadRequestException;
import com.javaedu.model.Course;
import com.javaedu.model.Exercise;
import com.javaedu.model.Grade;
import com.javaedu.model.User;
import com.javaedu.repository.CourseRepository;
import com.javaedu.repository.GradeRepository;
import com.javaedu.repository.UserRepository;
import com.javaedu.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleClassroomService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final GradeRepository gradeRepository;
    private final SubmissionRepository submissionRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${google.classroom.api-url:https://classroom.googleapis.com}")
    private String classroomApiUrl;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String clientSecret;

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";

    public List<ClassroomCourse> getCourses(String accessToken) {
        WebClient client = buildClient(accessToken);

        Map<String, Object> response = client.get()
                .uri("/v1/courses?teacherId=me")
                .retrieve()
                .bodyToMono(Map.class)
                .block(REQUEST_TIMEOUT);

        if (response == null || !response.containsKey("courses")) {
            return List.of();
        }

        List<Map<String, Object>> courses = (List<Map<String, Object>>) response.get("courses");

        return courses.stream()
                .map(c -> new ClassroomCourse(
                        (String) c.get("id"),
                        (String) c.get("name"),
                        (String) c.get("section"),
                        (String) c.get("descriptionHeading")
                ))
                .toList();
    }

    public List<ClassroomStudent> getStudents(String accessToken, String courseId) {
        WebClient client = buildClient(accessToken);

        Map<String, Object> response = client.get()
                .uri("/v1/courses/{courseId}/students", courseId)
                .retrieve()
                .bodyToMono(Map.class)
                .block(REQUEST_TIMEOUT);

        if (response == null || !response.containsKey("students")) {
            return List.of();
        }

        List<Map<String, Object>> students = (List<Map<String, Object>>) response.get("students");

        return students.stream()
                .map(s -> {
                    Map<String, Object> profile = (Map<String, Object>) s.get("profile");
                    Map<String, Object> name = (Map<String, Object>) profile.get("name");
                    return new ClassroomStudent(
                            (String) s.get("userId"),
                            (String) profile.get("emailAddress"),
                            (String) name.get("fullName")
                    );
                })
                .toList();
    }

    public int getStudentCount(String accessToken, String courseId) {
        return getStudents(accessToken, courseId).size();
    }

    @Transactional
    public SyncResult syncClassroomToCourse(String accessToken, String classroomId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BadRequestException("Course not found"));

        List<ClassroomStudent> classroomStudents = getStudents(accessToken, classroomId);
        Set<String> classroomEmails = classroomStudents.stream()
                .map(ClassroomStudent::email)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<User> existingStudents = course.getStudents();
        Set<String> existingEmails = existingStudents.stream()
                .map(User::getEmail)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        int added = 0;
        List<User> addedStudents = new ArrayList<>();

        for (ClassroomStudent student : classroomStudents) {
            String studentEmailLower = student.email().toLowerCase();
            if (!existingEmails.contains(studentEmailLower)) {
                User user = userRepository.findByGoogleId(student.userId())
                        .orElseGet(() -> userRepository.findByEmail(student.email())
                                .map(u -> {
                                    u.setGoogleId(student.userId());
                                    return userRepository.save(u);
                                })
                                .orElseGet(() -> {
                                    User newUser = User.builder()
                                            .email(student.email())
                                            .name(student.name())
                                            .googleId(student.userId())
                                            .role(User.Role.STUDENT)
                                            .isActive(true)
                                            .build();
                                    return userRepository.save(newUser);
                                }));

                course.getStudents().add(user);
                addedStudents.add(user);
                added++;
            }
        }

        course.setGoogleClassroomId(classroomId);
        course.setLastSyncedAt(LocalDateTime.now());
        courseRepository.save(course);

        log.info("Synced {} students from Google Classroom {} to course {}",
                added, classroomId, course.getName());

        return new SyncResult(added, 0, addedStudents, List.of());
    }

    @Transactional
    public SyncPreview previewSync(String accessToken, String classroomId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BadRequestException("Course not found"));

        List<ClassroomStudent> classroomStudents = getStudents(accessToken, classroomId);
        Set<String> classroomEmails = classroomStudents.stream()
                .map(ClassroomStudent::email)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Set<User> existingStudents = course.getStudents();
        Map<String, User> existingByEmail = existingStudents.stream()
                .collect(Collectors.toMap(u -> u.getEmail().toLowerCase(), u -> u));

        // Find students to add (in Google, not in JavaEdu)
        List<SyncStudentPreview> toAdd = new ArrayList<>();
        for (ClassroomStudent student : classroomStudents) {
            String emailLower = student.email().toLowerCase();
            if (!existingByEmail.containsKey(emailLower)) {
                toAdd.add(new SyncStudentPreview(
                        null,
                        student.name(),
                        student.email(),
                        0
                ));
            }
        }

        // Find students to remove (in JavaEdu, not in Google)
        List<SyncStudentPreview> toRemove = new ArrayList<>();
        for (User student : existingStudents) {
            String emailLower = student.getEmail().toLowerCase();
            if (!classroomEmails.contains(emailLower)) {
                long submissionCount = submissionRepository.countByUserIdAndCourseId(student.getId(), courseId);
                toRemove.add(new SyncStudentPreview(
                        student.getId(),
                        student.getName(),
                        student.getEmail(),
                        (int) submissionCount
                ));
            }
        }

        return new SyncPreview(toAdd, toRemove);
    }

    @Transactional
    public SyncResult applySyncChanges(String accessToken, String classroomId, Long courseId,
                                       List<String> emailsToAdd, List<Long> idsToRemove) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BadRequestException("Course not found"));

        List<ClassroomStudent> classroomStudents = getStudents(accessToken, classroomId);
        Map<String, ClassroomStudent> classroomByEmail = classroomStudents.stream()
                .collect(Collectors.toMap(s -> s.email().toLowerCase(), s -> s));

        List<User> addedStudents = new ArrayList<>();
        List<User> removedStudents = new ArrayList<>();

        // Add selected students
        for (String email : emailsToAdd) {
            String emailLower = email.toLowerCase();
            ClassroomStudent student = classroomByEmail.get(emailLower);
            if (student != null) {
                User user = userRepository.findByGoogleId(student.userId())
                        .orElseGet(() -> userRepository.findByEmail(student.email())
                                .map(u -> {
                                    u.setGoogleId(student.userId());
                                    return userRepository.save(u);
                                })
                                .orElseGet(() -> {
                                    User newUser = User.builder()
                                            .email(student.email())
                                            .name(student.name())
                                            .googleId(student.userId())
                                            .role(User.Role.STUDENT)
                                            .isActive(true)
                                            .build();
                                    return userRepository.save(newUser);
                                }));

                if (!course.getStudents().contains(user)) {
                    course.getStudents().add(user);
                    addedStudents.add(user);
                }
            }
        }

        // Remove selected students
        for (Long userId : idsToRemove) {
            course.getStudents().removeIf(s -> {
                if (s.getId().equals(userId)) {
                    removedStudents.add(s);
                    return true;
                }
                return false;
            });
        }

        course.setGoogleClassroomId(classroomId);
        course.setLastSyncedAt(LocalDateTime.now());
        courseRepository.save(course);

        log.info("Applied sync changes: added {}, removed {} students for course {}",
                addedStudents.size(), removedStudents.size(), course.getName());

        return new SyncResult(addedStudents.size(), removedStudents.size(), addedStudents, removedStudents);
    }

    public void exportGradesToClassroom(String accessToken, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BadRequestException("Course not found"));

        if (course.getGoogleClassroomId() == null) {
            throw new BadRequestException("Course is not linked to Google Classroom");
        }

        WebClient client = buildClient(accessToken);

        for (Exercise exercise : course.getExercises()) {
            String courseworkId = createOrGetCoursework(client, course.getGoogleClassroomId(), exercise);

            for (User student : course.getStudents()) {
                if (student.getGoogleId() == null) continue;

                Grade grade = gradeRepository
                        .findBestGradeByUserIdAndExerciseId(student.getId(), exercise.getId())
                        .orElse(null);

                if (grade != null) {
                    submitGrade(client, course.getGoogleClassroomId(), courseworkId,
                            student.getGoogleId(), grade.getScore(), grade.getMaxScore());
                }
            }
        }

        log.info("Exported grades to Google Classroom for course {}", course.getName());
    }

    public GradeExportPreview previewGradeExport(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BadRequestException("Course not found"));

        boolean isLinkedToGoogle = course.getGoogleClassroomId() != null;

        List<ExerciseExportPreview> exercises = new ArrayList<>();
        int totalStudentsWithGrades = 0;

        for (Exercise exercise : course.getExercises()) {
            int studentsWithGrades = 0;
            for (User student : course.getStudents()) {
                // For Google export, only count students with googleId
                // For CSV export (no Google link), count all students
                if (!isLinkedToGoogle || student.getGoogleId() != null) {
                    if (gradeRepository.findBestGradeByUserIdAndExerciseId(student.getId(), exercise.getId()).isPresent()) {
                        studentsWithGrades++;
                    }
                }
            }
            exercises.add(new ExerciseExportPreview(
                    exercise.getId(),
                    exercise.getTitle(),
                    studentsWithGrades
            ));
            totalStudentsWithGrades += studentsWithGrades;
        }

        return new GradeExportPreview(exercises, totalStudentsWithGrades);
    }

    public String refreshAccessToken(User user) {
        if (user.getGoogleRefreshToken() == null) {
            throw new BadRequestException("User not connected to Google");
        }

        WebClient tokenClient = webClientBuilder
                .baseUrl(GOOGLE_TOKEN_URL)
                .build();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("refresh_token", user.getGoogleRefreshToken());
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("grant_type", "refresh_token");

        try {
            Map<String, Object> response = tokenClient.post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(params)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(REQUEST_TIMEOUT);

            if (response == null) {
                throw new RuntimeException("Failed to refresh access token");
            }

            String accessToken = (String) response.get("access_token");
            long expiresIn = ((Number) response.get("expires_in")).longValue();

            // Update token expiry
            user.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn));
            userRepository.save(user);

            return accessToken;
        } catch (Exception e) {
            log.error("Failed to refresh Google token for user {}: {}", user.getId(), e.getMessage());
            throw new BadRequestException("Failed to refresh Google access token");
        }
    }

    public String getValidAccessToken(User user) {
        if (user.getGoogleRefreshToken() == null) {
            throw new BadRequestException("User not connected to Google");
        }

        // Check if we need to refresh
        if (user.getGoogleTokenExpiry() == null ||
            user.getGoogleTokenExpiry().isBefore(LocalDateTime.now().plusMinutes(5))) {
            return refreshAccessToken(user);
        }

        // Token is still valid, refresh it anyway since we don't store the access token
        return refreshAccessToken(user);
    }

    private String createOrGetCoursework(WebClient client, String classroomId, Exercise exercise) {
        // First, try to find existing coursework with the same title
        try {
            Map<String, Object> listResponse = client.get()
                    .uri("/v1/courses/{courseId}/courseWork", classroomId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(REQUEST_TIMEOUT);

            if (listResponse != null && listResponse.containsKey("courseWork")) {
                List<Map<String, Object>> existing = (List<Map<String, Object>>) listResponse.get("courseWork");
                for (Map<String, Object> cw : existing) {
                    if (exercise.getTitle().equals(cw.get("title"))) {
                        log.info("Found existing coursework for exercise: {}", exercise.getTitle());
                        return (String) cw.get("id");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not list existing coursework: {}", e.getMessage());
        }

        // Create new coursework if not found
        Map<String, Object> coursework = Map.of(
                "title", exercise.getTitle(),
                "description", exercise.getDescription() != null ? exercise.getDescription() : "",
                "maxPoints", exercise.getPoints(),
                "workType", "ASSIGNMENT",
                "state", "PUBLISHED"
        );

        try {
            Map<String, Object> response = client.post()
                    .uri("/v1/courses/{courseId}/courseWork", classroomId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(coursework)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(REQUEST_TIMEOUT);

            return response != null ? (String) response.get("id") : null;
        } catch (Exception e) {
            log.warn("Could not create coursework for exercise {}: {}", exercise.getTitle(), e.getMessage());
            return null;
        }
    }

    private void submitGrade(WebClient client, String classroomId, String courseworkId,
                             String studentId, int score, int maxScore) {
        if (courseworkId == null) return;

        double normalizedGrade = (double) score / maxScore * 100;

        Map<String, Object> submission = Map.of(
                "assignedGrade", normalizedGrade,
                "draftGrade", normalizedGrade
        );

        try {
            client.patch()
                    .uri("/v1/courses/{courseId}/courseWork/{courseworkId}/studentSubmissions/{studentId}",
                            classroomId, courseworkId, studentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(submission)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(REQUEST_TIMEOUT);
        } catch (Exception e) {
            log.warn("Could not submit grade for student {}: {}", studentId, e.getMessage());
        }
    }

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private WebClient buildClient(String accessToken) {
        return webClientBuilder
                .baseUrl(classroomApiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build();
    }

    /**
     * Scheduled task to auto-sync courses that have autoSyncEnabled=true.
     * Runs every 6 hours.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void autoSyncEnabledCourses() {
        List<Course> autoSyncCourses = courseRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getAutoSyncEnabled()))
                .filter(c -> c.getGoogleClassroomId() != null)
                .toList();

        if (autoSyncCourses.isEmpty()) {
            return;
        }

        log.info("Running auto-sync for {} courses", autoSyncCourses.size());

        for (Course course : autoSyncCourses) {
            try {
                User teacher = course.getTeacher();
                String accessToken = getValidAccessToken(teacher);
                SyncResult result = syncClassroomToCourse(accessToken, course.getGoogleClassroomId(), course.getId());
                if (result.added() > 0) {
                    log.info("Auto-sync added {} students to course '{}'", result.added(), course.getName());
                }
            } catch (Exception e) {
                log.error("Auto-sync failed for course '{}': {}", course.getName(), e.getMessage());
            }
        }
    }

    // Records for API responses
    public record ClassroomCourse(String id, String name, String section, String description) {}
    public record ClassroomStudent(String userId, String email, String name) {}

    public record SyncResult(
            int added,
            int removed,
            List<User> addedStudents,
            List<User> removedStudents
    ) {}

    public record SyncPreview(
            List<SyncStudentPreview> toAdd,
            List<SyncStudentPreview> toRemove
    ) {}

    public record SyncStudentPreview(
            Long id,
            String name,
            String email,
            int submissionCount
    ) {}

    public record GradeExportPreview(
            List<ExerciseExportPreview> exercises,
            int totalStudentsWithGrades
    ) {}

    public record ExerciseExportPreview(
            Long exerciseId,
            String exerciseTitle,
            int studentsWithGrades
    ) {}
}
