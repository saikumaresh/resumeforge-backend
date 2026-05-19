package com.resumeforge.resume.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ats_score_results")
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

    public ATSScoreResult() {}

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TailoredResume getTailoredResume() { return tailoredResume; }
    public void setTailoredResume(TailoredResume tailoredResume) { this.tailoredResume = tailoredResume; }
    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
    public Integer getKeywordScore() { return keywordScore; }
    public void setKeywordScore(Integer keywordScore) { this.keywordScore = keywordScore; }
    public Integer getSectionScore() { return sectionScore; }
    public void setSectionScore(Integer sectionScore) { this.sectionScore = sectionScore; }
    public Integer getActionVerbScore() { return actionVerbScore; }
    public void setActionVerbScore(Integer actionVerbScore) { this.actionVerbScore = actionVerbScore; }
    public String getMissingKeywords() { return missingKeywords; }
    public void setMissingKeywords(String missingKeywords) { this.missingKeywords = missingKeywords; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
