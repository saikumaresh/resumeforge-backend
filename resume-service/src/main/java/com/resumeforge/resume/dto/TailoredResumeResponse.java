package com.resumeforge.resume.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TailoredResumeResponse {

    private UUID id;
    private UUID masterResumeId;
    private UUID jobDescriptionId;
    private String status;
    private Integer atsScore;
    private String pdfDownloadUrl;
    private LocalDateTime createdAt;
}
