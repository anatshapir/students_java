-- JavaEdu Initial Schema
-- V1: Core tables for users, courses, exercises, submissions, and analytics

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    github_id VARCHAR(255),
    google_id VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_github_id ON users(github_id);
CREATE INDEX idx_users_google_id ON users(google_id);
CREATE INDEX idx_users_role ON users(role);

-- Courses table
CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    teacher_id BIGINT NOT NULL REFERENCES users(id),
    google_classroom_id VARCHAR(255),
    github_org VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_courses_teacher_id ON courses(teacher_id);

-- Course enrollments (many-to-many)
CREATE TABLE course_enrollments (
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (course_id, user_id)
);

CREATE INDEX idx_course_enrollments_user_id ON course_enrollments(user_id);

-- Exercises table
CREATE TABLE exercises (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    starter_code TEXT,
    solution_code TEXT,
    difficulty VARCHAR(50) NOT NULL,
    points INTEGER NOT NULL DEFAULT 100,
    category VARCHAR(50),
    due_date TIMESTAMP,
    is_published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_exercises_course_id ON exercises(course_id);
CREATE INDEX idx_exercises_difficulty ON exercises(difficulty);
CREATE INDEX idx_exercises_category ON exercises(category);
CREATE INDEX idx_exercises_due_date ON exercises(due_date);

-- Test cases table
CREATE TABLE test_cases (
    id BIGSERIAL PRIMARY KEY,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    test_code TEXT NOT NULL,
    input TEXT,
    expected_output TEXT,
    is_hidden BOOLEAN DEFAULT FALSE,
    points INTEGER NOT NULL DEFAULT 10,
    order_num INTEGER DEFAULT 0,
    timeout_seconds INTEGER DEFAULT 5
);

CREATE INDEX idx_test_cases_exercise_id ON test_cases(exercise_id);

-- Hints table
CREATE TABLE hints (
    id BIGSERIAL PRIMARY KEY,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    order_num INTEGER NOT NULL,
    content TEXT NOT NULL,
    is_ai_generated BOOLEAN DEFAULT FALSE,
    penalty_percentage INTEGER DEFAULT 0
);

CREATE INDEX idx_hints_exercise_id ON hints(exercise_id);

-- Submissions table
CREATE TABLE submissions (
    id BIGSERIAL PRIMARY KEY,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code TEXT NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    execution_time_ms BIGINT,
    compiler_output TEXT
);

CREATE INDEX idx_submissions_exercise_id ON submissions(exercise_id);
CREATE INDEX idx_submissions_user_id ON submissions(user_id);
CREATE INDEX idx_submissions_submitted_at ON submissions(submitted_at);
CREATE INDEX idx_submissions_status ON submissions(status);

-- Test results table
CREATE TABLE test_results (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    test_case_id BIGINT NOT NULL REFERENCES test_cases(id) ON DELETE CASCADE,
    passed BOOLEAN NOT NULL,
    actual_output TEXT,
    error_message TEXT,
    execution_time_ms BIGINT
);

CREATE INDEX idx_test_results_submission_id ON test_results(submission_id);

-- Grades table
CREATE TABLE grades (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL UNIQUE REFERENCES submissions(id) ON DELETE CASCADE,
    score INTEGER NOT NULL,
    max_score INTEGER NOT NULL,
    feedback TEXT,
    graded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_auto_graded BOOLEAN DEFAULT TRUE,
    graded_by BIGINT REFERENCES users(id)
);

CREATE INDEX idx_grades_submission_id ON grades(submission_id);

-- AI interactions table
CREATE TABLE ai_interactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise_id BIGINT REFERENCES exercises(id) ON DELETE SET NULL,
    question TEXT NOT NULL,
    response TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    was_helpful BOOLEAN,
    tokens_used INTEGER
);

CREATE INDEX idx_ai_interactions_user_id ON ai_interactions(user_id);
CREATE INDEX idx_ai_interactions_exercise_id ON ai_interactions(exercise_id);
CREATE INDEX idx_ai_interactions_timestamp ON ai_interactions(timestamp);

-- Error patterns table (for learning engine)
CREATE TABLE error_patterns (
    id BIGSERIAL PRIMARY KEY,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    pattern TEXT NOT NULL,
    occurrence_count INTEGER NOT NULL DEFAULT 1,
    suggested_hint TEXT,
    error_type VARCHAR(255)
);

CREATE INDEX idx_error_patterns_exercise_id ON error_patterns(exercise_id);
CREATE UNIQUE INDEX idx_error_patterns_unique ON error_patterns(exercise_id, pattern);

-- Student analytics table
CREATE TABLE student_analytics (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise_id BIGINT NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    attempts INTEGER NOT NULL DEFAULT 0,
    time_spent_minutes INTEGER DEFAULT 0,
    hints_used INTEGER DEFAULT 0,
    ai_interactions_count INTEGER DEFAULT 0,
    first_submission_at TIMESTAMP,
    completed_at TIMESTAMP,
    UNIQUE (user_id, exercise_id)
);

CREATE INDEX idx_student_analytics_user_id ON student_analytics(user_id);
CREATE INDEX idx_student_analytics_exercise_id ON student_analytics(exercise_id);
