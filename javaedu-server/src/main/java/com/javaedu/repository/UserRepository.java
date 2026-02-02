package com.javaedu.repository;

import com.javaedu.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGithubId(String githubId);

    Optional<User> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

    List<User> findByRole(User.Role role);

    @Query("SELECT u FROM User u JOIN u.enrolledCourses c WHERE c.id = :courseId")
    List<User> findStudentsByCourseId(Long courseId);

    @Query("SELECT u FROM User u WHERE u.role = 'TEACHER' AND u.isActive = true")
    List<User> findActiveTeachers();
}
