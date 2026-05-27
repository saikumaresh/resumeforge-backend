package com.resumeforge.resume.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class ChatRequest {
    @NotBlank(message = "message must not be blank")
    private String message;
    private Map<String, String> sections;
    private String targetSection;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, String> getSections() { return sections; }
    public void setSections(Map<String, String> sections) { this.sections = sections; }

    public String getTargetSection() { return targetSection; }
    public void setTargetSection(String targetSection) { this.targetSection = targetSection; }
}
