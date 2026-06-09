package com.resumeforge.resume;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * ✅ PHASE 5: Base class for integration tests.
 *
 * Provides:
 * - Test database (H2, in-memory)
 * - MockMvc for API testing
 * - EntityManager for direct DB access
 * - @Transactional rollback after each test (clean state)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected EntityManager entityManager;

    /**
     * Clears persistence context after each test
     * to ensure fresh state for next test.
     */
    protected void clearEntityManager() {
        entityManager.flush();
        entityManager.clear();
    }
}
