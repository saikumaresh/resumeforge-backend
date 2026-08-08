package com.resumeforge.resume.scoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Keyword Extractor Edge Cases Tests")
class KeywordExtractorEdgeCasesTest {

    private final KeywordExtractor extractor = new KeywordExtractor();

    @Test
    @DisplayName("Should handle whitespace-only input")
    void shouldHandleWhitespaceOnly() {
        Set<String> keywords = extractor.extractKeywords("     ");
        assertThat(keywords).isEmpty();
    }

    @Test
    @DisplayName("Should extract programming languages")
    void shouldExtractProgrammingLanguages() {
        Set<String> keywords = extractor.extractKeywords("C C++ CSharp Java");
        assertThat(keywords).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle mixed case correctly")
    void shouldHandleMixedCase() {
        Set<String> keywords = extractor.extractKeywords("JAVA java Java jAvA");
        long javaCount = keywords.stream().filter(k -> "java".equals(k)).count();
        assertThat(javaCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should extract common abbreviations")
    void shouldExtractAbbreviations() {
        Set<String> keywords = extractor.extractKeywords("AWS S3 EC2 RDS");
        assertThat(keywords).isNotEmpty();
        assertThat(keywords.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should handle repeated keywords")
    void shouldHandleRepeatedKeywords() {
        Set<String> keywords = extractor.extractKeywords("Java Java Java Java Java");
        assertThat(keywords).contains("java");
        assertThat(keywords.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should extract hyphenated keywords")
    void shouldExtractHyphenatedKeywords() {
        Set<String> keywords = extractor.extractKeywords("full-stack end-to-end real-time");
        assertThat(keywords).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle keywords with numbers")
    void shouldHandleKeywordsWithNumbers() {
        Set<String> keywords = extractor.extractKeywords("Python3 Node.js14 Java8");
        assertThat(keywords).isNotEmpty();
    }

    @Test
    @DisplayName("Should extract database keywords")
    void shouldExtractDatabaseKeywords() {
        Set<String> keywords = extractor.extractKeywords("PostgreSQL MongoDB Cassandra Redis");
        assertThat(keywords).isNotEmpty();
    }

    @Test
    @DisplayName("Should extract framework keywords")
    void shouldExtractFrameworkKeywords() {
        Set<String> keywords = extractor.extractKeywords("Spring Boot FastAPI Django Flask");
        assertThat(keywords).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle very long text efficiently")
    void shouldHandleLongTextEfficiently() {
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longText.append("Java Spring Boot ");
        }
        Set<String> keywords = extractor.extractKeywords(longText.toString());
        assertThat(keywords).isNotEmpty();
        assertThat(keywords.size()).isLessThan(200);
    }

    @Test
    @DisplayName("Should extract from real job description")
    void shouldExtractFromRealJobDesc() {
        String jobDesc = "We are looking for a Senior Java developer with 5+ years of experience. " +
                "Must have expertise in Spring Boot, Microservices, Kafka, and PostgreSQL. " +
                "Experience with AWS, Docker, and Kubernetes is a plus.";
        Set<String> keywords = extractor.extractKeywords(jobDesc);
        assertThat(keywords).isNotEmpty();
        assertThat(keywords.size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should handle special characters in text")
    void shouldHandleSpecialCharacters() {
        Set<String> keywords = extractor.extractKeywords("Java (Core) [Advanced] {Spring}");
        assertThat(keywords).isNotEmpty();
    }

    @Test
    @DisplayName("Should extract from comma-separated values")
    void shouldExtractFromCSV() {
        Set<String> keywords = extractor.extractKeywords("Java, Python, C++, Go, Rust");
        assertThat(keywords).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle URLs in text")
    void shouldHandleURLs() {
        Set<String> keywords = extractor.extractKeywords("Check out https://github.com/example REST APIs");
        assertThat(keywords).isNotEmpty();
    }

    @Test
    @DisplayName("Should extract acronyms")
    void shouldExtractAcronyms() {
        Set<String> keywords = extractor.extractKeywords("API REST JSON YAML XML");
        assertThat(keywords).isNotEmpty();
    }

    @Test
    @DisplayName("Should not include common stop words")
    void shouldExcludeStopWords() {
        Set<String> keywords = extractor.extractKeywords("the and or is a an in on at");
        assertThat(keywords).doesNotContain("the", "and", "or", "is", "a", "an", "in", "on", "at");
    }

    @Test
    @DisplayName("Should handle null input safely")
    void shouldHandleNullInputSafely() {
        Set<String> keywords = extractor.extractKeywords(null);
        assertThat(keywords).isEmpty();
    }

    @Test
    @DisplayName("Should extract technical terms from paragraphs")
    void shouldExtractFromParagraphs() {
        String paragraph = "Our company uses a microservices architecture with Docker containers " +
                "deployed on Kubernetes. We leverage Spring Cloud for service discovery and " +
                "Apache Kafka for event streaming.";
        Set<String> keywords = extractor.extractKeywords(paragraph);
        assertThat(keywords).isNotEmpty();
    }
}
