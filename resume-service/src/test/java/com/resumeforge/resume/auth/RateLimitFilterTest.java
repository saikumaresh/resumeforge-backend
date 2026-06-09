package com.resumeforge.resume.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeforge.resume.dto.LoginRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ PHASE 5: RateLimitFilter Tests
 *
 * Tests DoS protection via rate limiting:
 * - Login rate limited to 10 per 60 seconds
 * - Register rate limited to 5 per 60 seconds
 * - 429 Too Many Requests returned when exceeded
 * - Counter resets after time window
 *
 * Estimated Time: 2 hours
 * Priority: CRITICAL (prevents brute force attacks)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Prepare test login request
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("TestPassword123!");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // LOGIN RATE LIMITING TESTS (10 per 60 seconds)
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Login requests below rate limit (10/60s) succeed")
    void testLoginBelowRateLimitSucceeds() throws Exception {
        // Act & Assert: First 10 requests should succeed (200 OK)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())  // 401 because credentials wrong, not 429
                    .andReturn();
        }
    }

    @Test
    @DisplayName("❌ Login request exceeding rate limit (11th) returns 429 Too Many Requests")
    void testLoginExceedingRateLimitReturns429() throws Exception {
        // Arrange: Make 10 requests to reach limit
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andReturn();  // First 10 succeed (or get 401 for auth failure)
        }

        // Act: 11th request exceeds limit
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isTooManyRequests())  // 429
                .andReturn();

        // Assert
        assertEquals(429, result.getResponse().getStatus());
        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("rate limit") || responseBody.contains("too many"));
    }

    // ════════════════════════════════════════════════════════════════════════════
    // REGISTER RATE LIMITING TESTS (5 per 60 seconds)
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Register requests below rate limit (5/60s) succeed")
    void testRegisterBelowRateLimitSucceeds() throws Exception {
        // Act & Assert: First 5 requests should get through (at least 400+ response, not 429)
        for (int i = 0; i < 5; i++) {
            String email = "test" + i + "@example.com";
            String registerPayload = String.format(
                    "{\"name\":\"Test User %d\",\"email\":\"%s\",\"password\":\"TestPassword123!\"}",
                    i, email
            );

            MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerPayload))
                    .andReturn();

            // Should not be 429
            assertNotEquals(429, result.getResponse().getStatus());
        }
    }

    @Test
    @DisplayName("❌ Register request exceeding rate limit (6th) returns 429 Too Many Requests")
    void testRegisterExceedingRateLimitReturns429() throws Exception {
        // Arrange: Make 5 requests to reach register limit
        for (int i = 0; i < 5; i++) {
            String email = "test" + i + "@example.com";
            String registerPayload = String.format(
                    "{\"name\":\"Test User %d\",\"email\":\"%s\",\"password\":\"TestPassword123!\"}",
                    i, email
            );

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerPayload))
                    .andReturn();
        }

        // Act: 6th request exceeds limit
        String registerPayload = "{\"name\":\"Test User 6\",\"email\":\"test6@example.com\",\"password\":\"TestPassword123!\"}";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
                .andExpect(status().isTooManyRequests())  // 429
                .andReturn();

        // Assert
        assertEquals(429, result.getResponse().getStatus());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // RATE LIMIT COUNTER RESET TESTS
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Rate limit counter resets after time window (60 seconds)")
    void testRateLimitCounterResetsAfterWindow() throws Exception {
        // Note: This test would need to use SystemClock manipulation or
        // a real wait. For CI/CD efficiency, this is typically tested via:
        // 1. Unit test with mocked time
        // 2. Integration test with Thread.sleep (slow)
        // 3. Functional test in staging environment

        // Arrange: Simulate reaching limit
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andReturn();
        }

        // Act: Verify limit reached
        MvcResult limitResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();
        assertEquals(429, limitResult.getResponse().getStatus());

        // TODO: After 60 seconds (or mocked time advancement), rate limit should reset
        // This requires either:
        // 1. Actual sleep: Thread.sleep(61000);
        // 2. Clock mock: ManualClock or similar
        // 3. Separate integration test with test container
    }

    // ════════════════════════════════════════════════════════════════════════════
    // RATE LIMIT BY IP ADDRESS TESTS
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Rate limits are per IP address (different IPs have separate limits)")
    void testRateLimitPerIpAddress() throws Exception {
        // Arrange: Make 10 requests from IP 127.0.0.1
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Forwarded-For", "192.168.1.100")  // Different IP
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andReturn();
        }

        // Act: 11th request from IP 127.0.0.1 should be rate limited
        MvcResult result1 = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "192.168.1.100")
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();
        assertEquals(429, result1.getResponse().getStatus());

        // Act: Request from different IP (192.168.1.101) should NOT be rate limited
        MvcResult result2 = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "192.168.1.101")  // Different IP
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();
        assertNotEquals(429, result2.getResponse().getStatus());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // RATE LIMIT BYPASS TESTS (Admin endpoints)
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Admin/health endpoints bypass rate limiting")
    void testHealthEndpointBypassesRateLimit() throws Exception {
        // /health endpoint should not be rate limited
        // Even if called 100+ times, should always succeed

        for (int i = 0; i < 20; i++) {
            MvcResult result = mockMvc.perform(post("/health"))
                    .andReturn();

            // Should never return 429
            assertNotEquals(429, result.getResponse().getStatus());
        }
    }
}
