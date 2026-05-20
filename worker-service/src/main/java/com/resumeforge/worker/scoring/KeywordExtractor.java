package com.resumeforge.worker.scoring;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeywordExtractor {

    private static final Set<String> STOP_WORDS = Set.of(
            // Articles, prepositions, conjunctions
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "into", "onto", "upon", "about", "above",
            "below", "between", "through", "during", "before", "after", "over",
            "under", "again", "further", "then", "once", "per",
            // Common verbs / auxiliaries
            "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "having", "will", "would", "could", "should",
            "may", "might", "must", "shall", "can", "need", "dare", "used",
            "make", "made", "making", "take", "took", "taking",
            "help", "helped", "helps", "helping",
            "work", "worked", "works", "working",
            "build", "built", "builds", "building",
            "create", "created", "creates", "creating",
            "ensure", "ensures", "ensuring", "ensured",
            "maintain", "maintains", "maintaining", "maintained",
            "provide", "provides", "providing", "provided",
            "develop", "develops", "developing", "developed",
            "support", "supports", "supporting", "supported",
            "manage", "manages", "managing", "managed",
            "design", "designs", "designing", "designed",
            "implement", "implements", "implementing", "implemented",
            "deliver", "delivers", "delivering", "delivered",
            "drive", "drives", "driving", "driven",
            "lead", "leads", "leading",
            // Common resume filler words
            "strong", "good", "great", "excellent", "best", "better",
            "able", "ability", "skills", "skill", "knowledge", "experience",
            "years", "year", "month", "months", "time", "times",
            "team", "teams", "member", "members", "cross", "functional",
            "senior", "junior", "lead", "staff", "principal",
            "software", "engineer", "engineering", "developer", "development",
            "technology", "technologies", "solution", "solutions",
            "system", "systems", "platform", "platforms",
            "product", "products", "project", "projects",
            "process", "processes", "business", "company",
            "using", "used", "various", "multiple", "across",
            "well", "high", "large", "new", "based", "related",
            "role", "roles", "position", "positions",
            "fast", "agile", "dynamic", "innovative", "strategic",
            "including", "included", "includes",
            "following", "following", "such", "each", "both",
            "within", "without", "around", "along",
            "also", "other", "another", "same", "different",
            "more", "most", "less", "least", "many", "much",
            "this", "that", "these", "those", "which", "what",
            "when", "where", "while", "their", "there", "here",
            "them", "they", "your", "you", "our", "we", "its",
            "has", "been", "with", "all", "any", "some"
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
            if (word.length() >= 4 && !STOP_WORDS.contains(word)) {
                found.add(word);
            }
        }

        return found;
    }
}
