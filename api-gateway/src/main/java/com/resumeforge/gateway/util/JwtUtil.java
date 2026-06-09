package com.resumeforge.gateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${auth.jwt.secret}")
    private String jwtSecret;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates JWT token signature and expiration.
     * Returns true only if token is valid and not expired.
     *
     * @param token JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean isValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("[JWT] Token expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.debug("[JWT] Unsupported token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.debug("[JWT] Malformed token: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.debug("[JWT] Invalid signature: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("[JWT] Invalid token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts user ID (subject claim) from token.
     * MUST call isValid() first to ensure token is valid.
     *
     * @param token JWT token
     * @return User ID as UUID
     * @throws JwtException if token is invalid
     */
    public UUID extractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extracts email claim from token.
     * MUST call isValid() first to ensure token is valid.
     *
     * @param token JWT token
     * @return Email address
     * @throws JwtException if token is invalid
     */
    public String extractEmail(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("email", String.class);
    }
}
