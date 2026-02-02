package com.javaedu.config;

import com.javaedu.model.Course;
import com.javaedu.model.User;
import com.javaedu.repository.CourseRepository;
import com.javaedu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.UUID;

@Configuration
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer {

    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initDevData(UserRepository userRepository, CourseRepository courseRepository) {
        return args -> {
            // Create test teacher if not exists
            if (userRepository.findByEmail("teacher@test.com").isEmpty()) {
                User teacher = User.builder()
                        .name("Test Teacher")
                        .email("teacher@test.com")
                        .passwordHash(passwordEncoder.encode("password"))
                        .role(User.Role.TEACHER)
                        .build();
                teacher = userRepository.save(teacher);
                log.info("Created test teacher: teacher@test.com / password");

                // Create sample courses
                Course course1 = Course.builder()
                        .name("Introduction to Java")
                        .description("Learn the fundamentals of Java programming")
                        .teacher(teacher)
                        .students(new HashSet<>())
                        .exercises(new HashSet<>())
                        .enrollmentCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .isActive(true)
                        .build();
                courseRepository.save(course1);

                Course course2 = Course.builder()
                        .name("Data Structures")
                        .description("Master arrays, lists, trees, and graphs")
                        .teacher(teacher)
                        .students(new HashSet<>())
                        .exercises(new HashSet<>())
                        .enrollmentCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .isActive(true)
                        .build();
                courseRepository.save(course2);

                Course course3 = Course.builder()
                        .name("Advanced Java (Archived)")
                        .description("Advanced topics in Java")
                        .teacher(teacher)
                        .students(new HashSet<>())
                        .exercises(new HashSet<>())
                        .enrollmentCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .isActive(false)
                        .build();
                courseRepository.save(course3);

                log.info("Created 3 sample courses");
            }

            // Create test student if not exists
            if (userRepository.findByEmail("student@test.com").isEmpty()) {
                User student = User.builder()
                        .name("Test Student")
                        .email("student@test.com")
                        .passwordHash(passwordEncoder.encode("password"))
                        .role(User.Role.STUDENT)
                        .build();
                userRepository.save(student);
                log.info("Created test student: student@test.com / password");
            }

            log.info("===========================================");
            log.info("DEV MODE - Test accounts:");
            log.info("  Teacher: teacher@test.com / password");
            log.info("  Student: student@test.com / password");
            log.info("===========================================");
        };
    }
}
