package com.javaedu.repository;

import com.javaedu.model.Course;
import com.javaedu.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class CourseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    private User teacher;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
        userRepository.deleteAll();

        teacher = User.builder()
                .name("Test Teacher")
                .email("teacher@example.com")
                .passwordHash("hash")
                .role(User.Role.TEACHER)
                .build();
        teacher = userRepository.save(teacher);
    }

    @Test
    void save_CreatesCourse() {
        Course course = Course.builder()
                .name("Java 101")
                .description("Introduction to Java")
                .teacher(teacher)
                .build();

        Course saved = courseRepository.save(course);

        assertNotNull(saved.getId());
        assertEquals("Java 101", saved.getName());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void findByTeacherId_ReturnsCourses() {
        Course course1 = Course.builder()
                .name("Java 101")
                .teacher(teacher)
                .build();
        Course course2 = Course.builder()
                .name("Java 201")
                .teacher(teacher)
                .build();

        courseRepository.save(course1);
        courseRepository.save(course2);

        List<Course> courses = courseRepository.findByTeacherId(teacher.getId());

        assertEquals(2, courses.size());
    }

    @Test
    void findByStudentId_ReturnsEnrolledCourses() {
        User student = User.builder()
                .name("Test Student")
                .email("student@example.com")
                .passwordHash("hash")
                .role(User.Role.STUDENT)
                .build();
        student = userRepository.save(student);

        Course course = Course.builder()
                .name("Java 101")
                .teacher(teacher)
                .build();
        course.getStudents().add(student);
        courseRepository.save(course);

        List<Course> courses = courseRepository.findByStudentId(student.getId());

        assertEquals(1, courses.size());
        assertEquals("Java 101", courses.get(0).getName());
    }

    @Test
    void findByIsActiveTrue_ReturnsOnlyActiveCourses() {
        Course activeCourse = Course.builder()
                .name("Active Course")
                .teacher(teacher)
                .isActive(true)
                .build();
        Course inactiveCourse = Course.builder()
                .name("Inactive Course")
                .teacher(teacher)
                .isActive(false)
                .build();

        courseRepository.save(activeCourse);
        courseRepository.save(inactiveCourse);

        List<Course> activeCourses = courseRepository.findByIsActiveTrue();

        assertEquals(1, activeCourses.size());
        assertEquals("Active Course", activeCourses.get(0).getName());
    }
}
