package com.resumeforge.resume.controller;

import com.resumeforge.resume.dto.*;
import com.resumeforge.resume.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping("/users/{userId}/master")
    public ResponseEntity<MasterResumeResponse> createMasterResume(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateMasterResumeRequest request) {
        return ResponseEntity.ok(resumeService.createMasterResume(userId, request));
    }

    @GetMapping("/users/{userId}/master")
    public ResponseEntity<List<MasterResumeResponse>> getMasterResumes(@PathVariable UUID userId) {
        return ResponseEntity.ok(resumeService.getMasterResumes(userId));
    }

    @GetMapping("/{resumeId}/with-sections")
    public ResponseEntity<MasterResumeResponse> getMasterResumeWithSections(@PathVariable UUID resumeId) {
        return ResponseEntity.ok(resumeService.getMasterResumeWithSections(resumeId));
    }

    @PostMapping("/{masterResumeId}/tailor")
    public ResponseEntity<TailoredResumeResponse> tailorResume(
            @PathVariable UUID masterResumeId,
            @Valid @RequestBody TailorResumeRequest request) {
        return ResponseEntity.ok(resumeService.triggerTailoring(masterResumeId, request));
    }

    @GetMapping("/tailored/{tailoredResumeId}")
    public ResponseEntity<TailoredResumeResponse> getTailoredResume(@PathVariable UUID tailoredResumeId) {
        return ResponseEntity.ok(resumeService.getTailoredResume(tailoredResumeId));
    }
}
