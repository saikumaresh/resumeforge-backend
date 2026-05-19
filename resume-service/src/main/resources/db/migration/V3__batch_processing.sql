CREATE TABLE batch_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    master_resume_id UUID NOT NULL REFERENCES master_resumes(id),
    total_items INTEGER NOT NULL,
    completed_items INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE batch_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    batch_request_id UUID NOT NULL REFERENCES batch_requests(id),
    job_description_id UUID NOT NULL REFERENCES job_descriptions(id),
    status VARCHAR(50) DEFAULT 'PENDING',
    tailored_resume_id UUID REFERENCES tailored_resumes(id)
);
