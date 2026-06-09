package com.resumeforge.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ✅ PHASE 1.7 FIX: Configuration for JWT-protected routes.
 *
 * Allows defining unprotected routes via application.yml instead of hardcoding.
 * Example:
 *
 * jwt:
 *   unprotected-routes:
 *     - /actuator
 *     - /api/v1/auth/login
 *     - /api/v1/auth/register
 *     - /health
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtGatewayConfig {

    private List<String> unprotectedRoutes = new ArrayList<>(List.of(
        "/actuator",
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/auth/health",
        "/health",
        "/metrics",
        "/prometheus"
    ));

    public List<String> getUnprotectedRoutes() {
        return unprotectedRoutes;
    }

    public void setUnprotectedRoutes(List<String> unprotectedRoutes) {
        this.unprotectedRoutes = unprotectedRoutes;
    }

    /**
     * Check if a path is unprotected (doesn't require JWT).
     *
     * @param path Request path
     * @return true if path is in the unprotected list
     */
    public boolean isUnprotected(String path) {
        return unprotectedRoutes.stream()
                .anyMatch(path::startsWith);
    }
}
