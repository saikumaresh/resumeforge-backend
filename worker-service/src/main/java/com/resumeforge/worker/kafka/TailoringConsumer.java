package com.resumeforge.worker.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeforge.worker.guardrails.TailoringGuardrailValidator;
import com.resumeforge.worker.idempotency.IdempotencyService;
import com.resumeforge.worker.llm.OllamaClient;
import com.resumeforge.worker.scoring.ATSScorer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ✅ PHASE 1.4 FIX: Kafka message idempotency implemented.
 * Prevents duplicate processing of resume tailoring requests.
 */
@Component
public class TailoringConsumer {

    private static final Logger log = LoggerFactory.getLogger(TailoringConsumer.class);

    private final OllamaClient ollamaClient;
    private final TailoredResumeUpdater resumeUpdater;
    private final ATSScorer atsScorer;
    private final ObjectMapper objectMapper;
    private final TailoringGuardrailValidator guardrailValidator;
    private final IdempotencyService idempotencyService;

    public TailoringConsumer(OllamaClient ollamaClient,
                              TailoredResumeUpdater resumeUpdater,
                              ATSScorer atsScorer,
                              ObjectMapper objectMapper,
                              TailoringGuardrailValidator guardrailValidator,
                              IdempotencyService idempotencyService) {
        this.ollamaClient        = ollamaClient;
        this.resumeUpdater       = resumeUpdater;
        this.atsScorer           = atsScorer;
        this.objectMapper        = objectMapper;
        this.guardrailValidator  = guardrailValidator;
        this.idempotencyService  = idempotencyService;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Recursively flattens an AI JSON value to a plain readable string.
     *
     * gemma3 sometimes returns structured objects instead of plain strings:
     *   experience → { "title": "...", "company": "...", "dates": "...", "description": [...] }
     *   education  → { "degree": "...", "university": "...", "date": "..." }
     *
     * This method converts those nested structures to human-readable prose so
     * the resume sections display correctly.
     */
    @SuppressWarnings("unchecked")
    private String flattenValue(Object value) {
        if (value == null) return "";

        if (value instanceof String s) {
            return s;
        }

        if (value instanceof java.util.List<?> list) {
            // Array of items — join with bullet points if they're strings, else recurse
            return list.stream()
                    .filter(item -> item != null)
                    .map(item -> {
                        if (item instanceof String) return "• " + item;
                        if (item instanceof java.util.Map) return flattenMap((java.util.Map<String, Object>) item);
                        return item.toString();
                    })
                    .collect(java.util.stream.Collectors.joining("\n"));
        }

        if (value instanceof java.util.Map) {
            return flattenMap((java.util.Map<String, Object>) value);
        }

        return value.toString();
    }

    /**
     * Renders a structured experience/education Map as readable plain text.
     *
     * Recognises common field patterns:
     *   experience: title, role, position, company, employer, org, dates, date, duration, period, description, responsibilities, achievements
     *   education:  degree, qualification, institution, university, school, date, year, gpa
     */
    @SuppressWarnings("unchecked")
    private String flattenMap(java.util.Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();

        // ── Experience-style: title / role / position ─────────────────────
        String title    = firstNonNull(map, "title", "role", "position", "jobTitle");
        String company  = firstNonNull(map, "company", "employer", "organization", "org");
        String dates    = firstNonNull(map, "dates", "date", "duration", "period", "years");
        Object descObj  = firstValueNonNull(map, "description", "responsibilities", "achievements", "bullets", "details");

        // ── Education-style: degree / institution ────────────────────────
        String degree   = firstNonNull(map, "degree", "qualification", "program");
        String school   = firstNonNull(map, "university", "institution", "school", "college");
        String gradYear = firstNonNull(map, "year", "graduationYear", "graduation");
        Object gpa      = firstValueNonNull(map, "gpa", "grade", "honours", "honors");

        boolean isExperience = title != null || company != null;
        boolean isEducation  = !isExperience && (degree != null || school != null);

        if (isExperience) {
            // Header line: "Software Engineer at Google  (2021 – 2024)"
            if (title != null)   sb.append(title);
            if (company != null) sb.append(title != null ? " at " : "").append(company);
            if (dates != null)   sb.append("  (").append(dates).append(")");
            if (sb.length() > 0) sb.append("\n");

            if (descObj instanceof java.util.List<?> bullets) {
                bullets.stream().filter(b -> b != null)
                       .forEach(b -> sb.append("• ").append(b).append("\n"));
            } else if (descObj instanceof String d) {
                sb.append(d);
            } else if (descObj != null) {
                sb.append(flattenValue(descObj));
            }

            // Append any remaining fields that weren't captured above
            appendRemainingFields(sb, map, "title", "role", "position", "jobTitle",
                    "company", "employer", "organization", "org",
                    "dates", "date", "duration", "period", "years",
                    "description", "responsibilities", "achievements", "bullets", "details");

        } else if (isEducation) {
            // Header: "B.Tech Computer Science, MIT  (2019)"
            if (degree != null) sb.append(degree);
            if (school != null) sb.append(degree != null ? ", " : "").append(school);
            if (gradYear != null) sb.append("  (").append(gradYear).append(")");
            if (dates != null && gradYear == null) sb.append("  (").append(dates).append(")");
            if (gpa != null)    sb.append("  •  GPA: ").append(gpa);
            if (sb.length() > 0) sb.append("\n");

            appendRemainingFields(sb, map, "degree", "qualification", "program",
                    "university", "institution", "school", "college",
                    "year", "graduationYear", "graduation",
                    "date", "dates", "gpa", "grade", "honours", "honors");

        } else {
            // Unknown structure — render as "Key: value" lines
            map.forEach((k, v) -> {
                if (v != null) sb.append(k).append(": ").append(flattenValue(v)).append("\n");
            });
        }

        return sb.toString().trim();
    }

    /** Returns the first non-null string value found under any of the given keys. */
    private String firstNonNull(java.util.Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v instanceof String s && !s.isBlank()) return s;
        }
        return null;
    }

    /** Returns the first non-null Object value found under any of the given keys. */
    private Object firstValueNonNull(java.util.Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v != null) return v;
        }
        return null;
    }

    /** Appends fields from {@code map} that are NOT in the exclusion set, as "Key: value" lines. */
    private void appendRemainingFields(StringBuilder sb, java.util.Map<String, Object> map, String... exclude) {
        java.util.Set<String> skip = new java.util.HashSet<>(java.util.Arrays.asList(exclude));
        map.forEach((k, v) -> {
            if (!skip.contains(k) && v != null) {
                String rendered = flattenValue(v);
                if (!rendered.isBlank()) sb.append(k).append(": ").append(rendered).append("\n");
            }
        });
    }

    @KafkaListener(topics = "resume.tailoring.requested", groupId = "worker-group")
    public void consume(Map<String, Object> event) {
        String tailoredResumeIdStr = String.valueOf(event.get("tailoredResumeId"));
        MDC.put("tailoredResumeId", tailoredResumeIdStr);
        log.info("Received tailoring request for resumeId={}", tailoredResumeIdStr);

        try {
            UUID   tailoredResumeId = UUID.fromString(tailoredResumeIdStr);
            String masterContent    = String.valueOf(event.get("masterResumeContent"));
            String jobDescription   = String.valueOf(event.get("jobDescriptionText"));

            // ✅ PHASE 1.4 FIX: Check idempotency
            if (idempotencyService.isDuplicate(tailoredResumeId)) {
                log.info("Skipping duplicate message for resumeId={}", tailoredResumeId);
                return;  // Already processed, skip
            }

            resumeUpdater.updateStatus(tailoredResumeId, "PROCESSING");

            // ── 1. Call Ollama ───────────────────────────────────────────────
            String aiResponse = ollamaClient.tailorResume(masterContent, jobDescription);

            // ── 2. Structural check before Jackson parse ─────────────────────
            if (!guardrailValidator.looksLikeJson(aiResponse)) {
                log.error("[GUARDRAIL] AI response is not JSON — content starts with: {}",
                        aiResponse != null ? aiResponse.substring(0, Math.min(120, aiResponse.length())) : "null");
                resumeUpdater.updateStatus(tailoredResumeId, "FAILED");
                return;
            }

            // ── 3. Parse JSON — coerce arrays to strings ─────────────────────
            // gemma3 sometimes returns experience/skills as a JSON array of strings.
            // Deserialize into Map<String, Object> then flatten any arrays.
            @SuppressWarnings("unchecked")
            Map<String, Object> rawParsed = objectMapper.readValue(aiResponse,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));

            Map<String, String> rawSections = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, Object> e : rawParsed.entrySet()) {
                if (e.getValue() == null) continue;
                rawSections.put(e.getKey(), flattenValue(e.getValue()));
            }

            // ── 4. Guardrail validation ──────────────────────────────────────
            TailoringGuardrailValidator.ValidationResult validation =
                    guardrailValidator.validate(rawSections, masterContent);

            if (!validation.errors().isEmpty()) {
                log.error("[GUARDRAIL] Tailoring output failed validation: {}", validation.errors());
                resumeUpdater.updateStatus(tailoredResumeId, "FAILED");
                return;
            }

            // Use the sanitized (validated) sections from here on
            Map<String, String> sections = validation.sanitizedSections();

            // ── 5. PDF generation skipped (no persistent storage configured) ─
            //    pdfPath remains null; pdfDownloadUrl will be absent from responses.
            //    To enable: add cloud storage (e.g. R2) and wire ResumePDFGenerator.
            String pdfPath = null;

            // ── 6. ATS scoring ───────────────────────────────────────────────
            String tailoredContent = String.join("\n", sections.values());
            Set<String> presentSections = sections.keySet().stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());

            ATSScorer.ATSScoreBreakdown score =
                    atsScorer.score(tailoredContent, jobDescription, presentSections);
            log.info("ATS score computed: total={} for resumeId={}", score.totalScore(), tailoredResumeId);

            // ── 7. Persist ───────────────────────────────────────────────────
            resumeUpdater.saveAndComplete(
                    tailoredResumeId, sections, pdfPath,
                    score.totalScore(), score.keywordScore(),
                    score.sectionScore(), score.actionVerbScore(),
                    score.missingKeywords()
            );

            log.info("Tailoring completed resumeId={} pdfPath={} sections={}",
                    tailoredResumeId, pdfPath, sections.keySet());

            // ✅ PHASE 1.4 FIX: Mark as processed in Redis
            idempotencyService.markAsProcessed(tailoredResumeId,
                    "score=" + score.totalScore());

        } catch (Exception e) {
            log.error("Tailoring failed for resumeId={}: {}", tailoredResumeIdStr, e.getMessage(), e);
            try {
                UUID tailoredResumeId = UUID.fromString(tailoredResumeIdStr);
                resumeUpdater.updateStatus(tailoredResumeId, "FAILED");
                // ✅ PHASE 1.4 FIX: Mark failure to avoid infinite retries
                idempotencyService.markAsFailed(tailoredResumeId, e.getMessage());
            } catch (Exception ex) {
                log.error("Failed to mark FAILED status: {}", ex.getMessage());
            }
        } finally {
            MDC.clear();
        }
    }
}
