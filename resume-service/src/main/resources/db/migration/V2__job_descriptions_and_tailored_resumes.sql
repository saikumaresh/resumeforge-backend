CREATE TABLE job_descriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    company_name VARCHAR(255) NOT NULL,
    job_title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    required_skills TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE tailored_resumes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    master_resume_id UUID NOT NULL REFERENCES master_resumes(id),
    job_description_id UUID NOT NULL REFERENCES job_descriptions(id),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    version INTEGER DEFAULT 0,
    pdf_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE tailored_resume_sections (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tailored_resume_id UUID NOT NULL REFERENCES tailored_resumes(id),
    section_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    position INTEGER NOT NULL
);

CREATE TABLE ats_score_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tailored_resume_id UUID UNIQUE NOT NULL REFERENCES tailored_resumes(id),
    total_score INTEGER NOT NULL,
    keyword_score INTEGER,
    section_score INTEGER,
    action_verb_score INTEGER,
    missing_keywords TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
