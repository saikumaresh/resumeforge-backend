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
@DisplayName("ATS Scorer Unit Tests")
class ATSScorerTest {

    @Mock
    private KeywordExtractor keywordExtractor;

    @InjectMocks
    private ATSScorer atsScorer;

    @Test
    @DisplayName("Should return 100 keyword score when all JD keywords are in resume")
    void shouldReturnFullScoreWhenAllKeywordsMatch() {
        Set<String> jdKeywords = Set.of("java", "spring", "kafka");
        Set<String> resumeKeywords = Set.of("java", "spring", "kafka", "docker");

        when(keywordExtractor.extractKeywords("jd content")).thenReturn(jdKeywords);
        when(keywordExtractor.extractKeywords("resume content")).thenReturn(resumeKeywords);

        int score = atsScorer.scoreKeywords("resume content", "jd content");

        assertThat(score).isEqualTo(100);
    }

    @Test
    @DisplayName("Should return 0 keyword score when no keywords match")
    void shouldReturnZeroScoreWhenNoKeywordsMatch() {
        when(keywordExtractor.extractKeywords("jd content")).thenReturn(Set.of("python", "aws"));
        when(keywordExtractor.extractKeywords("resume content")).thenReturn(Set.of("java", "spring"));

        int score = atsScorer.scoreKeywords("resume content", "jd content");

        assertThat(score).isEqualTo(0);
    }

    @Test
    @DisplayName("Should score 100% when all required sections are present")
    void shouldReturnFullSectionScoreWhenAllSectionsPresent() {
        Set<String> sections = Set.of("SUMMARY", "EXPERIENCE", "EDUCATION", "SKILLS");
        int score = atsScorer.scoreSections(sections);
        assertThat(score).isEqualTo(100);
    }

    @Test
    @DisplayName("Should score 50% when half the required sections are missing")
    void shouldReturnHalfSectionScoreWhenTwoSectionsMissing() {
        Set<String> sections = Set.of("SUMMARY", "SKILLS");
        int score = atsScorer.scoreSections(sections);
        assertThat(score).isEqualTo(50);
    }

    @Test
    @DisplayName("Should return 0 section score for null sections")
    void shouldReturnZeroForNullSections() {
        assertThat(atsScorer.scoreSections(null)).isEqualTo(0);
    }

    @Test
    @DisplayName("Action verb score should increase with more action verbs in resume")
    void shouldScoreHigherWithMoreActionVerbs() {
        String richResume = "I developed a system, built APIs, deployed services, and optimised performance";
        String weakResume = "I worked on various tasks";

        int richScore = atsScorer.scoreActionVerbs(richResume);
        int weakScore = atsScorer.scoreActionVerbs(weakResume);

        assertThat(richScore).isGreaterThan(weakScore);
    }

    @Test
    @DisplayName("Full score method should return ATSScoreBreakdown with valid total score")
    void shouldReturnBreakdownWithTotalScore() {
        when(keywordExtractor.extractKeywords(anyString())).thenReturn(Set.of("java", "spring"));

        ATSScorer.ATSScoreBreakdown result = atsScorer.score(
                "I developed a java spring application",
                "Looking for java spring developer",
                Set.of("SUMMARY", "EXPERIENCE", "EDUCATION", "SKILLS")
        );

        assertThat(result.totalScore()).isBetween(0, 100);
        assertThat(result.keywordScore()).isGreaterThanOrEqualTo(0);
        assertThat(result.sectionScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should return 50 keyword score when JD has no extractable keywords")
    void shouldReturnFiftyWhenJdHasNoKeywords() {
        when(keywordExtractor.extractKeywords(anyString())).thenReturn(Set.of());

        int score = atsScorer.scoreKeywords("resume content", "jd content");

        assertThat(score).isEqualTo(50);
    }

    @Test
    @DisplayName("Action verb score should be capped at 100")
    void actionVerbScoreShouldBeCappedAt100() {
        String richResume = "developed built designed implemented led managed created optimised improved reduced increased delivered architected deployed automated scaled integrated";
        int score = atsScorer.scoreActionVerbs(richResume);
        assertThat(score).isLessThanOrEqualTo(100);
    }
}
