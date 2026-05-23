-- Add authentication fields to users table
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Insert the hardcoded test user if not already present,
-- so existing data continues to work after auth is added.
INSERT INTO users (id, name, email, email_verified)
VALUES ('550e8400-e29b-41d4-a716-446655440000', 'Test User', 'test@resumeforge.dev', TRUE)
ON CONFLICT (id) DO NOTHING;
