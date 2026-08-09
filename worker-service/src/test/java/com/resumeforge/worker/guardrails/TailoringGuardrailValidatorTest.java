package com.resumeforge.worker.guardrails;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the validation applied to language-model output before it is persisted.
 *
 * The model is an untrusted content source: it can return the wrong keys,
 * conversational narration instead of resume text, empty placeholders, or
 * content reflecting an injection attempt carried in the job description. This
 * validator is the boundary that decides what reaches the database, so these
 * tests describe the guarantee the rest of the pipeline depends on.
 */
class TailoringGuardrailValidatorTest {

    private TailoringGuardrailValidator validator;

    /** A body long enough to clear the minimum-length rule. */
    private static String body(String s) {
        return s + " with enough substance to pass the minimum length rule.";
    }

    private static Map<String, String> complete() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("summary", body("Backend engineer"));
        m.put("experience", body("Built an event-driven platform"));
        m.put("skills", body("Java, Spring, Kafka"));
        m.put("education", body("BTech Computer Science"));
        return m;
    }

    @BeforeEach
    void setUp() {
        validator = new TailoringGuardrailValidator();
    }

    // ────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("well-formed output")
    class WellFormed {

        @Test
        @DisplayName("a complete set of sections validates with no warnings")
        void completeSections() {
            var r = validator.validate(complete(), "original resume");
            assertTrue(r.valid());
            assertEquals(4, r.sanitizedSections().size());
            assertTrue(r.errors().isEmpty());
            assertTrue(r.warnings().isEmpty(), "unexpected warnings: " + r.warnings());
        }

        @Test
        @DisplayName("section keys are normalised to lower case")
        void keysNormalised() {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("SUMMARY", body("Backend engineer"));
            m.put("Experience", body("Built a platform"));
            var r = validator.validate(m, "original");
            assertTrue(r.sanitizedSections().containsKey("summary"));
            assertTrue(r.sanitizedSections().containsKey("experience"));
        }

        @Test
        @DisplayName("surrounding whitespace is trimmed")
        void trimsWhitespace() {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("summary", "   " + body("Backend engineer") + "   ");
            var r = validator.validate(m, "original");
            assertFalse(r.sanitizedSections().get("summary").startsWith(" "));
        }
    }

    // ────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("malformed output is rejected or dropped")
    class Malformed {

        @Test
        @DisplayName("an empty map is invalid and produces an error")
        void emptyMap() {
            var r = validator.validate(Map.of(), "original");
            assertFalse(r.valid());
            assertFalse(r.errors().isEmpty());
        }

        @Test
        @DisplayName("a null map is invalid rather than throwing")
        void nullMap() {
            var r = validator.validate(null, "original");
            assertFalse(r.valid());
        }

        @Test
        @DisplayName("keys outside the allow-list are dropped with a warning")
        void unknownKeyDropped() {
            Map<String, String> m = complete();
            m.put("salary_expectation", body("Two hundred thousand"));
            var r = validator.validate(m, "original");
            assertFalse(r.sanitizedSections().containsKey("salary_expectation"),
                    "the model must not be able to invent new sections");
            assertTrue(r.warnings().stream().anyMatch(w -> w.contains("salary_expectation")));
        }

        @Test
        @DisplayName("a suspiciously short value is treated as a placeholder and dropped")
        void tooShortDropped() {
            Map<String, String> m = complete();
            m.put("summary", "N/A");
            var r = validator.validate(m, "original");
            assertFalse(r.sanitizedSections().containsKey("summary"));
        }

        @Test
        @DisplayName("a blank value is dropped")
        void blankDropped() {
            Map<String, String> m = complete();
            m.put("skills", "   ");
            var r = validator.validate(m, "original");
            assertFalse(r.sanitizedSections().containsKey("skills"));
        }

        @Test
        @DisplayName("an oversized section is truncated rather than stored whole")
        void oversizedTruncated() {
            Map<String, String> m = complete();
            m.put("experience", "x".repeat(7000));
            var r = validator.validate(m, "original");
            assertEquals(6000, r.sanitizedSections().get("experience").length());
            assertTrue(r.warnings().stream().anyMatch(w -> w.contains("truncated")));
        }

        @Test
        @DisplayName("output containing nothing usable is invalid")
        void allSectionsInvalid() {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("unknown_one", body("something"));
            m.put("unknown_two", body("something else"));
            var r = validator.validate(m, "original");
            assertFalse(r.valid());
            assertFalse(r.errors().isEmpty());
        }

        @Test
        @DisplayName("a missing required section is reported as a warning")
        void missingRequiredSectionWarned() {
            Map<String, String> m = complete();
            m.remove("education");
            var r = validator.validate(m, "original");
            assertTrue(r.warnings().stream().anyMatch(w -> w.contains("education")));
        }
    }

    // ────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("structural pre-check")
    class LooksLikeJson {

        @Test
        @DisplayName("recognises a JSON object")
        void acceptsJson() {
            assertTrue(validator.looksLikeJson("{\"summary\": \"text\"}"));
        }

        @Test
        @DisplayName("rejects prose")
        void rejectsProse() {
            assertFalse(validator.looksLikeJson("Certainly! Here is the tailored resume you asked for."));
        }

        @Test
        @DisplayName("rejects null and blank input rather than throwing")
        void rejectsNullAndBlank() {
            assertFalse(validator.looksLikeJson(null));
            assertFalse(validator.looksLikeJson("   "));
        }
    }
}
