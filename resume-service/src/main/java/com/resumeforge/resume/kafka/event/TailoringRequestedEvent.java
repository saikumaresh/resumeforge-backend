package com.resumeforge.resume.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TailoringRequestedEvent {
    private UUID tailoredResumeId;
    private UUID masterResumeId;
    private UUID jobDescriptionId;
    private UUID userId;
    private String masterResumeContent;
    private String jobDescriptionText;
}
