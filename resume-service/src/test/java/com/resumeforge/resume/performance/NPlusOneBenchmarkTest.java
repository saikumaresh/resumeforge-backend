package com.resumeforge.resume.performance;

import com.resumeforge.resume.model.MasterResume;
import com.resumeforge.resume.model.MasterResumeSection;
import com.resumeforge.resume.repository.MasterResumeRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Measures the cost of the N+1 select problem on the master-resume listing path,
 * comparing the naive derived query against the LEFT JOIN FETCH variant.
 *
 * The numbers this prints are the ones quoted in the project report's
 * "Feature Development Process" chapter. Re-run with:
 *
 *   mvn test -Dtest=NPlusOneBenchmarkTest
 *
 * Absolute timings depend on the machine and on H2 running in memory; the
 * query-count reduction is deterministic and is the meaningful result.
 */
@DataJpaTest
@ActiveProfiles("test")
class NPlusOneBenchmarkTest {

    /** Master resumes owned by the benchmark user. */
    private static final int RESUMES = 30;
    /** Sections per resume — the collection that triggers the extra selects. */
    private static final int SECTIONS_PER_RESUME = 6;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int MEASURED_ITERATIONS = 20;

    @Autowired
    private MasterResumeRepository repository;

    @Autowired
    private EntityManager entityManager;

    private UUID userId;
    private Statistics statistics;

    @BeforeEach
    void seed() {
        userId = UUID.randomUUID();

        for (int i = 0; i < RESUMES; i++) {
            MasterResume resume = new MasterResume();
            resume.setUserId(userId);
            resume.setTitle("Benchmark resume " + i);
            resume.setSummary("Seeded for the N+1 measurement");

            List<MasterResumeSection> sections = new ArrayList<>();
            for (int s = 0; s < SECTIONS_PER_RESUME; s++) {
                MasterResumeSection section = new MasterResumeSection();
                section.setMasterResume(resume);
                section.setSectionType(MasterResumeSection.SectionType.EXPERIENCE);
                section.setContent("Section " + s + " of resume " + i);
                section.setPosition(s);
                sections.add(section);
            }
            resume.setSections(sections);
            repository.save(resume);
        }
        entityManager.flush();

        statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    /** Forces the lazy collection to initialise, which is what a mapper would do. */
    private int touchSections(List<MasterResume> resumes) {
        int count = 0;
        for (MasterResume r : resumes) {
            count += r.getSections().size();
        }
        return count;
    }

    private long queriesFor(Runnable work) {
        entityManager.clear();
        statistics.clear();
        work.run();
        return statistics.getPrepareStatementCount();
    }

    private double medianMillis(Runnable work) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            entityManager.clear();
            work.run();
        }
        List<Long> samples = new ArrayList<>();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            entityManager.clear();
            long start = System.nanoTime();
            work.run();
            samples.add(System.nanoTime() - start);
        }
        samples.sort(Long::compareTo);
        return samples.get(samples.size() / 2) / 1_000_000.0;
    }

    @Test
    @DisplayName("JOIN FETCH collapses the N+1 selects on the master-resume listing")
    void measureNPlusOne() {
        Runnable naive = () -> touchSections(repository.findByUserId(userId));
        Runnable fetched = () -> touchSections(repository.findByUserIdWithSections(userId));

        long naiveQueries = queriesFor(naive);
        long fetchedQueries = queriesFor(fetched);

        double naiveMs = medianMillis(naive);
        double fetchedMs = medianMillis(fetched);

        System.out.println();
        System.out.println("=== N+1 BENCHMARK ===================================================");
        System.out.printf ("dataset                     : %d resumes x %d sections%n",
                RESUMES, SECTIONS_PER_RESUME);
        System.out.printf ("iterations                  : %d warmup, %d measured (median)%n",
                WARMUP_ITERATIONS, MEASURED_ITERATIONS);
        System.out.println("---------------------------------------------------------------------");
        System.out.printf ("findByUserId (lazy)         : %3d queries   %7.3f ms%n", naiveQueries, naiveMs);
        System.out.printf ("findByUserIdWithSections    : %3d queries   %7.3f ms%n", fetchedQueries, fetchedMs);
        System.out.println("---------------------------------------------------------------------");
        System.out.printf ("query reduction             : %d -> %d  (%.1fx fewer)%n",
                naiveQueries, fetchedQueries, naiveQueries / (double) fetchedQueries);
        System.out.printf ("latency reduction           : %.3f ms -> %.3f ms  (%.1f%% faster)%n",
                naiveMs, fetchedMs, (1 - fetchedMs / naiveMs) * 100);
        System.out.println("=====================================================================");
        System.out.println();

        // The naive path issues one select for the parents plus one per resume.
        assertTrue(naiveQueries > fetchedQueries,
                "JOIN FETCH should issue strictly fewer statements than the lazy path");
        assertTrue(fetchedQueries <= 2,
                "JOIN FETCH should collapse the listing into a single round trip");
    }
}
