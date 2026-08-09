package com.resumeforge.worker.scoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the ATS scorer that actually runs in production.
 *
 * worker-service is the module TailoringConsumer calls when it scores a
 * tailored resume. An identically named class exists in resume-service, but no
 * production path reaches it, and the two copies have diverged: this one uses a
 * considerably larger stop-word list. Testing this copy therefore describes
 * real behaviour, which the other suite does not.
 *
 * KeywordExtractor is used directly instead of being mocked. It is a pure
 * function with no collaborators of its own, so substituting a double would
 * only assert that the scorer calls something, not that the score is right.
 */
class ATSScorerTest {

    private ATSScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new ATSScorer(new KeywordExtractor());
    }

    // ────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("scoreKeywords")
    class ScoreKeywords {

        @Test
        @DisplayName("full overlap between resume and posting scores 100")
        void fullOverlap() {
            String text = "Java Spring Boot Kafka PostgreSQL distributed systems";
            assertEquals(100, scorer.scoreKeywords(text, text));
        }

        @Test
        @DisplayName("no overlap scores 0")
        void noOverlap() {
            int score = scorer.scoreKeywords(
                    "gardening horticulture botany greenhouse cultivation",
                    "Java Spring Kafka PostgreSQL Kubernetes");
            assertEquals(0, score);
        }

        @Test
        @DisplayName("partial overlap scores proportionally")
        void partialOverlap() {
            int score = scorer.scoreKeywords(
                    "Java Spring experience building services",
                    "Java Spring Kafka Kubernetes");
            assertTrue(score > 0 && score < 100,
                    "expected a partial score, got " + score);
        }

        @Test
        @DisplayName("an empty job description yields the neutral 50 rather than dividing by zero")
        void emptyJobDescription() {
            assertEquals(50, scorer.scoreKeywords("Java Spring Kafka", ""));
        }

        @Test
        @DisplayName("matching ignores case")
        void caseInsensitive() {
            assertEquals(100, scorer.scoreKeywords("JAVA SPRING KAFKA", "java spring kafka"));
        }
    }

    // ────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("scoreSections")
    class ScoreSections {

        @Test
        @DisplayName("all four required sections score 100")
        void allPresent() {
            assertEquals(100, scorer.scoreSections(Set.of("SUMMARY", "EXPERIENCE", "EDUCATION", "SKILLS")));
        }

        @Test
        @DisplayName("half the required sections score 50")
        void halfPresent() {
            assertEquals(50, scorer.scoreSections(Set.of("SUMMARY", "EXPERIENCE")));
        }

        @Test
        @DisplayName("an empty set scores 0")
        void nonePresent() {
            assertEquals(0, scorer.scoreSections(Set.of()));
        }

        @Test
        @DisplayName("a null set scores 0 rather than throwing")
        void nullSet() {
            assertEquals(0, scorer.scoreSections(null));
        }

        @Test
        @DisplayName("sections outside the required set do not raise the score")
        void extraSectionsIgnored() {
            assertEquals(50, scorer.scoreSections(Set.of("SUMMARY", "EXPERIENCE", "PROJECTS", "OTHER")));
        }
    }

    // ────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("scoreActionVerbs")
    class ScoreActionVerbs {

        @Test
        @DisplayName("each recognised verb contributes twelve points")
        void countsVerbs() {
            assertEquals(24, scorer.scoreActionVerbs("Developed services and designed the schema."));
        }

        @Test
        @DisplayName("prose with no action verbs scores 0")
        void noVerbs() {
            assertEquals(0, scorer.scoreActionVerbs("Responsible for various tasks and duties."));
        }

        @Test
        @DisplayName("the score is capped at 100 however many verbs appear")
        void capped() {
            String everyVerb = "developed built designed implemented led managed created "
                    + "optimised improved reduced increased delivered architected deployed "
                    + "automated scaled integrated";
            assertEquals(100, scorer.scoreActionVerbs(everyVerb));
        }

        @Test
        @DisplayName("null content scores 0 rather than throwing")
        void nullContent() {
            assertEquals(0, scorer.scoreActionVerbs(null));
        }
    }

    // ────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("score (weighted total)")
    class WeightedTotal {

        @Test
        @DisplayName("applies the documented 50/30/20 weighting")
        void weighting() {
            // Keywords match fully (100), all sections present (100), two verbs (24).
            String resume = "Java Spring Kafka. Developed and designed the platform.";
            String jd = "Java Spring Kafka";
            var b = scorer.score(resume, jd, Set.of("SUMMARY", "EXPERIENCE", "EDUCATION", "SKILLS"));

            assertEquals(100, b.keywordScore());
            assertEquals(100, b.sectionScore());
            assertEquals(24, b.actionVerbScore());
            // 100*0.5 + 100*0.3 + 24*0.2 = 84.8, truncated to 84
            assertEquals(84, b.totalScore());
        }

        @Test
        @DisplayName("reports the posting's keywords that the resume is missing")
        void reportsMissingKeywords() {
            var b = scorer.score("Java Spring experience",
                    "Java Spring Kubernetes Terraform",
                    Set.of("SUMMARY"));
            String missing = b.missingKeywords().toLowerCase();
            assertTrue(missing.contains("kubernetes"), "expected kubernetes in: " + missing);
            assertTrue(missing.contains("terraform"), "expected terraform in: " + missing);
            assertFalse(missing.contains("java"), "matched keywords must not be reported missing");
        }

        @Test
        @DisplayName("the total never exceeds 100")
        void totalBounded() {
            String everyVerb = "developed built designed implemented led managed created optimised";
            var b = scorer.score("Java " + everyVerb, "Java",
                    Set.of("SUMMARY", "EXPERIENCE", "EDUCATION", "SKILLS"));
            assertTrue(b.totalScore() <= 100, "total was " + b.totalScore());
        }

        @Test
        @DisplayName("an empty resume against a real posting scores low but does not throw")
        void emptyResume() {
            var b = scorer.score("", "Java Spring Kafka", Set.of());
            assertEquals(0, b.keywordScore());
            assertEquals(0, b.sectionScore());
            assertEquals(0, b.actionVerbScore());
            assertEquals(0, b.totalScore());
        }
    }
}
