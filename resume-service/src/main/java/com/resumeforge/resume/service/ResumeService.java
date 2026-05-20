package com.resumeforge.resume.service;

import com.resumeforge.resume.dto.*;
import com.resumeforge.resume.kafka.event.TailoringRequestedEvent;
import com.resumeforge.resume.kafka.producer.TailoringProducer;
import com.resumeforge.resume.model.*;
import com.resumeforge.resume.repository.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

@Service
public class ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    private final MasterResumeRepository masterResumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final TailoredResumeRepository tailoredResumeRepository;
    private final TailoringProducer tailoringProducer;
    private final Counter tailoringRequestCounter;

    public ResumeService(MasterResumeRepository masterResumeRepository,
                         JobDescriptionRepository jobDescriptionRepository,
                         TailoredResumeRepository tailoredResumeRepository,
                         TailoringProducer tailoringProducer,
                         MeterRegistry meterRegistry) {
        this.masterResumeRepository = masterResumeRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.tailoredResumeRepository = tailoredResumeRepository;
        this.tailoringProducer = tailoringProducer;
        this.tailoringRequestCounter = Counter.builder("resumeforge.tailoring.requests")
                .description("Total number of resume tailoring requests")
                .register(meterRegistry);
    }

    @Transactional
    public MasterResumeResponse createMasterResume(UUID userId, CreateMasterResumeRequest request) {
        MDC.put("userId", userId.toString());
        log.info("Creating master resume for user={}", userId);

        MasterResume resume = new MasterResume();
        resume.setUserId(userId);
        resume.setTitle(request.getTitle());
        resume.setSummary(request.getSummary());

        if (request.getSections() != null) {
            List<MasterResumeSection> sections = request.getSections().stream()
                    .map(s -> {
                        MasterResumeSection sec = new MasterResumeSection();
                        sec.setMasterResume(resume);
                        sec.setSectionType(MasterResumeSection.SectionType.valueOf(s.getSectionType()));
                        sec.setContent(s.getContent());
                        sec.setPosition(s.getPosition());
                        return sec;
                    })
                    .collect(Collectors.toList());
            resume.setSections(sections);
        }

        MasterResume saved = masterResumeRepository.save(resume);
        log.info("Master resume created resumeId={}", saved.getId());
        MDC.clear();
        return toMasterResumeResponse(saved);
    }

    public List<MasterResumeResponse> getMasterResumes(UUID userId) {
        return masterResumeRepository.findByUserId(userId).stream()
                .map(this::toMasterResumeResponse)
                .collect(Collectors.toList());
    }

    public MasterResumeResponse getMasterResumeWithSections(UUID resumeId) {
        MasterResume resume = masterResumeRepository.findByIdWithSections(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found: " + resumeId));
        return toMasterResumeResponse(resume);
    }

    @Transactional
    public TailoredResumeResponse triggerTailoring(UUID masterResumeId, TailorResumeRequest request) {
        MDC.put("masterResumeId", masterResumeId.toString());

        MasterResume masterResume = masterResumeRepository.findByIdWithSections(masterResumeId)
                .orElseThrow(() -> new RuntimeException("Master resume not found: " + masterResumeId));

        JobDescription jd = new JobDescription();
        jd.setUserId(request.getUserId());
        jd.setCompanyName(request.getCompanyName());
        jd.setJobTitle(request.getJobTitle());
        jd.setDescription(request.getJobDescription());
        jd.setRequiredSkills(request.getRequiredSkills());
        JobDescription savedJd = jobDescriptionRepository.save(jd);

        TailoredResume tailoredResume = new TailoredResume();
        tailoredResume.setMasterResume(masterResume);
        tailoredResume.setJobDescription(savedJd);
        tailoredResume.setStatus(TailoredResume.TailoringStatus.PENDING);
        TailoredResume saved = tailoredResumeRepository.save(tailoredResume);

        String masterContent = masterResume.getSections().stream()
                .map(s -> s.getSectionType() + ":\n" + s.getContent())
                .collect(Collectors.joining("\n\n"));

        TailoringRequestedEvent event = new TailoringRequestedEvent(
                saved.getId(), masterResumeId, savedJd.getId(),
                request.getUserId(), masterContent, request.getJobDescription()
        );
        tailoringProducer.publish(event);
        tailoringRequestCounter.increment();

        log.info("Tailoring triggered tailoredResumeId={} jobTitle={}", saved.getId(), request.getJobTitle());
        MDC.clear();
        return toTailoredResumeResponse(saved);
    }

    @Transactional
    public TailoredResumeResponse retryTailoring(UUID tailoredResumeId) {
        MDC.put("tailoredResumeId", tailoredResumeId.toString());

        TailoredResume tailoredResume = tailoredResumeRepository.findById(tailoredResumeId)
                .orElseThrow(() -> new RuntimeException("Tailored resume not found: " + tailoredResumeId));

        if (tailoredResume.getStatus() != TailoredResume.TailoringStatus.FAILED) {
            throw new IllegalStateException("Can only retry FAILED tailoring jobs (current status: " + tailoredResume.getStatus() + ")");
        }

        // Reset status to PENDING
        tailoredResume.setStatus(TailoredResume.TailoringStatus.PENDING);
        tailoredResumeRepository.save(tailoredResume);

        // Re-build master content
        MasterResume masterResume = masterResumeRepository.findByIdWithSections(tailoredResume.getMasterResume().getId())
                .orElseThrow(() -> new RuntimeException("Master resume not found"));

        String masterContent = masterResume.getSections().stream()
                .map(s -> s.getSectionType() + ":\n" + s.getContent())
                .collect(Collectors.joining("\n\n"));

        JobDescription jd = tailoredResume.getJobDescription();

        TailoringRequestedEvent event = new TailoringRequestedEvent(
                tailoredResume.getId(), masterResume.getId(), jd.getId(),
                jd.getUserId(), masterContent, jd.getDescription()
        );
        tailoringProducer.publish(event);
        tailoringRequestCounter.increment();

        log.info("Tailoring retried tailoredResumeId={}", tailoredResumeId);
        MDC.clear();
        return toTailoredResumeResponse(tailoredResume);
    }

    public TailoredResumeResponse getTailoredResume(UUID tailoredResumeId) {
        TailoredResume resume = tailoredResumeRepository.findByIdWithSectionsAndScore(tailoredResumeId)
                .orElseGet(() -> tailoredResumeRepository.findById(tailoredResumeId)
                        .orElseThrow(() -> new RuntimeException("Tailored resume not found: " + tailoredResumeId)));
        return toTailoredResumeResponse(resume);
    }

    public List<TailoredResumeResponse> getUserTailoredResumes(UUID userId) {
        return tailoredResumeRepository.findByUserIdWithSections(userId).stream()
                .map(this::toTailoredResumeResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TailoredResumeResponse updateTailoredSections(UUID tailoredResumeId, Map<String, String> newSections) {
        TailoredResume resume = tailoredResumeRepository.findById(tailoredResumeId)
                .orElseThrow(() -> new RuntimeException("Tailored resume not found: " + tailoredResumeId));

        // Update existing sections content
        resume.getSections().forEach(section -> {
            String key = section.getSectionType().name().toLowerCase();
            if (newSections.containsKey(key)) {
                section.setContent(newSections.get(key));
            }
        });

        TailoredResume saved = tailoredResumeRepository.save(resume);
        return toTailoredResumeResponse(saved);
    }

    @Transactional
    public MasterResumeResponse upsertMasterResume(UUID userId, String content) {
        List<MasterResume> existing = masterResumeRepository.findByUserId(userId);
        MasterResume resume;
        if (!existing.isEmpty()) {
            resume = masterResumeRepository.findByIdWithSections(existing.get(0).getId())
                    .orElse(existing.get(0));
        } else {
            resume = new MasterResume();
            resume.setUserId(userId);
            resume.setTitle("My Resume");
        }

        // Replace sections with single OTHER section holding full content
        resume.getSections().clear();
        MasterResumeSection section = new MasterResumeSection();
        section.setMasterResume(resume);
        section.setSectionType(MasterResumeSection.SectionType.OTHER);
        section.setContent(content);
        section.setPosition(1);
        resume.getSections().add(section);
        resume.setSummary(content.substring(0, Math.min(500, content.length())));

        MasterResume saved = masterResumeRepository.save(resume);
        return toMasterResumeResponse(saved);
    }

    private MasterResumeResponse toMasterResumeResponse(MasterResume r) {
        MasterResumeResponse resp = new MasterResumeResponse();
        resp.setId(r.getId());
        resp.setUserId(r.getUserId());
        resp.setTitle(r.getTitle());
        resp.setSummary(r.getSummary());
        resp.setVersion(r.getVersion());
        resp.setCreatedAt(r.getCreatedAt());
        if (r.getSections() != null) {
            resp.setSections(r.getSections().stream().map(s -> {
                MasterResumeResponse.SectionResponse sr = new MasterResumeResponse.SectionResponse();
                sr.setId(s.getId());
                sr.setSectionType(s.getSectionType().name());
                sr.setContent(s.getContent());
                sr.setPosition(s.getPosition());
                return sr;
            }).collect(Collectors.toList()));
        }
        return resp;
    }

    private TailoredResumeResponse toTailoredResumeResponse(TailoredResume r) {
        TailoredResumeResponse resp = new TailoredResumeResponse();
        resp.setId(r.getId());
        resp.setMasterResumeId(r.getMasterResume().getId());
        resp.setJobDescriptionId(r.getJobDescription().getId());
        resp.setStatus(r.getStatus().name());
        resp.setPdfDownloadUrl(r.getPdfPath() != null ? "/api/v1/exports/" + r.getId() + "/pdf" : null);
        resp.setCreatedAt(r.getCreatedAt());

        // Job details
        if (r.getJobDescription() != null) {
            resp.setCompanyName(r.getJobDescription().getCompanyName());
            resp.setJobTitle(r.getJobDescription().getJobTitle());
        }

        // ATS score breakdown
        if (r.getAtsScoreResult() != null) {
            resp.setAtsScore(r.getAtsScoreResult().getTotalScore());
            resp.setKeywordScore(r.getAtsScoreResult().getKeywordScore());
            resp.setSectionScore(r.getAtsScoreResult().getSectionScore());
            resp.setActionVerbScore(r.getAtsScoreResult().getActionVerbScore());
            resp.setMissingKeywords(r.getAtsScoreResult().getMissingKeywords());
        }

        // Tailored sections as Map<sectionName, content>
        if (r.getSections() != null && !r.getSections().isEmpty()) {
            Map<String, String> sections = new LinkedHashMap<>();
            r.getSections().forEach(s ->
                sections.put(s.getSectionType().name().toLowerCase(), s.getContent()));
            resp.setTailoredSections(sections);
        }

        return resp;
    }
}
