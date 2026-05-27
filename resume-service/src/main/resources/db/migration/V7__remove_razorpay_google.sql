-- V7: Remove Razorpay payment tables and unused Google OAuth columns

-- Drop subscriptions table (Razorpay removed)
DROP TABLE IF EXISTS subscriptions;

-- Drop Google OAuth columns from users (Google auth removed)
ALTER TABLE users
    DROP COLUMN IF EXISTS google_id,
    DROP COLUMN IF EXISTS picture_url;
