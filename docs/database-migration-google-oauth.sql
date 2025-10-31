-- Add google_id column to users table for Google OAuth2 integration
-- Date: 2025-10-31

ALTER TABLE users
ADD COLUMN google_id VARCHAR(255) UNIQUE
COMMENT 'Google OAuth2 user ID';

-- Create index for faster lookups
CREATE INDEX idx_users_google_id ON users(google_id);

