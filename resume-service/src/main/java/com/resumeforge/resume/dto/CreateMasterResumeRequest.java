package com.resumeforge.resume.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CreateMasterResumeRequest {

    @NotBlank
    private String title;
    private String summary;
    private List<SectionRequest> sections;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<SectionRequest> getSections() { return sections; }
    public void setSections(List<SectionRequest> sections) { this.sections = sections; }

    public static class SectionRequest {
        private String sectionType;
        private String content;
        private int position;

        public String getSectionType() { return sectionType; }
        public void setSectionType(String sectionType) { this.sectionType = sectionType; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }
    }
}
