package com.javaedu.repository;

import com.javaedu.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void save_CreatesUser() {
        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .role(User.Role.STUDENT)
                .build();

        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertEquals("Test User", saved.getName());
        assertEquals("test@example.com", saved.getEmail());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void findByEmail_ExistingEmail_ReturnsUser() {
        User user = User.builder()
                .name("Test User")
                .email("findme@example.com")
                .passwordHash("hashedpassword")
                .role(User.Role.STUDENT)
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("findme@example.com");

        assertTrue(found.isPresent());
        assertEquals("Test User", found.get().getName());
    }

    @Test
    void findByEmail_NonexistentEmail_ReturnsEmpty() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertTrue(found.isEmpty());
    }

    @Test
    void existsByEmail_ExistingEmail_ReturnsTrue() {
        User user = User.builder()
                .name("Test User")
                .email("exists@example.com")
                .passwordHash("hashedpassword")
                .role(User.Role.STUDENT)
                .build();
        userRepository.save(user);

        assertTrue(userRepository.existsByEmail("exists@example.com"));
    }

    @Test
    void existsByEmail_NonexistentEmail_ReturnsFalse() {
        assertFalse(userRepository.existsByEmail("notexists@example.com"));
    }

    @Test
    void findByRole_ReturnsUsersWithRole() {
        User student1 = User.builder()
                .name("Student 1")
                .email("student1@example.com")
                .passwordHash("hash")
                .role(User.Role.STUDENT)
                .build();
        User student2 = User.builder()
                .name("Student 2")
                .email("student2@example.com")
                .passwordHash("hash")
                .role(User.Role.STUDENT)
                .build();
        User teacher = User.builder()
                .name("Teacher")
                .email("teacher@example.com")
                .passwordHash("hash")
                .role(User.Role.TEACHER)
                .build();

        userRepository.save(student1);
        userRepository.save(student2);
        userRepository.save(teacher);

        List<User> students = userRepository.findByRole(User.Role.STUDENT);
        List<User> teachers = userRepository.findByRole(User.Role.TEACHER);

        assertEquals(2, students.size());
        assertEquals(1, teachers.size());
    }

    @Test
    void findByGithubId_ExistingId_ReturnsUser() {
        User user = User.builder()
                .name("GitHub User")
                .email("github@example.com")
                .passwordHash("hash")
                .role(User.Role.STUDENT)
                .githubId("12345")
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByGithubId("12345");

        assertTrue(found.isPresent());
        assertEquals("GitHub User", found.get().getName());
    }

    @Test
    void findByGoogleId_ExistingId_ReturnsUser() {
        User user = User.builder()
                .name("Google User")
                .email("google@example.com")
                .passwordHash("hash")
                .role(User.Role.STUDENT)
                .googleId("google-123")
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByGoogleId("google-123");

        assertTrue(found.isPresent());
        assertEquals("Google User", found.get().getName());
    }
}
