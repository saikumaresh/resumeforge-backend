package com.resumeforge.resume.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ats_score_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ATSScoreResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tailored_resume_id", nullable = false)
    private TailoredResume tailoredResume;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Column(name = "keyword_score")
    private Integer keywordScore;

    @Column(name = "section_score")
    private Integer sectionScore;

    @Column(name = "action_verb_score")
    private Integer actionVerbScore;

    @Column(name = "missing_keywords", columnDefinition = "TEXT")
    private String missingKeywords;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
