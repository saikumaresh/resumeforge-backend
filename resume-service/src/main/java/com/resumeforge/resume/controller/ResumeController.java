package com.resumeforge.resume.controller;

import com.resumeforge.resume.dto.*;
import com.resumeforge.resume.service.ResumeService;
import com.resumeforge.resume.service.ResumeChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeChatService chatService;

    public ResumeController(ResumeService resumeService, ResumeChatService chatService) {
        this.resumeService = resumeService;
        this.chatService = chatService;
    }

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

    @PostMapping("/tailored/{tailoredResumeId}/retry")
    public ResponseEntity<TailoredResumeResponse> retryTailoring(@PathVariable UUID tailoredResumeId) {
        return ResponseEntity.ok(resumeService.retryTailoring(tailoredResumeId));
    }

    @GetMapping("/users/{userId}/tailored")
    public ResponseEntity<List<TailoredResumeResponse>> getUserTailoredResumes(@PathVariable UUID userId) {
        return ResponseEntity.ok(resumeService.getUserTailoredResumes(userId));
    }

    @PutMapping("/tailored/{tailoredResumeId}/sections")
    public ResponseEntity<TailoredResumeResponse> updateTailoredSections(
            @PathVariable UUID tailoredResumeId,
            @RequestBody java.util.Map<String, String> sections) {
        return ResponseEntity.ok(resumeService.updateTailoredSections(tailoredResumeId, sections));
    }

    @PostMapping("/tailored/{tailoredResumeId}/chat")
    public ResponseEntity<ChatResponse> chatWithResume(
            @PathVariable UUID tailoredResumeId,
            @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(request));
    }

    @PutMapping("/users/{userId}/master")
    public ResponseEntity<MasterResumeResponse> upsertMasterResume(
            @PathVariable UUID userId,
            @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(resumeService.upsertMasterResume(userId, body.get("content")));
    }

    @GetMapping("/users/{userId}/master/first")
    public ResponseEntity<MasterResumeResponse> getFirstMasterResume(@PathVariable UUID userId) {
        List<MasterResumeResponse> all = resumeService.getMasterResumes(userId);
        if (all.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(all.get(0));
    }
}
