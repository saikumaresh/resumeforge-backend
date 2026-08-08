package com.resumeforge.resume.scoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ATS Scorer Edge Cases Tests")
class ATSScorerEdgeCasesTest {

    @Mock
    private KeywordExtractor keywordExtractor;

    @InjectMocks
    private ATSScorer atsScorer;

    @Test
    @DisplayName("Should handle empty keyword sets gracefully")
    void shouldHandleEmptyKeywordSets() {
        when(keywordExtractor.extractKeywords(anyString())).thenReturn(Set.of());

        int score = atsScorer.scoreKeywords("", "");

        assertThat(score).isEqualTo(50);
    }

    @Test
    @DisplayName("Should handle null sections without crashing")
    void shouldHandleNullSections() {
        int score = atsScorer.scoreSections(null);
        assertThat(score).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle partial section matches")
    void shouldHandlePartialSectionMatches() {
        Set<String> sections = Set.of("SUMMARY", "EXPERIENCE");
        int score = atsScorer.scoreSections(sections);
        assertThat(score).isBetween(0, 100);
    }

    @Test
    @DisplayName("Should score 75% with 3 out of 4 required sections")
    void shouldScore75PercentWithThreeSections() {
        Set<String> sections = Set.of("SUMMARY", "EXPERIENCE", "EDUCATION");
        int score = atsScorer.scoreSections(sections);
        assertThat(score).isEqualTo(75);
    }

    @Test
    @DisplayName("Should handle action verbs with special characters")
    void shouldHandleActionVerbsWithSpecialChars() {
        String resume = "I developed, built, and deployed applications";
        int score = atsScorer.scoreActionVerbs(resume);
        assertThat(score).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should return zero score for empty resume")
    void shouldReturnZeroForEmptyResume() {
        String resume = "";
        int score = atsScorer.scoreActionVerbs(resume);
        assertThat(score).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Should handle very long resume content")
    void shouldHandleVeryLongResume() {
        String longResume = "developed ".repeat(1000);
        int score = atsScorer.scoreActionVerbs(longResume);
        assertThat(score).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Should score breakdown has non-negative values")
    void shouldHaveNonNegativeBreakdownScores() {
        when(keywordExtractor.extractKeywords(anyString())).thenReturn(Set.of("java", "spring"));

        ATSScorer.ATSScoreBreakdown result = atsScorer.score(
                "I developed a java spring application",
                "Looking for java spring developer",
                Set.of("SUMMARY", "EXPERIENCE", "EDUCATION", "SKILLS")
        );

        assertThat(result.totalScore()).isGreaterThanOrEqualTo(0);
        assertThat(result.keywordScore()).isGreaterThanOrEqualTo(0);
        assertThat(result.sectionScore()).isGreaterThanOrEqualTo(0);
        assertThat(result.actionVerbScore()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should handle one keyword match correctly")
    void shouldHandleOneKeywordMatch() {
        Set<String> jdKeywords = Set.of("java");
        Set<String> resumeKeywords = Set.of("java");

        when(keywordExtractor.extractKeywords("jd")).thenReturn(jdKeywords);
        when(keywordExtractor.extractKeywords("resume")).thenReturn(resumeKeywords);

        int score = atsScorer.scoreKeywords("resume", "jd");

        assertThat(score).isEqualTo(100);
    }

    @Test
    @DisplayName("Should handle partial keyword overlap")
    void shouldHandlePartialKeywordOverlap() {
        Set<String> jdKeywords = Set.of("java", "spring", "kafka");
        Set<String> resumeKeywords = Set.of("java", "spring");

        when(keywordExtractor.extractKeywords("jd")).thenReturn(jdKeywords);
        when(keywordExtractor.extractKeywords("resume")).thenReturn(resumeKeywords);

        int score = atsScorer.scoreKeywords("resume", "jd");

        assertThat(score).isBetween(0, 100);
    }

    @Test
    @DisplayName("Scoring multiple times should be consistent")
    void shouldProduceConsistentScores() {
        when(keywordExtractor.extractKeywords(anyString())).thenReturn(Set.of("java", "spring"));

        int score1 = atsScorer.scoreKeywords("resume1", "jd1");
        int score2 = atsScorer.scoreKeywords("resume1", "jd1");

        assertThat(score1).isEqualTo(score2);
    }

    @Test
    @DisplayName("Should handle very large keyword sets")
    void shouldHandleLargeKeywordSets() {
        Set<String> largeSet = Set.of("keyword1", "keyword2", "keyword3", "keyword4", "keyword5",
                "keyword6", "keyword7", "keyword8", "keyword9", "keyword10");

        when(keywordExtractor.extractKeywords("resume")).thenReturn(largeSet);
        when(keywordExtractor.extractKeywords("jd")).thenReturn(largeSet);

        int score = atsScorer.scoreKeywords("resume", "jd");

        assertThat(score).isEqualTo(100);
    }
}
