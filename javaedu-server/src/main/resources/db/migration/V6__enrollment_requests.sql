-- V6: Self-enrollment with approval workflow
-- Creates enrollment_requests table for pending student enrollment requests

CREATE TABLE enrollment_requests (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by BIGINT REFERENCES users(id),
    UNIQUE (course_id, user_id)
);

CREATE INDEX idx_enrollment_requests_course_id ON enrollment_requests(course_id);
CREATE INDEX idx_enrollment_requests_user_id ON enrollment_requests(user_id);
CREATE INDEX idx_enrollment_requests_status ON enrollment_requests(status);
