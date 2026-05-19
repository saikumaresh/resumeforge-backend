package com.resumeforge.resume.scoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Keyword Extractor Unit Tests")
class KeywordExtractorTest {

    private final KeywordExtractor extractor = new KeywordExtractor();

    @Test
    @DisplayName("Should extract Java and Spring from tech job description")
    void shouldExtractTechKeywords() {
        String text = "We are looking for a Java developer with Spring experience";
        Set<String> keywords = extractor.extractKeywords(text);
        assertThat(keywords).contains("java", "spring");
    }

    @Test
    @DisplayName("Should return empty set for null input")
    void shouldReturnEmptyForNull() {
        assertThat(extractor.extractKeywords(null)).isEmpty();
    }

    @Test
    @DisplayName("Should handle case insensitive extraction")
    void shouldBeCaseInsensitive() {
        Set<String> keywords = extractor.extractKeywords("JAVA SPRING KAFKA");
        assertThat(keywords).contains("java", "spring", "kafka");
    }

    @Test
    @DisplayName("Should return empty set for blank input")
    void shouldReturnEmptyForBlank() {
        assertThat(extractor.extractKeywords("   ")).isEmpty();
    }

    @Test
    @DisplayName("Should filter out common stop words")
    void shouldFilterStopWords() {
        Set<String> keywords = extractor.extractKeywords("the and or but with from");
        assertThat(keywords).doesNotContain("the", "and", "or", "but");
    }

    @Test
    @DisplayName("Should extract multiple tech keywords from complex description")
    void shouldExtractMultipleTechKeywords() {
        String text = "Senior engineer needed with Docker, Kubernetes, and AWS experience. REST API design required.";
        Set<String> keywords = extractor.extractKeywords(text);
        assertThat(keywords).contains("docker", "kubernetes", "aws", "rest", "api");
    }
}
