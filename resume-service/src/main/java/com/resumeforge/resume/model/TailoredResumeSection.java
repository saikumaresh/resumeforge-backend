package com.resumeforge.resume.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "tailored_resume_sections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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
}
