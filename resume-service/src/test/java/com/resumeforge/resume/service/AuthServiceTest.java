package com.resumeforge.resume.service;

import com.resumeforge.resume.auth.JwtUtil;
import com.resumeforge.resume.dto.AuthResponse;
import com.resumeforge.resume.dto.LoginRequest;
import com.resumeforge.resume.dto.RegisterRequest;
import com.resumeforge.resume.model.User;
import com.resumeforge.resume.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ✅ PHASE 5: AuthService Unit Tests
 *
 * Tests authentication flows:
 * - User registration
 * - User login
 * - JWT token generation
 * - Password hashing
 * - Email validation
 *
 * Estimated Time: 3 hours
 * Priority: CRITICAL (foundation of all security)
 */
@DataJpaTest
@Import({AuthService.class, JwtUtil.class})
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private PasswordEncoder passwordEncoder;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "SecurePassword123!";
    private static final String TEST_NAME = "Test User";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userRepository.deleteAll();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // REGISTRATION TESTS
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ User registration succeeds with valid input")
    void testRegistrationSucceeds() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setName(TEST_NAME);
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals(TEST_EMAIL, response.getEmail());
        assertEquals(TEST_NAME, response.getName());
        assertTrue(userRepository.existsByEmail(TEST_EMAIL));
    }

    @Test
    @DisplayName("❌ Registration fails with duplicate email (409 CONFLICT)")
    void testRegistrationFailsWithDuplicateEmail() {
        // Arrange: First registration
        RegisterRequest request1 = new RegisterRequest();
        request1.setName(TEST_NAME);
        request1.setEmail(TEST_EMAIL);
        request1.setPassword(TEST_PASSWORD);
        authService.register(request1);

        // Act & Assert: Second registration with same email fails
        RegisterRequest request2 = new RegisterRequest();
        request2.setName("Another User");
        request2.setEmail(TEST_EMAIL);  // Same email
        request2.setPassword(TEST_PASSWORD);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(request2));
        assertEquals(409, exception.getStatusCode().value());  // CONFLICT
    }

    @Test
    @DisplayName("✅ Email is normalized (trimmed and lowercased)")
    void testEmailNormalization() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setName(TEST_NAME);
        request.setEmail("  TEST@EXAMPLE.COM  ");  // With spaces and uppercase
        request.setPassword(TEST_PASSWORD);

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    @DisplayName("✅ Password is hashed (not stored in plaintext)")
    void testPasswordIsHashed() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setName(TEST_NAME);
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);

        // Act
        authService.register(request);

        // Assert: Verify password is hashed
        User savedUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        assertNotNull(savedUser.getPasswordHash());
        assertNotEquals(TEST_PASSWORD, savedUser.getPasswordHash());  // Not plaintext
        assertTrue(passwordEncoder.matches(TEST_PASSWORD, savedUser.getPasswordHash()));  // But matches
    }

    // ════════════════════════════════════════════════════════════════════════════
    // LOGIN TESTS
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Login succeeds with correct credentials")
    void testLoginSucceeds() {
        // Arrange: Register user first
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName(TEST_NAME);
        registerRequest.setEmail(TEST_EMAIL);
        registerRequest.setPassword(TEST_PASSWORD);
        authService.register(registerRequest);

        // Act: Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(TEST_EMAIL);
        loginRequest.setPassword(TEST_PASSWORD);
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals(TEST_EMAIL, response.getEmail());
    }

    @Test
    @DisplayName("❌ Login fails with wrong password (401 UNAUTHORIZED)")
    void testLoginFailsWithWrongPassword() {
        // Arrange: Register user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName(TEST_NAME);
        registerRequest.setEmail(TEST_EMAIL);
        registerRequest.setPassword(TEST_PASSWORD);
        authService.register(registerRequest);

        // Act & Assert: Login with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(TEST_EMAIL);
        loginRequest.setPassword("WrongPassword123!");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(loginRequest));
        assertEquals(401, exception.getStatusCode().value());  // UNAUTHORIZED
    }

    @Test
    @DisplayName("❌ Login fails with non-existent email (401 UNAUTHORIZED)")
    void testLoginFailsWithNonExistentEmail() {
        // Act & Assert
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword(TEST_PASSWORD);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(loginRequest));
        assertEquals(401, exception.getStatusCode().value());  // UNAUTHORIZED
    }

    @Test
    @DisplayName("❌ Login fails with null password (401 UNAUTHORIZED)")
    void testLoginFailsWithNullPassword() {
        // Arrange: Create user with no password (edge case)
        User user = new User();
        user.setName(TEST_NAME);
        user.setEmail(TEST_EMAIL);
        user.setPasswordHash(null);  // No password set
        userRepository.save(user);

        // Act & Assert
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(TEST_EMAIL);
        loginRequest.setPassword(TEST_PASSWORD);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(loginRequest));
        assertEquals(401, exception.getStatusCode().value());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // JWT TOKEN TESTS
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ JWT token is generated on successful registration")
    void testJwtTokenGeneratedOnRegistration() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setName(TEST_NAME);
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);

        // Act
        AuthResponse response = authService.register(request);

        // Assert: Token is not empty and contains three parts (header.payload.signature)
        String token = response.getToken();
        assertNotNull(token);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);  // JWT has 3 parts
    }

    @Test
    @DisplayName("✅ JWT token is valid and contains correct claims")
    void testJwtTokenContainsCorrectClaims() {
        // Arrange: Register and login
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName(TEST_NAME);
        registerRequest.setEmail(TEST_EMAIL);
        registerRequest.setPassword(TEST_PASSWORD);
        AuthResponse registerResponse = authService.register(registerRequest);

        // Act: Verify token validity and extract claims
        String token = registerResponse.getToken();
        assertTrue(jwtUtil.isValid(token));

        // Assert: Token contains user ID and email
        var userId = jwtUtil.extractUserId(token);
        var email = (String) jwtUtil.parse(token).get("email");
        assertEquals(registerResponse.getUserId(), userId);
        assertEquals(TEST_EMAIL, email);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // USER PROFILE TESTS
    // ════════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("✅ Get user profile (/me) succeeds with valid user ID")
    void testGetUserProfileSucceeds() {
        // Arrange: Register user
        RegisterRequest request = new RegisterRequest();
        request.setName(TEST_NAME);
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);
        AuthResponse registerResponse = authService.register(request);
        var userId = registerResponse.getUserId();

        // Act
        AuthResponse response = authService.me(userId);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(TEST_EMAIL, response.getEmail());
        assertEquals(TEST_NAME, response.getName());
    }

    @Test
    @DisplayName("❌ Get user profile fails with non-existent user (404 NOT_FOUND)")
    void testGetUserProfileFailsWithNonExistentUser() {
        // Act & Assert
        java.util.UUID nonExistentId = java.util.UUID.randomUUID();
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.me(nonExistentId));
        assertEquals(404, exception.getStatusCode().value());  // NOT_FOUND
    }

    @Test
    @DisplayName("✅ Default plan is FREE for new users")
    void testDefaultPlanIsFree() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setName(TEST_NAME);
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertEquals("FREE", response.getPlan());
    }
}
