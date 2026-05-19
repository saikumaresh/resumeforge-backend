package com.resumeforge.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class TailorResumeRequest {

    @NotNull
    private UUID userId;

    @NotBlank
    private String companyName;

    @NotBlank
    private String jobTitle;

    @NotBlank
    private String jobDescription;

    private String requiredSkills;
}
