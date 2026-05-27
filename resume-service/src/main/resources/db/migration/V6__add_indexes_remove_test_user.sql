-- Add missing indexes for performance
CREATE INDEX IF NOT EXISTS idx_master_resumes_user_id ON master_resumes(user_id);
CREATE INDEX IF NOT EXISTS idx_tailored_resumes_created_at ON tailored_resumes(created_at);
CREATE INDEX IF NOT EXISTS idx_tailored_resumes_status ON tailored_resumes(status);
CREATE INDEX IF NOT EXISTS idx_job_descriptions_user_id ON job_descriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_master_resume_sections_master_resume_id ON master_resume_sections(master_resume_id);

-- Remove hardcoded test user (inserted by V4 migration)
DELETE FROM users WHERE id = '550e8400-e29b-41d4-a716-446655440000';
