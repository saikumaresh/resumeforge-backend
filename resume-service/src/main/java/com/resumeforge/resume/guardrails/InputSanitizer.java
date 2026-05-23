package com.resumeforge.resume.guardrails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Sanitizes all user-controlled inputs before they are forwarded to the AI.
 *
 * Responsibilities:
 *   1. Detect prompt-injection attempts in chat messages and resume sections
 *   2. Enforce maximum length limits to prevent context overflow
 *   3. Strip control characters
 *
 * This does NOT block requests — it logs, flags, and lets the hardened system
 * prompt handle the rejection gracefully (better UX than a hard 400 error).
 * The only exception is content that is purely injection with no legitimate text.
 */
@Component
public class InputSanitizer {

    private static final Logger log = LoggerFactory.getLogger(InputSanitizer.class);

    // ── Limits ────────────────────────────────────────────────────────────────
    private static final int MAX_CHAT_MESSAGE_CHARS   = 2000;
    private static final int MAX_SECTION_CHARS        = 4000;
    private static final int MAX_TOTAL_CONTEXT_CHARS  = 12000;

    // ── Injection detection patterns ─────────────────────────────────────────
    /**
     * Patterns that commonly appear in prompt-injection attempts.
     * Covers: instruction override, role-switching, jailbreaks, encoded payloads.
     */
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            // Classic instruction overrides
            Pattern.compile("(?i)ignore\\s+(previous|above|all|prior)\\s+(instruction|prompt|rule|message|system)"),
            Pattern.compile("(?i)(new|updated|replacement|override)\\s+system\\s+(prompt|instruction|message)"),
            Pattern.compile("(?i)disregard\\s+(your|the|all)\\s+(previous|above|prior|system|guardrail|instruction|rule)"),
            Pattern.compile("(?i)forget\\s+(your|the|all)\\s+(instruction|rule|guardrail|training|system\\s+prompt)"),
            // Role switching
            Pattern.compile("(?i)you\\s+are\\s+now\\s+(a|an|the)?\\s*\\w"),
            Pattern.compile("(?i)act\\s+as\\s+(if\\s+you\\s+(are|were)|a\\s*(different|unrestricted|evil|harmful|jailbreak))"),
            Pattern.compile("(?i)pretend\\s+(you\\s+are|to\\s+be)\\s+(a|an)?\\s*(different|unrestricted|evil|harmful)"),
            Pattern.compile("(?i)role(-|\\s*)play\\s+(as|being)"),
            // Jailbreak keywords
            Pattern.compile("(?i)\\b(jailbreak|DAN\\s+mode|developer\\s+mode|god\\s+mode|unrestricted\\s+mode)\\b"),
            Pattern.compile("(?i)without\\s+(any|your)\\s+(restriction|limitation|guardrail|filter|safety\\s+rule)"),
            Pattern.compile("(?i)bypass\\s+(your|the)\\s+(safety|guardrail|filter|restriction)"),
            // Pseudo-markup injection
            Pattern.compile("(?i)\\[\\s*(system|admin|override|developer|root)\\s*\\]"),
            Pattern.compile("(?i)<\\s*(system|prompt|instruction|override)\\s*>"),
            // Base64 / encoded payloads (crude heuristic: long base64-like strings in chat)
            Pattern.compile("[A-Za-z0-9+/]{60,}={0,2}")
    );

    // ── Public API ─────────────────────────────────────────────────────────────

    public record SanitizationResult(
            String sanitized,
            boolean injectionDetected,
            String warningMessage
    ) {}

    /**
     * Sanitizes a user chat message.
     */
    public SanitizationResult sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return new SanitizationResult("", false, null);
        }

        // Strip control characters (keep newlines)
        message = message.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "").trim();

        // Enforce length
        if (message.length() > MAX_CHAT_MESSAGE_CHARS) {
            log.warn("[GUARDRAIL] Chat message truncated from {} to {} chars",
                    message.length(), MAX_CHAT_MESSAGE_CHARS);
            message = message.substring(0, MAX_CHAT_MESSAGE_CHARS);
        }

        // Injection scan
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(message).find()) {
                log.warn("[GUARDRAIL] Prompt injection pattern detected in chat message: pattern={}",
                        pattern.pattern());
                // Flag but do not block — let the system prompt handle it
                return new SanitizationResult(
                        message,
                        true,
                        "Your message contains patterns that may attempt to override the AI's safety rules. " +
                        "These rules are in place to keep your resume honest and accurate. " +
                        "Please rephrase your request or ask me to help improve your existing content."
                );
            }
        }

        return new SanitizationResult(message, false, null);
    }

    /**
     * Sanitizes the resume sections map passed with a chat request.
     * Sections come from the frontend (user-editable) so must be validated.
     */
    public Map<String, String> sanitizeSections(Map<String, String> sections) {
        if (sections == null || sections.isEmpty()) return Map.of();

        Map<String, String> sanitized = new LinkedHashMap<>();
        int totalChars = 0;

        for (Map.Entry<String, String> entry : sections.entrySet()) {
            String key   = entry.getKey();
            String value = entry.getValue();

            if (key == null || key.isBlank() || value == null || value.isBlank()) continue;

            // Enforce total context budget
            if (totalChars >= MAX_TOTAL_CONTEXT_CHARS) {
                log.warn("[GUARDRAIL] Sections context budget exhausted at key='{}' (total {} chars)", key, totalChars);
                break;
            }

            value = value.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "").trim();

            if (value.length() > MAX_SECTION_CHARS) {
                log.warn("[GUARDRAIL] Section '{}' truncated from {} to {} chars", key, value.length(), MAX_SECTION_CHARS);
                value = value.substring(0, MAX_SECTION_CHARS);
            }

            // Scan sections for injection too (malicious job description content, etc.)
            for (Pattern pattern : INJECTION_PATTERNS) {
                if (pattern.matcher(value).find()) {
                    log.warn("[GUARDRAIL] Injection pattern in section content key='{}', pattern={}",
                            key, pattern.pattern());
                    break; // Log and continue — the hardened system prompt handles it
                }
            }

            sanitized.put(key, value);
            totalChars += value.length();
        }

        return sanitized;
    }

    /**
     * Quick check — is this string suspiciously injection-heavy?
     * Used to detect messages that are PURELY injection with no real content.
     */
    public boolean isPureInjection(String text) {
        if (text == null || text.isBlank()) return false;
        int injectionMatches = 0;
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(text).find()) injectionMatches++;
        }
        // If more than 2 injection patterns match and the text is short, it's likely pure injection
        return injectionMatches >= 2 && text.length() < 500;
    }
}
