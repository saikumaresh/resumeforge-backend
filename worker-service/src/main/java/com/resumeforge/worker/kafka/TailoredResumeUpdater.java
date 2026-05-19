package com.resumeforge.worker.kafka;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;

@Service
public class TailoredResumeUpdater {

    private static final Logger log = LoggerFactory.getLogger(TailoredResumeUpdater.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void updateStatus(UUID tailoredResumeId, String status) {
        log.debug("Updating tailored_resume id={} status={}", tailoredResumeId, status);
        entityManager.createNativeQuery(
            "UPDATE tailored_resumes SET status = ? WHERE id = ?"
        ).setParameter(1, status)
         .setParameter(2, tailoredResumeId)
         .executeUpdate();
    }

    @Transactional
    public void saveAndComplete(UUID tailoredResumeId, Map<String, String> sections, String pdfPath,
                                 int totalScore, int keywordScore, int sectionScore, int actionVerbScore,
                                 String missingKeywords) {
        int position = 0;
        for (Map.Entry<String, String> entry : sections.entrySet()) {
            entityManager.createNativeQuery(
                "INSERT INTO tailored_resume_sections (id, tailored_resume_id, section_type, content, position) " +
                "VALUES (gen_random_uuid(), ?, ?, ?, ?)"
            ).setParameter(1, tailoredResumeId)
             .setParameter(2, entry.getKey().toUpperCase())
             .setParameter(3, entry.getValue())
             .setParameter(4, position++)
             .executeUpdate();
        }

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

        entityManager.createNativeQuery(
            "UPDATE tailored_resumes SET status = 'COMPLETED', pdf_path = ? WHERE id = ?"
        ).setParameter(1, pdfPath)
         .setParameter(2, tailoredResumeId)
         .executeUpdate();

        log.info("Saved {} sections + ATS score for tailoredResumeId={}", sections.size(), tailoredResumeId);
    }
}
