package com.resumeforge.resume.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MasterResumeResponse {

    private UUID id;
    private UUID userId;
    private String title;
    private String summary;
    private Integer version;
    private LocalDateTime createdAt;
    private List<SectionResponse> sections;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<SectionResponse> getSections() { return sections; }
    public void setSections(List<SectionResponse> sections) { this.sections = sections; }

    public static class SectionResponse {
        private UUID id;
        private String sectionType;
        private String content;
        private int position;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getSectionType() { return sectionType; }
        public void setSectionType(String sectionType) { this.sectionType = sectionType; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }
    }
}
