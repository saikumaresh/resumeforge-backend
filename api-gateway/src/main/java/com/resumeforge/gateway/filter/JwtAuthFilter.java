package com.resumeforge.gateway.filter;

import com.resumeforge.gateway.config.JwtGatewayConfig;
import com.resumeforge.gateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global JWT authentication filter for API Gateway.
 *
 * ✅ PHASE 1.2 FIX: Validates JWT signature and expiration.
 * ✅ PHASE 1.7 FIX: Strict path matching for route protection.
 *
 * Applies JWT validation to all routes EXCEPT:
 * - /actuator/* (health checks, metrics)
 * - /api/v1/auth/login (user login)
 * - /api/v1/auth/register (user registration)
 * - /api/v1/auth/health (service health)
 *
 * Returns 401 Unauthorized if:
 * - No Authorization header
 * - Token doesn't start with "Bearer "
 * - Token signature is invalid
 * - Token is expired
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final JwtGatewayConfig jwtGatewayConfig;

    public JwtAuthFilter(JwtUtil jwtUtil, JwtGatewayConfig jwtGatewayConfig) {
        this.jwtUtil = jwtUtil;
        this.jwtGatewayConfig = jwtGatewayConfig;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        // ✅ PHASE 1.7: Use configuration-based route protection
        if (jwtGatewayConfig.isUnprotected(path)) {
            log.debug("[JWT] Allowing unprotected route: {}", path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        // Missing Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[JWT] Missing or invalid Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Extract token
        String token = authHeader.substring(7);

        // ✅ PHASE 1.2 FIX: Validate JWT signature and expiration
        if (!jwtUtil.isValid(token)) {
            log.warn("[JWT] Invalid or expired token for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Extract user ID and add to request headers
        try {
            UUID userId = jwtUtil.extractUserId(token);
            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header("X-User-Id", userId.toString())
                            .build())
                    .build();
            return chain.filter(mutatedExchange);
        } catch (Exception e) {
            log.error("[JWT] Error extracting user ID from token: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
