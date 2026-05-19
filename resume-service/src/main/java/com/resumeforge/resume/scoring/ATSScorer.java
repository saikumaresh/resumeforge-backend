package com.resumeforge.resume.scoring;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ATSScorer {

    private static final Set<String> ACTION_VERBS = Set.of(
            "developed", "built", "designed", "implemented", "led", "managed",
            "created", "optimised", "improved", "reduced", "increased", "delivered",
            "architected", "deployed", "automated", "scaled", "integrated"
    );

    private static final Set<String> REQUIRED_SECTIONS = Set.of(
            "SUMMARY", "EXPERIENCE", "EDUCATION", "SKILLS"
    );

    private final KeywordExtractor keywordExtractor;

    public ATSScorer(KeywordExtractor keywordExtractor) {
        this.keywordExtractor = keywordExtractor;
    }

    public ATSScoreBreakdown score(String resumeContent, String jobDescriptionContent, Set<String> presentSections) {
        int keywordScore = scoreKeywords(resumeContent, jobDescriptionContent);
        int sectionScore = scoreSections(presentSections);
        int actionVerbScore = scoreActionVerbs(resumeContent);

        int totalScore = (int) (keywordScore * 0.5 + sectionScore * 0.3 + actionVerbScore * 0.2);

        Set<String> jdKeywords = keywordExtractor.extractKeywords(jobDescriptionContent);
        Set<String> resumeKeywords = keywordExtractor.extractKeywords(resumeContent);
        Set<String> missing = new HashSet<>(jdKeywords);
        missing.removeAll(resumeKeywords);
        String missingKeywordsStr = missing.stream().limit(10).collect(Collectors.joining(", "));

        return new ATSScoreBreakdown(totalScore, keywordScore, sectionScore, actionVerbScore, missingKeywordsStr);
    }

    public int scoreKeywords(String resumeContent, String jobDescriptionContent) {
        Set<String> jdKeywords = keywordExtractor.extractKeywords(jobDescriptionContent);
        if (jdKeywords.isEmpty()) return 50;

        Set<String> resumeKeywords = keywordExtractor.extractKeywords(resumeContent);
        long matchCount = jdKeywords.stream().filter(resumeKeywords::contains).count();
        double rate = (double) matchCount / jdKeywords.size();

        return (int) Math.min(100, rate * 100);
    }

    public int scoreSections(Set<String> presentSections) {
        if (presentSections == null || presentSections.isEmpty()) return 0;
        long count = REQUIRED_SECTIONS.stream()
                .filter(presentSections::contains)
                .count();
        return (int) ((double) count / REQUIRED_SECTIONS.size() * 100);
    }

    public int scoreActionVerbs(String resumeContent) {
        if (resumeContent == null) return 0;
        String lower = resumeContent.toLowerCase();
        long count = ACTION_VERBS.stream().filter(lower::contains).count();
        return (int) Math.min(100, count * 12);
    }

    public record ATSScoreBreakdown(
            int totalScore,
            int keywordScore,
            int sectionScore,
            int actionVerbScore,
            String missingKeywords
    ) {}
}
