package com.resumeforge.resume.guardrails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Validates and normalises AI chat responses before they are returned to the client.
 *
 * Guards against:
 *   1. Unknown or invalid suggestedSection values (prevents client-side confusion)
 *   2. Reply or content strings that are excessively long
 *   3. suggestedContent without a matching suggestedSection (orphaned content)
 *   4. Section names in non-canonical form (uppercase, mixed case, etc.)
 */
@Component
public class ChatGuardrailValidator {

    private static final Logger log = LoggerFactory.getLogger(ChatGuardrailValidator.class);

    private static final Set<String> VALID_SECTIONS = Set.of(
            "summary", "experience", "skills", "education", "projects"
    );

    private static final int MAX_REPLY_CHARS            = 3000;
    private static final int MAX_SUGGESTED_CONTENT_CHARS = 5000;

    // ── Public API ────────────────────────────────────────────────────────────

    public record ValidationResult(
            String reply,
            String suggestedSection,
            String suggestedContent,
            boolean wasSanitized
    ) {}

    /**
     * Validates all three fields of a chat response.
     *
     * @param reply            the AI's explanation text
     * @param suggestedSection the section the AI wants to rewrite (may be null)
     * @param suggestedContent the rewritten section content (may be null)
     */
    public ValidationResult validate(String reply, String suggestedSection, String suggestedContent) {
        boolean dirty = false;

        // ── Reply ─────────────────────────────────────────────────────────────
        if (reply == null || reply.isBlank()) {
            reply = "I wasn't able to generate a response. Please try again.";
            dirty = true;
        } else if (reply.length() > MAX_REPLY_CHARS) {
            log.warn("[GUARDRAIL] Chat reply truncated from {} to {} chars", reply.length(), MAX_REPLY_CHARS);
            reply = reply.substring(0, MAX_REPLY_CHARS) + "…";
            dirty = true;
        }

        // ── Suggested section ─────────────────────────────────────────────────
        if (suggestedSection != null) {
            String normalised = suggestedSection.trim().toLowerCase();
            if (!VALID_SECTIONS.contains(normalised)) {
                log.warn("[GUARDRAIL] AI returned unknown suggestedSection='{}' — nulling it", suggestedSection);
                suggestedSection = null;
                suggestedContent = null; // content is meaningless without a valid section
                dirty = true;
            } else {
                suggestedSection = normalised; // ensure canonical lowercase form
            }
        }

        // ── Suggested content ─────────────────────────────────────────────────
        if (suggestedContent != null) {
            if (suggestedSection == null) {
                // Content with no section target makes no sense — discard it
                log.warn("[GUARDRAIL] suggestedContent present but suggestedSection is null — discarding content");
                suggestedContent = null;
                dirty = true;
            } else if (suggestedContent.isBlank()) {
                suggestedContent = null;
                dirty = true;
            } else if (suggestedContent.length() > MAX_SUGGESTED_CONTENT_CHARS) {
                log.warn("[GUARDRAIL] suggestedContent truncated from {} to {} chars",
                        suggestedContent.length(), MAX_SUGGESTED_CONTENT_CHARS);
                suggestedContent = suggestedContent.substring(0, MAX_SUGGESTED_CONTENT_CHARS);
                dirty = true;
            }
        }

        return new ValidationResult(reply, suggestedSection, suggestedContent, dirty);
    }
}
