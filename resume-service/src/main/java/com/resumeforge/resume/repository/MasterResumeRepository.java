package com.resumeforge.resume.repository;

import com.resumeforge.resume.model.MasterResume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MasterResumeRepository extends JpaRepository<MasterResume, UUID> {

    List<MasterResume> findByUserId(UUID userId);

    // Fix N+1: JOIN FETCH sections in a single query
    @Query("SELECT r FROM MasterResume r LEFT JOIN FETCH r.sections WHERE r.id = :id")
    Optional<MasterResume> findByIdWithSections(@Param("id") UUID id);
}
