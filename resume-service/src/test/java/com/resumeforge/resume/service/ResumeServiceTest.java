package com.resumeforge.resume.service;

import com.resumeforge.resume.dto.CreateMasterResumeRequest;
import com.resumeforge.resume.dto.TailorResumeRequest;
import com.resumeforge.resume.kafka.event.TailoringRequestedEvent;
import com.resumeforge.resume.kafka.producer.TailoringProducer;
import com.resumeforge.resume.model.JobDescription;
import com.resumeforge.resume.model.MasterResume;
import com.resumeforge.resume.model.MasterResumeSection;
import com.resumeforge.resume.model.TailoredResume;
import com.resumeforge.resume.repository.JobDescriptionRepository;
import com.resumeforge.resume.repository.MasterResumeRepository;
import com.resumeforge.resume.repository.TailoredResumeRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ResumeService in isolation from Spring and the database.
 *
 * The collaborators are replaced with test doubles so that only the service's
 * own logic is under test:
 *
 *   - Mocks   : the three repositories and TailoringProducer. Interactions with
 *               these are the observable behaviour of most service methods, so
 *               they are asserted with verify() rather than only stubbed.
 *   - Stubs   : the same repositories, configured with when(...).thenReturn(...)
 *               to supply the state a given scenario needs.
 *   - Fake    : SimpleMeterRegistry — a real, in-memory MeterRegistry rather
 *               than a mock, because the service builds a Counter from it in
 *               its constructor and a mock would return null.
 *   - Captor  : ArgumentCaptor, to assert on the contents of the Kafka event
 *               the service publishes, not merely that it published something.
 */
