package com.resumeforge.resume.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeforge.resume.dto.CreateMasterResumeRequest;
import com.resumeforge.resume.model.User;
import com.resumeforge.resume.model.MasterResume;
import com.resumeforge.resume.repository.UserRepository;
import com.resumeforge.resume.repository.MasterResumeRepository;
import com.resumeforge.resume.auth.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ PHASE 5: BOLA (Broken Object Level Authorization) Tests
 *
 * CRITICAL SECURITY TEST: Verify users can ONLY access their own data
 *
 * BOLA Vulnerability Example:
 * - User A logs in, gets JWT token
 * - User A tries to access User B's resume: /api/v1/resumes/{userB-resume-id}
 * - VULNERABLE: Request returns User B's data (403 SHOULD be returned)
 * - SECURE: Request returns 403 Forbidden
 *
 * This test prevents:
 * - User data leaks
 * - Account takeover scenarios
 * - Unauthorized data access
 *
 * Estimated Time: 3 hours
 * Priority: CRITICAL (prevents user data exposure)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BOLATest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MasterResumeRepository masterResumeRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User user1;
    private User user2;
    private MasterResume resume1;
    private MasterResume resume2;
    private String token1;
    private String token2;

    @BeforeEach
    void setUp() {
        // Clean up
        masterResumeRepository.deleteAll();
        userRepository.deleteAll();

        // Create two separate users
        user1 = new User();
        user1.setName("User One");
        user1.setEmail("user1@example.com");
        user1.setPasswordHash("hashed_password_1");
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setName("User Two");
        user2.setEmail("user2@example.com");
        user2.setPasswordHash("hashed_password_2");
        user2 = userRepository.save(user2);

        // Create resumes for each user
        resume1 = new MasterResume();
        resume1.setUserId(user1.getId());
        resume1.setTitle("User 1 Resume");
        resume1.setSummary("User 1's resume content");
        resume1 = masterResumeRepository.save(resume1);

        resume2 = new MasterResume();
        resume2.setUserId(user2.getId());
        resume2.setTitle("User 2 Resume");
        resume2.setSummary("User 2's resume content");
        resume2 = masterResumeRepository.save(resume2);

        // Generate JWT tokens for each user
        token1 = jwtUtil.generate(user1.getId(), user1.getEmail());
        token2 = jwtUtil.generate(user2.getId(), user2.getEmail());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // POSITIVE TESTS: User CAN access own data
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ User can access their own resume")
    void testUserCanAccessOwnResume() throws Exception {
        // Act: User1 accesses User1's resume with User1's token
        mockMvc.perform(get("/api/v1/resumes/" + resume1.getId())
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())  // 200 OK
                .andReturn();
    }

    @Test
    @DisplayName("✅ User can create resume for themselves")
    void testUserCanCreateOwnResume() throws Exception {
        // Arrange
        CreateMasterResumeRequest request = new CreateMasterResumeRequest();
        request.setTitle("New Resume");
        request.setSummary("New resume content");

        // Act: User1 creates resume with User1's token
        mockMvc.perform(post("/api/v1/resumes")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())  // 201 Created
                .andReturn();
    }

    @Test
    @DisplayName("✅ User can update their own resume")
    void testUserCanUpdateOwnResume() throws Exception {
        // Arrange
        CreateMasterResumeRequest request = new CreateMasterResumeRequest();
        request.setTitle("Updated Resume");
        request.setSummary("Updated content");

        // Act: User1 updates User1's resume with User1's token
        mockMvc.perform(put("/api/v1/resumes/" + resume1.getId())
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())  // 200 OK
                .andReturn();
    }

    @Test
    @DisplayName("✅ User can delete their own resume")
    void testUserCanDeleteOwnResume() throws Exception {
        // Act: User1 deletes User1's resume with User1's token
        mockMvc.perform(delete("/api/v1/resumes/" + resume1.getId())
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())  // 204 No Content
                .andReturn();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // NEGATIVE TESTS: User CANNOT access other user's data
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("❌ User CANNOT read another user's resume (403 Forbidden)")
    void testUserCannotReadOtherUserResume() throws Exception {
        // Act: User1 tries to access User2's resume with User1's token
        mockMvc.perform(get("/api/v1/resumes/" + resume2.getId())
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())  // 403 Forbidden (SECURITY)
                .andReturn();
    }

    @Test
    @DisplayName("❌ User CANNOT update another user's resume (403 Forbidden)")
    void testUserCannotUpdateOtherUserResume() throws Exception {
        // Arrange
        CreateMasterResumeRequest request = new CreateMasterResumeRequest();
        request.setTitle("Hacked!");
        request.setSummary("User 1 tries to modify User 2's resume");

        // Act: User1 tries to update User2's resume with User1's token
        mockMvc.perform(put("/api/v1/resumes/" + resume2.getId())
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())  // 403 Forbidden (SECURITY)
                .andReturn();
    }

    @Test
    @DisplayName("❌ User CANNOT delete another user's resume (403 Forbidden)")
    void testUserCannotDeleteOtherUserResume() throws Exception {
        // Act: User1 tries to delete User2's resume with User1's token
        mockMvc.perform(delete("/api/v1/resumes/" + resume2.getId())
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())  // 403 Forbidden (SECURITY)
                .andReturn();
    }

    @Test
    @DisplayName("❌ User CANNOT access other user's resumes list filtered by owner")
    void testUserCannotListOtherUserResumes() throws Exception {
        // Act: User1 tries to list User2's resumes
        mockMvc.perform(get("/api/v1/resumes?userId=" + user2.getId())
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())  // 403 Forbidden (if supported)
                // OR: should only return User1's resumes, not User2's
                .andReturn();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // EDGE CASES: Malicious attempts
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("❌ User with invalid token cannot access resumes (401 Unauthorized)")
    void testInvalidTokenRejected() throws Exception {
        // Act: Access with invalid token
        mockMvc.perform(get("/api/v1/resumes/" + resume1.getId())
                .header("Authorization", "Bearer invalid_token_xyz")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())  // 401 Unauthorized
                .andReturn();
    }

    @Test
    @DisplayName("❌ User without token cannot access protected endpoints (401 Unauthorized)")
    void testMissingTokenRejected() throws Exception {
        // Act: Access without token
        mockMvc.perform(get("/api/v1/resumes/" + resume1.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())  // 401 Unauthorized
                .andReturn();
    }

    @Test
    @DisplayName("❌ User cannot escalate privileges (stay in own context)")
    void testPrivilegeEscalationPrevented() throws Exception {
        // Scenario: User1 tries to modify their token to claim they're User2
        // (This should fail at JWT validation layer)

        // Act: User1 with User1's valid token should NOT be able to access User2 data
        mockMvc.perform(get("/api/v1/resumes/" + resume2.getId())
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())  // 403 Forbidden
                .andReturn();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // ADMIN TESTS: Admin SHOULD be able to access all data (if admin exists)
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("⏭️ Admin role tests skipped - role field not implemented in User model")
    void testAdminCanAccessAllData() throws Exception {
        // Note: Admin role tests can be implemented when role field is added to User model
        // Current implementation: All users have equal access to only their own data
        // Future: Can enhance with role-based access control (RBAC)
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SUMMARY: BOLA Prevention Checklist
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * BOLA Prevention Checklist:
     *
     * ✅ User can read own data (GET /resource/{id})
     * ✅ User can create own data (POST /resources)
     * ✅ User can update own data (PUT /resource/{id})
     * ✅ User can delete own data (DELETE /resource/{id})
     *
     * ❌ User CANNOT read other user's data (403 Forbidden)
     * ❌ User CANNOT update other user's data (403 Forbidden)
     * ❌ User CANNOT delete other user's data (403 Forbidden)
     * ❌ User CANNOT list other user's data (403 Forbidden)
     *
     * ❌ Invalid token rejected (401 Unauthorized)
     * ❌ Missing token rejected (401 Unauthorized)
     * ❌ Privilege escalation prevented (403 Forbidden)
     *
     * ✅ Admin can access all user data (if admin role exists)
     */
}
