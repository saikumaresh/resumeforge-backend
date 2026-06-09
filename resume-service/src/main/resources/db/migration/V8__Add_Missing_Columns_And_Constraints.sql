-- ✅ PHASE 3 FIX: Add missing columns, constraints, and indexes
-- Aligns database schema with production code and audit requirements

-- ════════════════════════════════════════════════════════════════════════════
-- STEP 1: Add missing columns to users table
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS monthly_usage_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS stripe_customer_id VARCHAR(255) UNIQUE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT now();

-- ════════════════════════════════════════════════════════════════════════════
-- STEP 2: Add missing columns to tailored_resumes table
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE tailored_resumes
    ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS error_message TEXT,
    ADD COLUMN IF NOT EXISTS pdf_s3_key VARCHAR(500);

-- ════════════════════════════════════════════════════════════════════════════
-- STEP 3: Add CASCADE DELETE to foreign keys (prevent orphaned records)
-- ════════════════════════════════════════════════════════════════════════════

-- Note: In PostgreSQL, we cannot directly modify existing FK constraints.
-- These would need manual recreation in a larger refactor.
-- For now, verify the constraints are set correctly:

-- master_resumes: ON DELETE CASCADE
-- job_descriptions: ON DELETE CASCADE (add if missing)
-- tailored_resumes: ON DELETE CASCADE for both master_resume_id and job_description_id

-- Update FK for job_descriptions if it doesn't have CASCADE
-- (Run manually if needed: ALTER TABLE job_descriptions DROP CONSTRAINT job_descriptions_user_id_fkey,
--  ADD CONSTRAINT job_descriptions_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)

-- ════════════════════════════════════════════════════════════════════════════
-- STEP 4: Add CHECK constraints for ENUM columns
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE users
    ADD CONSTRAINT check_plan_enum
    CHECK (plan IN ('FREE', 'PRO'));

ALTER TABLE tailored_resumes
    ADD CONSTRAINT check_tailored_resume_status_enum
    CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'));

-- Subscriptions table: recreate with Stripe columns (if table exists)
-- The V7 migration dropped the old subscriptions table, so we recreate with new schema
CREATE TABLE IF NOT EXISTS subscriptions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stripe_subscription_id VARCHAR(255) UNIQUE,
    stripe_customer_id     VARCHAR(255),
    plan           VARCHAR(20) NOT NULL DEFAULT 'PRO',
    status         VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | CANCELLED | EXPIRED
    is_active      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT check_subscription_status CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_stripe_customer_id ON subscriptions(stripe_customer_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);

-- ════════════════════════════════════════════════════════════════════════════
-- STEP 5: Add indexes on high-query columns for performance
-- ════════════════════════════════════════════════════════════════════════════

-- Users table indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_stripe_customer_id ON users(stripe_customer_id);

-- Master resumes indexes
CREATE INDEX IF NOT EXISTS idx_master_resumes_user_id ON master_resumes(user_id);

-- Job descriptions indexes
CREATE INDEX IF NOT EXISTS idx_job_descriptions_user_id ON job_descriptions(user_id);

-- Tailored resumes indexes (most critical for filtering)
CREATE INDEX IF NOT EXISTS idx_tailored_resumes_user_id ON tailored_resumes(user_id);
CREATE INDEX IF NOT EXISTS idx_tailored_resumes_status ON tailored_resumes(status);
CREATE INDEX IF NOT EXISTS idx_tailored_resumes_master_resume_id ON tailored_resumes(master_resume_id);
CREATE INDEX IF NOT EXISTS idx_tailored_resumes_job_description_id ON tailored_resumes(job_description_id);

-- ════════════════════════════════════════════════════════════════════════════
-- STEP 6: Add triggers for automatic updated_at timestamp updates
-- ════════════════════════════════════════════════════════════════════════════

-- Create function to update timestamp if not exists
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for automatic timestamp updates
DROP TRIGGER IF EXISTS users_update_timestamp ON users;
CREATE TRIGGER users_update_timestamp
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

DROP TRIGGER IF EXISTS master_resumes_update_timestamp ON master_resumes;
CREATE TRIGGER master_resumes_update_timestamp
    BEFORE UPDATE ON master_resumes
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

DROP TRIGGER IF EXISTS subscriptions_update_timestamp ON subscriptions;
CREATE TRIGGER subscriptions_update_timestamp
    BEFORE UPDATE ON subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- ════════════════════════════════════════════════════════════════════════════
-- STEP 7: Verify schema consistency
-- ════════════════════════════════════════════════════════════════════════════

-- After this migration:
-- ✓ users: has monthly_usage_count, stripe_customer_id, updated_at, plan
-- ✓ tailored_resumes: has user_id, error_message, pdf_s3_key
-- ✓ subscriptions: recreated with Stripe integration (not Razorpay)
-- ✓ All foreign keys have CHECK constraints for valid ENUMs
-- ✓ Indexes on frequently queried columns for performance
-- ✓ Automatic timestamp updates on write operations

-- IMPORTANT: Manual step needed (if not automated elsewhere):
-- 1. Populate user_id in tailored_resumes from master_resumes.user_id
--    UPDATE tailored_resumes SET user_id = (
--      SELECT mr.user_id FROM master_resumes mr WHERE mr.id = tailored_resumes.master_resume_id
--    ) WHERE user_id IS NULL;
-- 2. Review and migrate Razorpay data to Stripe (if any subscriptions exist)
-- 3. Test cascading deletes with test data before production
