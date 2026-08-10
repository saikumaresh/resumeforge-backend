package com.resumeforge.gateway.filter;

import com.resumeforge.gateway.config.JwtGatewayConfig;
import com.resumeforge.gateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the gateway filter that stands in front of every proxied route.
 *
 * The filter has two jobs: refuse a request that cannot present a valid token,
 * and tell the downstream service who the caller is. The second job matters as
 * much as the first, because a downstream service that receives no identity
 * header cannot make an ownership decision.
 *
 * The chain is a hand-written stub rather than a mock so each test can see
 * whether the request continued and what it looked like when it did.
 */
class JwtAuthFilterTest {

    private static final String SECRET = "gateway-test-secret-at-least-32-bytes-long";

    private JwtAuthFilter filter;

    /** Records whether the chain ran, and with which exchange. */
    private AtomicReference<ServerWebExchange> forwarded;
    private GatewayFilterChain chain;

    private static String signedToken(String secret, UUID subject, long millisFromNow) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject.toString())
                .claim("email", "user@example.com")
                .issuedAt(new Date(now - 1000))
                .expiration(new Date(now + millisFromNow))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private static MockServerWebExchange exchangeFor(String path, String authHeader) {
        MockServerHttpRequest.BaseBuilder<?> b = MockServerHttpRequest.get(path);
        if (authHeader != null) {
            b = MockServerHttpRequest.get(path).header("Authorization", authHeader);
        }
        return MockServerWebExchange.from(b.build());
    }

    @BeforeEach
    void setUp() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", SECRET);
        filter = new JwtAuthFilter(jwtUtil, new JwtGatewayConfig());

        forwarded = new AtomicReference<>();
        chain = exchange -> {
            forwarded.set(exchange);
            return Mono.empty();
        };
    }

    @Test
    @DisplayName("an unprotected route is forwarded without any token")
    void unprotectedRoutePasses() {
        MockServerWebExchange exchange = exchangeFor("/api/v1/auth/login", null);

        filter.filter(exchange, chain).block();

        assertNotNull(forwarded.get(), "login must remain reachable without a token");
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("a protected route with no Authorization header is refused")
    void missingHeaderIsUnauthorized() {
        MockServerWebExchange exchange = exchangeFor("/api/v1/resumes/users/x/master", null);

        filter.filter(exchange, chain).block();

        assertNull(forwarded.get(), "the request must not reach the downstream service");
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("an Authorization header that is not a Bearer token is refused")
    void wrongSchemeIsUnauthorized() {
        MockServerWebExchange exchange =
                exchangeFor("/api/v1/resumes/users/x/master", "Basic dXNlcjpwYXNz");

        filter.filter(exchange, chain).block();

        assertNull(forwarded.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("a token signed with the wrong secret is refused")
    void forgedTokenIsUnauthorized() {
        String forged = signedToken("a-completely-different-secret-also-32-bytes",
                UUID.randomUUID(), 60_000);
        MockServerWebExchange exchange =
                exchangeFor("/api/v1/resumes/users/x/master", "Bearer " + forged);

        filter.filter(exchange, chain).block();

        assertNull(forwarded.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("an expired token is refused")
    void expiredTokenIsUnauthorized() {
        String stale = signedToken(SECRET, UUID.randomUUID(), -60_000);
        MockServerWebExchange exchange =
                exchangeFor("/api/v1/resumes/users/x/master", "Bearer " + stale);

        filter.filter(exchange, chain).block();

        assertNull(forwarded.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("a valid token is forwarded with the caller identity attached")
    void validTokenAddsUserIdHeader() {
        UUID userId = UUID.randomUUID();
        String token = signedToken(SECRET, userId, 60_000);
        MockServerWebExchange exchange =
                exchangeFor("/api/v1/resumes/users/x/master", "Bearer " + token);

        filter.filter(exchange, chain).block();

        assertNotNull(forwarded.get(), "a valid request must reach the downstream service");
        assertEquals(userId.toString(),
                forwarded.get().getRequest().getHeaders().getFirst("X-User-Id"),
                "the downstream service cannot check ownership without this header");
    }

    @Test
    @DisplayName("the filter runs before the routing filters")
    void filterOrderIsBeforeRouting() {
        assertTrue(filter.getOrder() < 0,
                "authentication must run before a request is routed anywhere");
    }
}
