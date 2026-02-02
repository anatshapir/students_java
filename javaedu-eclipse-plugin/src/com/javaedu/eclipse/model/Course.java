package com.javaedu.eclipse.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a course.
 */
public class Course {
    private Long id;
    private String name;
    private String description;
    private String teacherName;
    private List<Exercise> exercises = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public List<Exercise> getExercises() {
        return exercises;
    }

    public void setExercises(List<Exercise> exercises) {
        this.exercises = exercises;
        // Set back-reference
        for (Exercise ex : exercises) {
            ex.setCourse(this);
        }
    }
}
