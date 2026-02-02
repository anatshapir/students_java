package com.javaedu.controller;

import com.javaedu.service.GoogleClassroomService;
import com.javaedu.service.GoogleClassroomService.ClassroomCourse;
import com.javaedu.service.GoogleClassroomService.ClassroomStudent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/google-classroom")
@RequiredArgsConstructor
@Tag(name = "Google Classroom", description = "Google Classroom integration endpoints")
public class GoogleClassroomController {

    private final GoogleClassroomService googleClassroomService;

    @GetMapping("/courses")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get Google Classroom courses for the authenticated user")
    public ResponseEntity<List<ClassroomCourse>> getClassroomCourses(
            @RequestHeader("X-Google-Token") String accessToken) {
        List<ClassroomCourse> courses = googleClassroomService.getCourses(accessToken);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/courses/{classroomId}/students")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Get students from a Google Classroom course")
    public ResponseEntity<List<ClassroomStudent>> getClassroomStudents(
            @RequestHeader("X-Google-Token") String accessToken,
            @PathVariable String classroomId) {
        List<ClassroomStudent> students = googleClassroomService.getStudents(accessToken, classroomId);
        return ResponseEntity.ok(students);
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Sync students from Google Classroom to a JavaEdu course")
    public ResponseEntity<Map<String, String>> syncClassroom(@RequestBody SyncRequest request) {
        googleClassroomService.syncClassroomToCourse(
                request.getGoogleToken(),
                request.getClassroomId(),
                request.getCourseId()
        );
        return ResponseEntity.ok(Map.of("message", "Students synced successfully"));
    }

    @PostMapping("/export-grades")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    @Operation(summary = "Export grades to Google Classroom")
    public ResponseEntity<Map<String, String>> exportGrades(@RequestBody ExportGradesRequest request) {
        googleClassroomService.exportGradesToClassroom(request.getGoogleToken(), request.getCourseId());
        return ResponseEntity.ok(Map.of("message", "Grades exported successfully"));
    }

    @Data
    public static class SyncRequest {
        private String googleToken;
        private String classroomId;
        private Long courseId;
    }

    @Data
    public static class ExportGradesRequest {
        private String googleToken;
        private Long courseId;
    }
}
