package com.resumeforge.resume.repository;

import com.resumeforge.resume.model.TailoredResume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface TailoredResumeRepository extends JpaRepository<TailoredResume, UUID> {

    List<TailoredResume> findByMasterResumeId(UUID masterResumeId);

    // Fix N+1 for listing tailored resumes with sections
    @Query("SELECT r FROM TailoredResume r LEFT JOIN FETCH r.sections WHERE r.masterResume.id = :masterResumeId")
    List<TailoredResume> findByMasterResumeIdWithSections(@Param("masterResumeId") UUID masterResumeId);
}
