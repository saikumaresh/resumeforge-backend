package com.resumeforge.resume.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "master_resume_sections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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

    public enum SectionType {
        SUMMARY, EXPERIENCE, EDUCATION, SKILLS, PROJECTS, CERTIFICATIONS, OTHER
    }
}
