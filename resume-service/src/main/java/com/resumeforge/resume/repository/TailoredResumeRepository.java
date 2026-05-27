package com.resumeforge.resume.repository;

import com.resumeforge.resume.model.TailoredResume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TailoredResumeRepository extends JpaRepository<TailoredResume, UUID> {

    List<TailoredResume> findByMasterResumeId(UUID masterResumeId);

    /** Count tailored resumes created by a user since a given timestamp (for quota checks), excluding FAILED. */
    @Query("SELECT COUNT(r) FROM TailoredResume r WHERE r.masterResume.userId = :userId AND r.createdAt >= :since AND r.status <> com.resumeforge.resume.model.TailoredResume.TailoringStatus.FAILED")
    long countByUserIdSince(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    @Query("SELECT r FROM TailoredResume r LEFT JOIN FETCH r.sections WHERE r.masterResume.id = :masterResumeId")
    List<TailoredResume> findByMasterResumeIdWithSections(@Param("masterResumeId") UUID masterResumeId);

    @Query("SELECT r FROM TailoredResume r LEFT JOIN FETCH r.sections WHERE r.masterResume.userId = :userId ORDER BY r.createdAt DESC")
    List<TailoredResume> findByUserIdWithSections(@Param("userId") UUID userId);

    @Query("SELECT r FROM TailoredResume r LEFT JOIN FETCH r.sections LEFT JOIN FETCH r.atsScoreResult WHERE r.id = :id")
    java.util.Optional<TailoredResume> findByIdWithSectionsAndScore(@Param("id") UUID id);
}
