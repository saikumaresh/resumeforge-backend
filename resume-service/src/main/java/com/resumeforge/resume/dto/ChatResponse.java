package com.resumeforge.resume.dto;

public class ChatResponse {
    private String reply;
    private String suggestedSection;
    private String suggestedContent;

    public ChatResponse(String reply, String suggestedSection, String suggestedContent) {
        this.reply = reply;
        this.suggestedSection = suggestedSection;
        this.suggestedContent = suggestedContent;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }

    public String getSuggestedSection() { return suggestedSection; }
    public void setSuggestedSection(String suggestedSection) { this.suggestedSection = suggestedSection; }

    public String getSuggestedContent() { return suggestedContent; }
    public void setSuggestedContent(String suggestedContent) { this.suggestedContent = suggestedContent; }
}
