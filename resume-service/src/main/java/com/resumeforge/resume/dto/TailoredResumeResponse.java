package com.resumeforge.resume.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import java.util.Map;

public class TailoredResumeResponse {

    private UUID id;
    private UUID masterResumeId;
    private UUID jobDescriptionId;
    private UUID userId;
    private String status;
    private Integer atsScore;
    private Integer keywordScore;
    private Integer sectionScore;
    private Integer actionVerbScore;
    private String missingKeywords;
    private String pdfDownloadUrl;
    private Map<String, String> tailoredSections;
    private String companyName;
    private String jobTitle;
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMasterResumeId() { return masterResumeId; }
    public void setMasterResumeId(UUID masterResumeId) { this.masterResumeId = masterResumeId; }
    public UUID getJobDescriptionId() { return jobDescriptionId; }
    public void setJobDescriptionId(UUID jobDescriptionId) { this.jobDescriptionId = jobDescriptionId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getAtsScore() { return atsScore; }
    public void setAtsScore(Integer atsScore) { this.atsScore = atsScore; }
    public Integer getKeywordScore() { return keywordScore; }
    public void setKeywordScore(Integer keywordScore) { this.keywordScore = keywordScore; }
    public Integer getSectionScore() { return sectionScore; }
    public void setSectionScore(Integer sectionScore) { this.sectionScore = sectionScore; }
    public Integer getActionVerbScore() { return actionVerbScore; }
    public void setActionVerbScore(Integer actionVerbScore) { this.actionVerbScore = actionVerbScore; }
    public String getMissingKeywords() { return missingKeywords; }
    public void setMissingKeywords(String missingKeywords) { this.missingKeywords = missingKeywords; }
    public String getPdfDownloadUrl() { return pdfDownloadUrl; }
    public void setPdfDownloadUrl(String pdfDownloadUrl) { this.pdfDownloadUrl = pdfDownloadUrl; }
    public Map<String, String> getTailoredSections() { return tailoredSections; }
    public void setTailoredSections(Map<String, String> tailoredSections) { this.tailoredSections = tailoredSections; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
