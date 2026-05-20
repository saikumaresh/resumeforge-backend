package com.resumeforge.resume.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window rate limiter for sensitive endpoints.
 *
 * Uses per-IP request counts within a configurable time window.
 * Stored in memory — resets on restart. Suitable for single-node;
 * for multi-node production, replace backing store with Redis.
 *
 * Default limits (configurable via RuleConfig):
 *   /api/v1/auth/login    → 10 requests / 60 s
 *   /api/v1/auth/register → 5 requests / 60 s
 *   /api/v1/auth/google   → 10 requests / 60 s
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private record RuleConfig(String pathPrefix, int maxRequests, long windowMillis) {}

    private static final List<RuleConfig> RULES = List.of(
        new RuleConfig("/api/v1/auth/login",    10,  60_000),
        new RuleConfig("/api/v1/auth/register",  5,  60_000),
        new RuleConfig("/api/v1/auth/google",   10,  60_000)
    );

    // Map<ruleKey+ip, Deque<requestTimestampMs>>
    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI();
        String ip   = resolveClientIp(req);

        for (RuleConfig rule : RULES) {
            if (path.startsWith(rule.pathPrefix())) {
                if (!isAllowed(rule, ip)) {
                    log.warn("[RATE-LIMIT] Blocked ip={} path={}", ip, path);
                    res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    res.setContentType("application/json");
                    res.getWriter().write(
                        "{\"error\":\"Too many requests. Please wait before trying again.\"}");
                    return;
                }
                break; // only apply the first matching rule
            }
        }

        chain.doFilter(req, res);
    }

    private boolean isAllowed(RuleConfig rule, String ip) {
        String key  = rule.pathPrefix() + "|" + ip;
        long   now  = System.currentTimeMillis();
        long   cutoff = now - rule.windowMillis();

        Deque<Long> timestamps = windows.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Drop expired entries
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= rule.maxRequests()) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    /** Handles X-Forwarded-For from reverse proxies/CDN. */
    private String resolveClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
