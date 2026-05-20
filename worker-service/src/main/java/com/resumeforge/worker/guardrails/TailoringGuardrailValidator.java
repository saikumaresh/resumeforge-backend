package com.resumeforge.worker.guardrails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validates AI-generated tailoring output before it is written to the database.
 *
 * Guards against:
 *   1. Empty or structurally broken responses
 *   2. Model narrating instead of generating content (hallucination signals)
 *   3. Sections that are suspiciously too short or too long
 *   4. Any section key not in our known set
 */
@Component
public class TailoringGuardrailValidator {

    private static final Logger log = LoggerFactory.getLogger(TailoringGuardrailValidator.class);

    private static final Set<String> ALLOWED_SECTION_KEYS = Set.of(
            "summary", "experience", "skills", "education", "projects"
    );

    private static final Set<String> REQUIRED_SECTION_KEYS = Set.of(
            "summary", "experience", "skills", "education"
    );

    private static final int MAX_SECTION_CHARS = 6000;
    private static final int MIN_SECTION_CHARS = 5;

    /**
     * Phrases that suggest the model wrote prose about what it did rather than
     * generating the actual resume content. These appear when the model ignores
     * the JSON-only instruction and starts narrating.
     */
    private static final List<String> NARRATION_SIGNALS = List.of(
            "as requested", "i have", "i've ", "i added", "i included",
            "i created", "i rewrote", "here is", "here's ", "below is",
            "please note", "note:", "disclaimer:", "as an ai",
            "i cannot", "i can't", "i should not", "i must not",
            "certainly!", "of course!", "sure!"
    );

    /**
     * Patterns that suggest the user (or job description) injected instructions
     * that the model may have acted on instead of following its system prompt.
     */
    private static final List<Pattern> INJECTION_SIGNALS = List.of(
            Pattern.compile("(?i)ignore\\s+(previous|all|prior)\\s+instruction"),
            Pattern.compile("(?i)you\\s+are\\s+now"),
            Pattern.compile("(?i)act\\s+as\\s+(if|a|an)"),
            Pattern.compile("(?i)forget\\s+(your|the)\\s+(rule|instruction|guardrail)")
    );

    // ── Public API ────────────────────────────────────────────────────────────

    public record ValidationResult(
            boolean valid,
            Map<String, String> sanitizedSections,
            List<String> warnings,
            List<String> errors
    ) {}

    /**
     * Full validation + sanitization pipeline.
     *
     * @param rawSections   sections map as parsed from AI JSON
     * @param originalContent   the master resume text (used for context logging)
     * @return result with sanitized sections and any warnings/errors
     */
    public ValidationResult validate(Map<String, String> rawSections, String originalContent) {
        List<String> warnings = new ArrayList<>();
        List<String> errors   = new ArrayList<>();

        if (rawSections == null || rawSections.isEmpty()) {
            errors.add("AI returned an empty sections map — no content to save");
            return new ValidationResult(false, Map.of(), warnings, errors);
        }

        Map<String, String> sanitized = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : rawSections.entrySet()) {
            String key   = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase();
            String value = entry.getValue();

            // Unknown section key
            if (!ALLOWED_SECTION_KEYS.contains(key)) {
                warnings.add("Ignoring unknown section key '" + key + "'");
                continue;
            }

            // Null / blank
            if (value == null || value.isBlank()) {
                warnings.add("Section '" + key + "' is null/blank — skipping");
                continue;
            }

            value = value.trim();

            // Too short — model probably returned a placeholder
            if (value.length() < MIN_SECTION_CHARS) {
                warnings.add("Section '" + key + "' suspiciously short (" + value.length() + " chars) — skipping");
                continue;
            }

            // Too long — truncate safely
            if (value.length() > MAX_SECTION_CHARS) {
                warnings.add("Section '" + key + "' truncated from " + value.length() + " to " + MAX_SECTION_CHARS + " chars");
                value = value.substring(0, MAX_SECTION_CHARS);
            }

            // Narration / hallucination leak
            String valueLower = value.toLowerCase();
            for (String signal : NARRATION_SIGNALS) {
                if (valueLower.contains(signal)) {
                    warnings.add("Narration signal '" + signal + "' found in section '" + key
                            + "' — model may have ignored JSON-only instruction");
                    break;
                }
            }

            // Injection signals in output (model echoing injected instructions)
            for (Pattern pattern : INJECTION_SIGNALS) {
                if (pattern.matcher(value).find()) {
                    warnings.add("Injection pattern detected in section '" + key + "' output — content may be compromised");
                    break;
                }
            }

            sanitized.put(key, value);
        }

        // Check required sections
        for (String required : REQUIRED_SECTION_KEYS) {
            if (!sanitized.containsKey(required)) {
                warnings.add("Required section '" + required + "' missing from AI output");
            }
        }

        if (sanitized.isEmpty()) {
            errors.add("All sections were empty or invalid after validation — cannot save");
            return new ValidationResult(false, sanitized, warnings, errors);
        }

        if (!warnings.isEmpty()) {
            log.warn("[GUARDRAIL] Tailoring validation warnings: {}", warnings);
        }

        return new ValidationResult(errors.isEmpty(), sanitized, warnings, errors);
    }

    /**
     * Quick structural check: does the raw AI response look like JSON at all?
     * Used before Jackson deserialization to give a clearer error message.
     */
    public boolean looksLikeJson(String content) {
        if (content == null || content.isBlank()) return false;
        String t = content.trim();
        return t.startsWith("{") && t.endsWith("}");
    }
}
