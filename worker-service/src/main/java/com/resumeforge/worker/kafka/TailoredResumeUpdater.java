package com.resumeforge.worker.kafka;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;

/**
 * ✅ PHASE 1.6 FIX: SQL Injection Prevention
 *
 * All queries use parameterized statements with `.setParameter()`.
 * NO string concatenation. NO dynamic SQL construction.
 *
 * Parameterized queries ensure:
 * - SQL injection impossible (parameters are data, not code)
 * - Database can cache prepared statements (performance)
 * - Clear separation between SQL and data
 *
 * All methods are @Transactional to ensure atomic operations.
 */
@Service
public class TailoredResumeUpdater {

    private static final Logger log = LoggerFactory.getLogger(TailoredResumeUpdater.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Updates tailored resume status atomically.
     * ✅ PHASE 1.6: Uses parameterized query (safe from SQL injection)
     *
     * @param tailoredResumeId The resume to update
     * @param status New status (PENDING, PROCESSING, COMPLETED, FAILED)
     */
    @Transactional
    public void updateStatus(UUID tailoredResumeId, String status) {
        log.debug("Updating tailored_resume id={} status={}", tailoredResumeId, status);

        // ✅ PHASE 1.6: Parameterized query (? placeholders, setParameter)
        entityManager.createNativeQuery(
            "UPDATE tailored_resumes SET status = ? WHERE id = ?"
        ).setParameter(1, status)
         .setParameter(2, tailoredResumeId)
         .executeUpdate();
    }

    /**
     * ✅ PHASE 1.6: Atomic transaction for complete tailoring save.
     *
     * All-or-nothing: Either all sections + score + status are updated, or none.
     * If any step fails, entire transaction rolls back (no partial data).
     *
     * Steps (all within single @Transactional):
     * 1. Delete old sections (idempotent cleanup)
     * 2. Insert new sections (with position ordering)
     * 3. Insert ATS score breakdown
     * 4. Update status to COMPLETED
     *
     * @param tailoredResumeId The resume being completed
     * @param sections Map of section names to content
     * @param pdfPath Path to generated PDF (may be null)
     * @param totalScore Overall ATS score (0-100)
     * @param keywordScore Keyword match component
     * @param sectionScore Section relevance component
     * @param actionVerbScore Action verb score component
     * @param missingKeywords Comma-separated missing keywords from JD
     */
    @Transactional
    public void saveAndComplete(UUID tailoredResumeId, Map<String, String> sections, String pdfPath,
                                 int totalScore, int keywordScore, int sectionScore, int actionVerbScore,
                                 String missingKeywords) {

        // Step 1: Delete any pre-existing sections (idempotent retry safety)
        // ✅ PHASE 1.6: Parameterized query
        entityManager.createNativeQuery(
            "DELETE FROM tailored_resume_sections WHERE tailored_resume_id = ?"
        ).setParameter(1, tailoredResumeId)
         .executeUpdate();

        // Step 2: Insert all new sections with position ordering
        int position = 0;
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) continue;

            // ✅ PHASE 1.6: Parameterized query (all ? are filled with setParameter)
            entityManager.createNativeQuery(
                "INSERT INTO tailored_resume_sections (id, tailored_resume_id, section_type, content, position) " +
                "VALUES (gen_random_uuid(), ?, ?, ?, ?)"
            ).setParameter(1, tailoredResumeId)
             .setParameter(2, entry.getKey().toUpperCase())
             .setParameter(3, entry.getValue())
             .setParameter(4, position++)
             .executeUpdate();
        }

        // Step 3: Insert ATS score breakdown
        // ✅ PHASE 1.6: Parameterized query
        entityManager.createNativeQuery(
            "INSERT INTO ats_score_results (id, tailored_resume_id, total_score, keyword_score, section_score, action_verb_score, missing_keywords) " +
            "VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?)"
        ).setParameter(1, tailoredResumeId)
         .setParameter(2, totalScore)
         .setParameter(3, keywordScore)
         .setParameter(4, sectionScore)
         .setParameter(5, actionVerbScore)
         .setParameter(6, missingKeywords)
         .executeUpdate();

        // Step 4: Mark as COMPLETED (atomic with all above steps)
        // ✅ PHASE 1.6: Parameterized query
        entityManager.createNativeQuery(
            "UPDATE tailored_resumes SET status = 'COMPLETED', pdf_path = ? WHERE id = ?"
        ).setParameter(1, pdfPath)
         .setParameter(2, tailoredResumeId)
         .executeUpdate();

        log.info("Saved {} sections + ATS score for tailoredResumeId={}", sections.size(), tailoredResumeId);
    }
}