@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock private MasterResumeRepository masterResumeRepository;
    @Mock private JobDescriptionRepository jobDescriptionRepository;
    @Mock private TailoredResumeRepository tailoredResumeRepository;
    @Mock private TailoringProducer tailoringProducer;

    @Captor private ArgumentCaptor<TailoringRequestedEvent> eventCaptor;
    @Captor private ArgumentCaptor<MasterResume> masterResumeCaptor;

    private MeterRegistry meterRegistry;
    private ResumeService service;

    private UUID callerId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new ResumeService(
                masterResumeRepository,
                jobDescriptionRepository,
                tailoredResumeRepository,
                tailoringProducer,
                meterRegistry);

        callerId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        authenticateAs(callerId);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** ResumeService reads the caller from the security context, so tests must populate it. */
    private void authenticateAs(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private MasterResume masterResumeOwnedBy(UUID ownerId) {
        MasterResume resume = new MasterResume();
        resume.setId(UUID.randomUUID());
        resume.setUserId(ownerId);
        resume.setTitle("Backend Engineer");
        resume.setSummary("Summary");

        MasterResumeSection section = new MasterResumeSection();
        section.setMasterResume(resume);
        section.setSectionType(MasterResumeSection.SectionType.EXPERIENCE);
        section.setContent("Built an event-driven backend");
        section.setPosition(0);
        resume.setSections(new ArrayList<>(List.of(section)));
        return resume;
    }

    private TailorResumeRequest tailorRequest() {
        TailorResumeRequest r = new TailorResumeRequest();
        r.setCompanyName("Acme");
        r.setJobTitle("Senior Backend Engineer");
        r.setJobDescription("Java, Spring Boot, Kafka");
        r.setRequiredSkills("Java, Kafka");
        return r;
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("createMasterResume")
    class CreateMasterResume {

        @Test
        @DisplayName("persists the resume for the authenticated owner")
        void savesForOwner() {
            CreateMasterResumeRequest request = new CreateMasterResumeRequest();
            request.setTitle("Backend Engineer");
            request.setSummary("Summary");

            when(masterResumeRepository.save(any(MasterResume.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.createMasterResume(callerId, request);

            // Assert on what was actually handed to the repository, not just that it was called.
            verify(masterResumeRepository).save(masterResumeCaptor.capture());
            MasterResume saved = masterResumeCaptor.getValue();
            assertEquals(callerId, saved.getUserId());
            assertEquals("Backend Engineer", saved.getTitle());
        }

        @Test
        @DisplayName("refuses to create a resume for another user and writes nothing")
        void refusesForeignOwner() {
            CreateMasterResumeRequest request = new CreateMasterResumeRequest();
            request.setTitle("Backend Engineer");

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.createMasterResume(otherUserId, request));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            // The important assertion: authorisation fails closed, before any write.
            verify(masterResumeRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects an unknown section type with 400 rather than persisting it")
        void rejectsUnknownSectionType() {
            CreateMasterResumeRequest request = new CreateMasterResumeRequest();
            request.setTitle("Backend Engineer");
            CreateMasterResumeRequest.SectionRequest bad = new CreateMasterResumeRequest.SectionRequest();
            bad.setSectionType("NOT_A_SECTION");
            bad.setContent("x");
            request.setSections(List.of(bad));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.createMasterResume(callerId, request));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            verify(masterResumeRepository, never()).save(any());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getMasterResumes")
    class GetMasterResumes {

        @Test
        @DisplayName("uses the fetch-joined query so the listing does not trigger N+1 selects")
        void usesFetchJoinedQuery() {
            when(masterResumeRepository.findByUserIdWithSections(callerId))
                    .thenReturn(List.of(masterResumeOwnedBy(callerId)));

            List<?> result = service.getMasterResumes(callerId);

            assertEquals(1, result.size());
            verify(masterResumeRepository).findByUserIdWithSections(callerId);
            // The lazy variant must not be used on this path — that is the N+1 regression.
            verify(masterResumeRepository, never()).findByUserId(any());
        }

        @Test
        @DisplayName("refuses to list another user's resumes and issues no query")
        void refusesForeignOwner() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.getMasterResumes(otherUserId));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            verifyNoInteractions(masterResumeRepository);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getMasterResumeWithSections")
    class GetMasterResumeWithSections {

        @Test
        @DisplayName("returns a resume the caller owns")
        void returnsOwnResume() {
            MasterResume resume = masterResumeOwnedBy(callerId);
            when(masterResumeRepository.findByIdWithSections(resume.getId()))
                    .thenReturn(Optional.of(resume));

            assertNotNull(service.getMasterResumeWithSections(resume.getId()));
        }

        @Test
        @DisplayName("refuses a resume owned by somebody else")
        void refusesForeignResume() {
            MasterResume foreign = masterResumeOwnedBy(otherUserId);
            when(masterResumeRepository.findByIdWithSections(foreign.getId()))
                    .thenReturn(Optional.of(foreign));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.getMasterResumeWithSections(foreign.getId()));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("triggerTailoring")
    class TriggerTailoring {

        private MasterResume resume;

        private void stubHappyPath() {
            resume = masterResumeOwnedBy(callerId);
            when(masterResumeRepository.findByIdWithSections(resume.getId()))
                    .thenReturn(Optional.of(resume));
            when(jobDescriptionRepository.save(any(JobDescription.class)))
                    .thenAnswer(inv -> {
                        JobDescription jd = inv.getArgument(0);
                        jd.setId(UUID.randomUUID());
                        return jd;
                    });
            when(tailoredResumeRepository.save(any(TailoredResume.class)))
                    .thenAnswer(inv -> {
                        TailoredResume tr = inv.getArgument(0);
                        tr.setId(UUID.randomUUID());
                        return tr;
                    });
        }

        @Test
        @DisplayName("persists a PENDING record before publishing")
        void persistsPendingRecord() {
            stubHappyPath();

            service.triggerTailoring(resume.getId(), tailorRequest());

            ArgumentCaptor<TailoredResume> captor = ArgumentCaptor.forClass(TailoredResume.class);
            verify(tailoredResumeRepository).save(captor.capture());
            assertEquals(TailoredResume.TailoringStatus.PENDING, captor.getValue().getStatus(),
                    "the record must be PENDING when the API returns 202");
        }

        @Test
        @DisplayName("publishes an event carrying the ids and content the worker needs")
        void publishesEventWithCorrectPayload() {
            stubHappyPath();

            service.triggerTailoring(resume.getId(), tailorRequest());

            verify(tailoringProducer).publish(eventCaptor.capture());
            TailoringRequestedEvent event = eventCaptor.getValue();

            assertEquals(resume.getId(), event.getMasterResumeId());
            assertEquals(callerId, event.getUserId());
            assertNotNull(event.getTailoredResumeId());
            assertNotNull(event.getJobDescriptionId());
            assertTrue(event.getMasterResumeContent().contains("Built an event-driven backend"),
                    "assembled content must carry the master resume's section text");
        }

        @Test
        @DisplayName("attributes the job description to the resume owner")
        void jobDescriptionInheritsOwner() {
            stubHappyPath();

            service.triggerTailoring(resume.getId(), tailorRequest());

            ArgumentCaptor<JobDescription> captor = ArgumentCaptor.forClass(JobDescription.class);
            verify(jobDescriptionRepository).save(captor.capture());
            assertEquals(callerId, captor.getValue().getUserId());
            assertEquals("Acme", captor.getValue().getCompanyName());
        }

        @Test
        @DisplayName("increments the tailoring request counter")
        void incrementsCounter() {
            stubHappyPath();

            service.triggerTailoring(resume.getId(), tailorRequest());

            assertEquals(1.0,
                    meterRegistry.get("resumeforge.tailoring.requests").counter().count());
        }

        @Test
        @DisplayName("refuses to tailor another user's resume and publishes nothing")
        void refusesForeignResume() {
            MasterResume foreign = masterResumeOwnedBy(otherUserId);
            when(masterResumeRepository.findByIdWithSections(foreign.getId()))
                    .thenReturn(Optional.of(foreign));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.triggerTailoring(foreign.getId(), tailorRequest()));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            // No side effects may escape a failed authorisation check.
            verify(jobDescriptionRepository, never()).save(any());
            verify(tailoredResumeRepository, never()).save(any());
            verifyNoInteractions(tailoringProducer);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("retryTailoring")
    class RetryTailoring {

        private TailoredResume tailoredIn(TailoredResume.TailoringStatus status) {
            MasterResume master = masterResumeOwnedBy(callerId);
            JobDescription jd = new JobDescription();
            jd.setId(UUID.randomUUID());
            jd.setUserId(callerId);
            jd.setDescription("Java, Kafka");

            TailoredResume tr = new TailoredResume();
            tr.setId(UUID.randomUUID());
            tr.setMasterResume(master);
            tr.setJobDescription(jd);
            tr.setStatus(status);
            return tr;
        }

        @Test
        @DisplayName("resets a FAILED job to PENDING and republishes it")
        void retriesFailedJob() {
            TailoredResume failed = tailoredIn(TailoredResume.TailoringStatus.FAILED);
            when(tailoredResumeRepository.findById(failed.getId())).thenReturn(Optional.of(failed));
            when(masterResumeRepository.findByIdWithSections(failed.getMasterResume().getId()))
                    .thenReturn(Optional.of(failed.getMasterResume()));

            service.retryTailoring(failed.getId());

            assertEquals(TailoredResume.TailoringStatus.PENDING, failed.getStatus());
            verify(tailoredResumeRepository).save(failed);
            verify(tailoringProducer).publish(any(TailoringRequestedEvent.class));
        }

        @Test
        @DisplayName("refuses to retry a COMPLETED job so a result cannot be overwritten")
        void refusesCompletedJob() {
            TailoredResume completed = tailoredIn(TailoredResume.TailoringStatus.COMPLETED);
            when(tailoredResumeRepository.findById(completed.getId())).thenReturn(Optional.of(completed));

            assertThrows(IllegalStateException.class, () -> service.retryTailoring(completed.getId()));

            assertEquals(TailoredResume.TailoringStatus.COMPLETED, completed.getStatus());
            verify(tailoredResumeRepository, never()).save(any());
            verifyNoInteractions(tailoringProducer);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("updateTailoredSections")
    class UpdateTailoredSections {

        @Test
        @DisplayName("rejects section content beyond the length limit before loading anything")
        void rejectsOversizedSection() {
            String tooLong = "x".repeat(10_001);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.updateTailoredSections(UUID.randomUUID(), Map.of("summary", tooLong)));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            verifyNoInteractions(tailoredResumeRepository);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("upsertMasterResume")
    class UpsertMasterResume {

        @Test
        @DisplayName("creates a new resume when the user has none")
        void createsWhenAbsent() {
            when(masterResumeRepository.findByUserId(callerId)).thenReturn(List.of());
            when(masterResumeRepository.save(any(MasterResume.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.upsertMasterResume(callerId, "Fresh content");

            verify(masterResumeRepository).save(masterResumeCaptor.capture());
            assertEquals(callerId, masterResumeCaptor.getValue().getUserId());
        }

        @Test
        @DisplayName("refuses to upsert on behalf of another user")
        void refusesForeignOwner() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.upsertMasterResume(otherUserId, "content"));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            verifyNoInteractions(masterResumeRepository);
        }
    }
}
