package com.resumeforge.resume.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "tailored_resume_sections")
public class TailoredResumeSection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tailored_resume_id", nullable = false)
    private TailoredResume tailoredResume;

    @Column(name = "section_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MasterResumeSection.SectionType sectionType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer position;

    public TailoredResumeSection() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public TailoredResume getTailoredResume() { return tailoredResume; }
    public void setTailoredResume(TailoredResume tailoredResume) { this.tailoredResume = tailoredResume; }
    public MasterResumeSection.SectionType getSectionType() { return sectionType; }
    public void setSectionType(MasterResumeSection.SectionType sectionType) { this.sectionType = sectionType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
}
