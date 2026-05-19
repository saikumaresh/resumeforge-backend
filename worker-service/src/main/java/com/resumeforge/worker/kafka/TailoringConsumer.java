package com.resumeforge.worker.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeforge.worker.llm.OllamaClient;
import com.resumeforge.worker.pdf.ResumePDFGenerator;
import com.resumeforge.worker.scoring.ATSScorer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TailoringConsumer {

    private final OllamaClient ollamaClient;
    private final ResumePDFGenerator pdfGenerator;
    private final TailoredResumeUpdater resumeUpdater;
    private final ATSScorer atsScorer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "resume.tailoring.requested", groupId = "worker-group")
    public void consume(Map<String, Object> event) {
        String tailoredResumeIdStr = String.valueOf(event.get("tailoredResumeId"));
        MDC.put("tailoredResumeId", tailoredResumeIdStr);
        log.info("Received tailoring request for resumeId={}", tailoredResumeIdStr);

        try {
            UUID tailoredResumeId = UUID.fromString(tailoredResumeIdStr);
            String masterContent = String.valueOf(event.get("masterResumeContent"));
            String jobDescription = String.valueOf(event.get("jobDescriptionText"));

            // Step 1: Mark PROCESSING
            resumeUpdater.updateStatus(tailoredResumeId, "PROCESSING");

            // Step 2: Call Ollama LLM
            String aiResponse = ollamaClient.tailorResume(masterContent, jobDescription);

            // Step 3: Parse sections from JSON response
            Map<String, String> sections = objectMapper.readValue(aiResponse,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));

            // Step 4: Generate PDF
            String pdfPath = pdfGenerator.generate(tailoredResumeId, sections);

            // Step 5: Run ATS scoring on tailored content
            String tailoredContent = sections.values().stream().collect(Collectors.joining("\n"));
            Set<String> presentSections = sections.keySet().stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());

            ATSScorer.ATSScoreBreakdown score = atsScorer.score(tailoredContent, jobDescription, presentSections);
            log.info("ATS score computed: total={} for resumeId={}", score.totalScore(), tailoredResumeId);

            // Step 6: Save everything and mark COMPLETED
            resumeUpdater.saveAndComplete(
                    tailoredResumeId, sections, pdfPath,
                    score.totalScore(), score.keywordScore(),
                    score.sectionScore(), score.actionVerbScore(),
                    score.missingKeywords()
            );

            log.info("Tailoring completed resumeId={} pdfPath={}", tailoredResumeId, pdfPath);

        } catch (Exception e) {
            log.error("Tailoring failed for resumeId={}: {}", tailoredResumeIdStr, e.getMessage(), e);
            try {
                resumeUpdater.updateStatus(UUID.fromString(tailoredResumeIdStr), "FAILED");
            } catch (Exception ex) {
                log.error("Failed to mark FAILED status: {}", ex.getMessage());
            }
        } finally {
            MDC.clear();
        }
    }
}
