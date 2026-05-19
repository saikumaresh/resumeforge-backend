package com.resumeforge.resume.scoring;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class KeywordExtractor {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "is", "are", "was", "were", "be", "been"
    );

    private static final Set<String> TECH_KEYWORDS = Set.of(
            "java", "python", "spring", "springboot", "kafka", "redis", "docker",
            "kubernetes", "aws", "gcp", "azure", "sql", "postgresql", "mysql",
            "mongodb", "rest", "api", "microservices", "git", "ci/cd", "jenkins",
            "react", "angular", "nodejs", "typescript", "javascript",
            "spark", "hadoop", "tensorflow", "pytorch", "machine learning",
            "data engineering", "devops", "terraform", "ansible"
    );

    public Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();

        String lower = text.toLowerCase();
        Set<String> found = new HashSet<>();

        for (String keyword : TECH_KEYWORDS) {
            if (lower.contains(keyword)) {
                found.add(keyword);
            }
        }

        String[] words = lower.replaceAll("[^a-zA-Z0-9\\s/]", " ").split("\\s+");
        for (String word : words) {
            if (word.length() > 3 && !STOP_WORDS.contains(word)) {
                found.add(word);
            }
        }

        return found;
    }
}
