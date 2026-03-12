-- V5: Google OAuth support for users
-- Adds columns to store Google OAuth refresh tokens and connection status

-- Add Google OAuth columns to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_refresh_token TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_token_expiry TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_connected_at TIMESTAMP;

-- Add sync-related columns to courses table
ALTER TABLE courses ADD COLUMN IF NOT EXISTS enrollment_code VARCHAR(20) UNIQUE;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS auto_sync_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS last_synced_at TIMESTAMP;

-- Create index for enrollment code lookups
CREATE INDEX IF NOT EXISTS idx_courses_enrollment_code ON courses(enrollment_code);
