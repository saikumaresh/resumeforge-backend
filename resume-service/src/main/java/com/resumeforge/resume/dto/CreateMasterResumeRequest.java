package com.resumeforge.resume.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class CreateMasterResumeRequest {

    @NotBlank
    private String title;
    private String summary;
    private List<SectionRequest> sections;

    @Data
    public static class SectionRequest {
        private String sectionType;
        private String content;
        private int position;
    }
}
