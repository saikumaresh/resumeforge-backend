package com.resumeforge.resume.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class TailoredResumeResponse {

    private UUID id;
    private UUID masterResumeId;
    private UUID jobDescriptionId;
    private String status;
    private Integer atsScore;
    private String pdfDownloadUrl;
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMasterResumeId() { return masterResumeId; }
    public void setMasterResumeId(UUID masterResumeId) { this.masterResumeId = masterResumeId; }
    public UUID getJobDescriptionId() { return jobDescriptionId; }
    public void setJobDescriptionId(UUID jobDescriptionId) { this.jobDescriptionId = jobDescriptionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getAtsScore() { return atsScore; }
    public void setAtsScore(Integer atsScore) { this.atsScore = atsScore; }
    public String getPdfDownloadUrl() { return pdfDownloadUrl; }
    public void setPdfDownloadUrl(String pdfDownloadUrl) { this.pdfDownloadUrl = pdfDownloadUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
