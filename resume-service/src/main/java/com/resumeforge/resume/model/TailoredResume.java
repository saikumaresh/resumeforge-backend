package com.resumeforge.resume.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tailored_resumes")
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
    private TailoringStatus status = TailoringStatus.PENDING;

    @Version
    private Integer version;

    @Column(name = "pdf_path")
    private String pdfPath;

    @OneToOne(mappedBy = "tailoredResume", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private ATSScoreResult atsScoreResult;

    @OneToMany(mappedBy = "tailoredResume", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    private List<TailoredResumeSection> sections = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public TailoredResume() {}

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum TailoringStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public MasterResume getMasterResume() { return masterResume; }
    public void setMasterResume(MasterResume masterResume) { this.masterResume = masterResume; }
    public JobDescription getJobDescription() { return jobDescription; }
    public void setJobDescription(JobDescription jobDescription) { this.jobDescription = jobDescription; }
    public TailoringStatus getStatus() { return status; }
    public void setStatus(TailoringStatus status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
    public ATSScoreResult getAtsScoreResult() { return atsScoreResult; }
    public void setAtsScoreResult(ATSScoreResult atsScoreResult) { this.atsScoreResult = atsScoreResult; }
    public List<TailoredResumeSection> getSections() { return sections; }
    public void setSections(List<TailoredResumeSection> sections) { this.sections = sections; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
