package com.javaedu.eclipse.services;

import java.util.List;

import com.javaedu.eclipse.model.Course;
import com.javaedu.eclipse.model.Exercise;

/**
 * Manages exercises and tracks current exercise selection.
 */
public class ExerciseManager {

    private static ExerciseManager instance;
    private Exercise currentExercise;
    private List<Course> cachedCourses;

    private ExerciseManager() {
    }

    public static synchronized ExerciseManager getInstance() {
        if (instance == null) {
            instance = new ExerciseManager();
        }
        return instance;
    }

    /**
     * Get all courses with exercises.
     */
    public List<Course> getCourses() throws Exception {
        List<Course> courses = ApiClient.getInstance().getCourses();

        // Load exercises for each course
        for (Course course : courses) {
            List<Exercise> exercises = ApiClient.getInstance().getExercisesForCourse(course.getId());
            course.setExercises(exercises);
        }

        this.cachedCourses = courses;
        return courses;
    }

    /**
     * Get cached courses without fetching.
     */
    public List<Course> getCachedCourses() {
        return cachedCourses;
    }

    /**
     * Get full exercise details.
     */
    public Exercise getExerciseDetails(Long exerciseId) throws Exception {
        return ApiClient.getInstance().getExercise(exerciseId);
    }

    /**
     * Set the currently selected exercise.
     */
    public void setCurrentExercise(Exercise exercise) {
        this.currentExercise = exercise;
    }

    /**
     * Get the currently selected exercise.
     */
    public Exercise getCurrentExercise() {
        return currentExercise;
    }

    /**
     * Clear cached data.
     */
    public void clearCache() {
        cachedCourses = null;
        currentExercise = null;
    }
}
