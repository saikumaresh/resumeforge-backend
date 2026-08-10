package com.resumeforge.gateway.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the token check the gateway applies before forwarding a request.
 *
 * The gateway trusts nothing about a caller except what a signed token says,
 * so the value of this class is entirely in what it refuses. Each test below
 * describes one way a caller could try to present a token they should not be
 * able to produce.
 */
class JwtUtilTest {

    /** HS256 needs at least 32 bytes of key material. */
    private static final String SECRET = "gateway-test-secret-at-least-32-bytes-long";
    private static final String OTHER_SECRET = "a-completely-different-secret-also-32-bytes";

    private JwtUtil jwtUtil;

    private static SecretKey keyFor(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** A well-formed token for the given subject, signed with the given secret. */
    private static String token(String secret, UUID subject, String email, long millisFromNow) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject.toString())
                .claim("email", email)
                .issuedAt(new Date(now - 1000))
                .expiration(new Date(now + millisFromNow))
                .signWith(keyFor(secret))
                .compact();
    }

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", SECRET);
    }

    // ────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("tokens that should be accepted")
    class Accepted {

        @Test
        @DisplayName("a token signed with the configured secret is valid")
        void validToken() {
            assertTrue(jwtUtil.isValid(token(SECRET, UUID.randomUUID(), "user@example.com", 60_000)));
        }

        @Test
        @DisplayName("the subject is read back as the user id")
        void extractsUserId() {
            UUID id = UUID.randomUUID();
            assertEquals(id, jwtUtil.extractUserId(token(SECRET, id, "user@example.com", 60_000)));
        }

        @Test
        @DisplayName("the email claim is read back")
        void extractsEmail() {
            String t = token(SECRET, UUID.randomUUID(), "person@example.com", 60_000);
            assertEquals("person@example.com", jwtUtil.extractEmail(t));
        }
    }

    // ────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("tokens that must be refused")
    class Refused {

        @Test
        @DisplayName("a token signed with a different secret is rejected")
        void wrongSignature() {
            String forged = token(OTHER_SECRET, UUID.randomUUID(), "attacker@example.com", 60_000);
            assertFalse(jwtUtil.isValid(forged),
                    "a token this gateway did not sign must not be accepted");
        }

        @Test
        @DisplayName("an expired token is rejected")
        void expired() {
            String stale = token(SECRET, UUID.randomUUID(), "user@example.com", -60_000);
            assertFalse(jwtUtil.isValid(stale));
        }

        @Test
        @DisplayName("an unsigned token is rejected")
        void unsignedToken() {
            String unsigned = Jwts.builder()
                    .subject(UUID.randomUUID().toString())
                    .claim("email", "user@example.com")
                    .compact();
            assertFalse(jwtUtil.isValid(unsigned),
                    "dropping the signature must not be a way past the check");
        }

        @Test
        @DisplayName("a malformed token is rejected rather than throwing")
        void malformed() {
            assertFalse(jwtUtil.isValid("not-a-token"));
            assertFalse(jwtUtil.isValid("a.b.c"));
        }

        @Test
        @DisplayName("null and empty input are rejected rather than throwing")
        void nullAndEmpty() {
            assertFalse(jwtUtil.isValid(null));
            assertFalse(jwtUtil.isValid(""));
        }

        @Test
        @DisplayName("reading claims from a forged token throws instead of returning them")
        void extractionFromForgedTokenThrows() {
            String forged = token(OTHER_SECRET, UUID.randomUUID(), "attacker@example.com", 60_000);
            assertThrows(JwtException.class, () -> jwtUtil.extractUserId(forged));
            assertThrows(JwtException.class, () -> jwtUtil.extractEmail(forged));
        }
    }
}
