package com.resumeforge.resume.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "master_resume_sections")
public class MasterResumeSection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_resume_id", nullable = false)
    private MasterResume masterResume;

    @Column(name = "section_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SectionType sectionType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer position;

    public MasterResumeSection() {}

    public enum SectionType {
        SUMMARY, EXPERIENCE, EDUCATION, SKILLS, PROJECTS, CERTIFICATIONS, OTHER
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public MasterResume getMasterResume() { return masterResume; }
    public void setMasterResume(MasterResume masterResume) { this.masterResume = masterResume; }
    public SectionType getSectionType() { return sectionType; }
    public void setSectionType(SectionType sectionType) { this.sectionType = sectionType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
