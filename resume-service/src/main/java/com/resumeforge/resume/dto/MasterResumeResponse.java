package com.resumeforge.resume.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class MasterResumeResponse {

    private UUID id;
    private UUID userId;
    private String title;
    private String summary;
    private Integer version;
    private LocalDateTime createdAt;
    private List<SectionResponse> sections;

    @Data
    public static class SectionResponse {
        private UUID id;
        private String sectionType;
        private String content;
        private int position;
    }
}
