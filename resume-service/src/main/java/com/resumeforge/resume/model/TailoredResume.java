package com.resumeforge.resume.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tailored_resumes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TailoredResume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_resume_id", nullable = false)
    private MasterResume masterResume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_description_id", nullable = false)
    private JobDescription jobDescription;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TailoringStatus status = TailoringStatus.PENDING;

    @Version
    private Integer version;

    @Column(name = "pdf_path")
    private String pdfPath;

    @OneToOne(mappedBy = "tailoredResume", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private ATSScoreResult atsScoreResult;

    @OneToMany(mappedBy = "tailoredResume", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    @Builder.Default
    private List<TailoredResumeSection> sections = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum TailoringStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}
