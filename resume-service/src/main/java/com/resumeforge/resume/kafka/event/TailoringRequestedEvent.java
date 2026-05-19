package com.resumeforge.resume.kafka.event;

import java.util.UUID;

public class TailoringRequestedEvent {

    private UUID tailoredResumeId;
    private UUID masterResumeId;
    private UUID jobDescriptionId;
    private UUID userId;
    private String masterResumeContent;
    private String jobDescriptionText;

    public TailoringRequestedEvent() {}

    public TailoringRequestedEvent(UUID tailoredResumeId, UUID masterResumeId, UUID jobDescriptionId,
                                    UUID userId, String masterResumeContent, String jobDescriptionText) {
        this.tailoredResumeId = tailoredResumeId;
        this.masterResumeId = masterResumeId;
        this.jobDescriptionId = jobDescriptionId;
        this.userId = userId;
        this.masterResumeContent = masterResumeContent;
        this.jobDescriptionText = jobDescriptionText;
    }

    public UUID getTailoredResumeId() { return tailoredResumeId; }
    public void setTailoredResumeId(UUID tailoredResumeId) { this.tailoredResumeId = tailoredResumeId; }
    public UUID getMasterResumeId() { return masterResumeId; }
    public void setMasterResumeId(UUID masterResumeId) { this.masterResumeId = masterResumeId; }
    public UUID getJobDescriptionId() { return jobDescriptionId; }
    public void setJobDescriptionId(UUID jobDescriptionId) { this.jobDescriptionId = jobDescriptionId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getMasterResumeContent() { return masterResumeContent; }
    public void setMasterResumeContent(String masterResumeContent) { this.masterResumeContent = masterResumeContent; }
    public String getJobDescriptionText() { return jobDescriptionText; }
    public void setJobDescriptionText(String jobDescriptionText) { this.jobDescriptionText = jobDescriptionText; }
}
