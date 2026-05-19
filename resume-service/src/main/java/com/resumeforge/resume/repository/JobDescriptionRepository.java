package com.resumeforge.resume.repository;

import com.resumeforge.resume.model.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface JobDescriptionRepository extends JpaRepository<JobDescription, UUID> {
    List<JobDescription> findByUserId(UUID userId);
}
