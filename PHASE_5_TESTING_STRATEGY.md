# Phase 5: Comprehensive Testing Strategy

**Target:** 80% code coverage  
**Current Coverage:** 4% (ATSScorerTest, KeywordExtractorTest only)  
**Estimated Effort:** 34.5 hours  
**Priority:** 🔴 CRITICAL for production deployment

---

## Executive Summary

The ResumeForge backend is **feature-complete but test-immature**. Only 2 test files exist covering utility functions. **Zero tests** cover:
- Authentication (JWT generation, validation, expiry)
- Authorization (BOLA attacks)
- Rate limiting (DoS protection)
- Kafka consumer (async pipeline)
- API contracts (HTTP status codes)
- Database transactions (atomicity)

**Production Deployment Risk:** **HIGH**

This phase implements comprehensive testing across all critical paths, with 80% coverage target achieving a **confidence level suitable for production**.

---

## Testing Pyramid Strategy

```
                    ▲
                   ╱ ╲
                  ╱   ╲  End-to-End (1-2 hours)
                 ╱  E2E ╲  
                ╱________╲
               ╱          ╲
              ╱    Inte-   ╲ Integration Tests (6-8 hours)
             ╱   gration   ╲
            ╱______________╲
           ╱                ╲
          ╱      Unit Tests   ╲ Unit Tests (25-26 hours)
         ╱______________________╲

Testing Approach:
- Base (70%): Unit tests — Single class/method isolation
- Middle (20%): Integration tests — Multiple components together
- Top (10%): E2E tests — Real database + message broker
```

---

## Test Suites by Priority

### 🔴 CRITICAL (Must Have) — 16 hours

These tests prevent production disasters:

#### 1. **AuthServiceTest** (3 hours)
**What:** JWT generation, validation, user registration  
**Why Critical:** Authentication is the foundation of all security  
**Coverage:**
- ✓ JWT generation with correct claims
- ✓ JWT validation (signature, expiry, issuer)
- ✓ Invalid token rejection
- ✓ User registration (email validation, password hashing)
- ✓ Login (correct/incorrect credentials)
- ✓ Token refresh
- ✓ Email verification flow

**Key Scenarios:**
```java
@Test void validTokenAccepted()
@Test void expiredTokenRejected()
@Test void forgedTokenRejected()
@Test void registrationSucceeds()
@Test void loginWithInvalidCredentialsFails()
```

---

#### 2. **RateLimitFilterTest** (2 hours)
**What:** DoS protection via rate limiting  
**Why Critical:** Prevents brute force attacks, API abuse  
**Coverage:**
- ✓ Rate limit enforced (10/60s for login, 5/60s for register)
- ✓ 429 Too Many Requests returned when exceeded
- ✓ Counter resets after time window
- ✓ IP-based limits (not account-based)
- ✓ Bypasses for admin endpoints

**Key Scenarios:**
```java
@Test void loginRateLimited10Per60Seconds()
@Test void registerRateLimited5Per60Seconds()
@Test void tooManyRequestsReturns429()
@Test void counterResetsAfterWindow()
```

---

#### 3. **BOLATest** — Authorization (3 hours)
**What:** Broken Object Level Authorization (can user X access user Y's data?)  
**Why Critical:** Prevents user impersonation, data leaks  
**Coverage:**
- ✓ User can access own resumes only
- ✓ User CANNOT access other user's resumes
- ✓ User CANNOT update other user's resumes
- ✓ User CANNOT delete other user's job descriptions
- ✓ Admin CAN access all user data
- ✓ Service-to-service auth (Kafka events only process own data)

**Key Scenarios:**
```java
@Test void userCanAccessOwnResumes()
@Test void userCannotAccessOtherUserResumes()
@Test void nonOwnerGetReturns403Forbidden()
@Test void adminCanAccessAllData()
@Test void kafkaEventProcessedOnlyForOwnResumes()
```

---

#### 4. **KafkaConsumerTest** (4 hours)
**What:** Async resume tailoring pipeline (most complex)  
**Why Critical:** Data corruption, duplicate processing, lost messages  
**Coverage:**
- ✓ Kafka consumer receives tailoring requests
- ✓ Idempotency (duplicate messages skipped)
- ✓ Failed processing stored in DLQ (Dead Letter Queue)
- ✓ Retry logic (exponential backoff)
- ✓ Ollama failures handled gracefully
- ✓ Database transaction atomicity (all-or-nothing)
- ✓ Status updates: PENDING → PROCESSING → COMPLETED/FAILED

**Key Scenarios:**
```java
@Test void tailoringRequestProcessed()
@Test void duplicateMessageSkipped()
@Test void failedMessageSentToDLQ()
@Test void retryAfterOllamaTimeout()
@Test void databaseTransactionRolledBackOnError()
@Test void statusUpdatedCorrectly()
```

---

#### 5. **JwtAuthFilterTest** (2 hours)
**What:** Gateway-level JWT validation  
**Why Critical:** Prevents unauthenticated requests reaching backend  
**Coverage:**
- ✓ Valid JWT accepted, forwarded to backend
- ✓ Missing JWT returns 401
- ✓ Expired JWT returns 401
- ✓ Invalid signature returns 401
- ✓ /auth endpoints bypass JWT check
- ✓ User ID extracted and added to headers

**Key Scenarios:**
```java
@Test void validTokenForwarded()
@Test void missingTokenReturns401()
@Test void expiredTokenReturns401()
@Test void authEndpointsAllowedWithoutToken()
@Test void userIdAddedToHeaders()
```

---

#### 6. **ResumeServiceTest** (2 hours)
**What:** Core business logic (tailoring, scoring)  
**Why Critical:** Main revenue path, user-facing feature  
**Coverage:**
- ✓ Resume tailoring triggered correctly
- ✓ ATS scoring calculated correctly
- ✓ PDF generation called (or fallback when disabled)
- ✓ Error states handled gracefully
- ✓ Kafka message published
- ✓ Database transaction atomicity

**Key Scenarios:**
```java
@Test void tailoringRequestQueued()
@Test void atsScoreCalculated()
@Test void pdfGenerationAttempted()
@Test void kafkaMessagePublished()
@Test void errorHandledGracefully()
```

---

### 🟠 HIGH PRIORITY (Should Have) — 10 hours

#### 7. **ControllerIntegrationTests** (6 hours)
**What:** All REST endpoints end-to-end  
**Why Important:** API contract validation, HTTP status codes  
**Coverage:**
- ✓ POST /api/v1/auth/register → 201 Created
- ✓ POST /api/v1/auth/login → 200 OK
- ✓ POST /api/v1/resumes/tailor → 202 Accepted (async)
- ✓ GET /api/v1/resumes/{id} → 200 OK or 404 Not Found
- ✓ DELETE /api/v1/resumes/{id} → 204 No Content
- ✓ Invalid input → 400 Bad Request
- ✓ Unauthorized → 401 Unauthorized
- ✓ Forbidden → 403 Forbidden

**Key Scenarios:**
```java
@Test void registerReturns201Created()
@Test void loginReturns200OK()
@Test void tailorReturns202Accepted()
@Test void invalidInputReturns400()
@Test void unauthorizedReturns401()
@Test void forbiddenReturns403()
@Test void notFoundReturns404()
```

---

#### 8. **DatabaseIntegrationTests** (4 hours)
**What:** Transaction atomicity, FK constraints, cascading deletes  
**Why Important:** Data integrity, prevents corruption  
**Coverage:**
- ✓ Insert/update/delete operations succeed
- ✓ FK constraints enforced (can't insert orphaned records)
- ✓ Cascade delete works (delete user → all related records gone)
- ✓ Transactions roll back on error (all-or-nothing)
- ✓ Unique constraints enforced (duplicate email rejected)
- ✓ CHECK constraints enforced (invalid enum rejected)

**Key Scenarios:**
```java
@Test void insertValidRecordSucceeds()
@Test void foreignKeyConstraintEnforced()
@Test void cascadeDeleteRemovesChildRecords()
@Test void transactionRolledBackOnError()
@Test void uniqueConstraintEnforced()
@Test void checkConstraintEnforced()
```

---

### 🟡 MEDIUM PRIORITY (Nice to Have) — 8.5 hours

#### 9. **OllamaClientTest** (2.5 hours)
**What:** Circuit breaker behavior, fallback handling  
**Why Important:** Graceful degradation when LLM down  
**Coverage:**
- ✓ Successful API call returns tailored resume
- ✓ Circuit breaker opens after threshold failures
- ✓ Fallback resume returned when circuit open
- ✓ Exponential backoff on retries
- ✓ Timeout handled gracefully

---

#### 10. **InputSanitizerTest** (2 hours)
**What:** Prompt injection prevention  
**Why Important:** Security against adversarial input  
**Coverage:**
- ✓ Malicious prompts blocked
- ✓ Valid input passes through
- ✓ Edge cases (empty, null) handled

---

#### 11. **FrontendSecurityTests** (2 hours)
**What:** JavaScript-level security  
**Why Important:** XSS, CSRF prevention  
**Coverage:**
- ✓ JWT not accessible via JS (httpOnly)
- ✓ CSRF token validated
- ✓ XSS payloads in input don't execute
- ✓ Secure headers set

---

#### 12. **ConfigurationTests** (2 hours)
**What:** Application startup, config validation  
**Why Important:** Catches config errors before deployment  
**Coverage:**
- ✓ Application starts with minimal config
- ✓ Required env vars enforced
- ✓ Default values work for dev

---

## Implementation Roadmap

### Week 1: Critical Tests (16 hours)
```
Day 1: AuthServiceTest (3h) + RateLimitFilterTest (2h) = 5h
Day 2: BOLATest (3h) + JwtAuthFilterTest (2h) = 5h
Day 3: KafkaConsumerTest (4h) + ResumeServiceTest (2h) = 6h
Total: 16 hours
```

### Week 2: Integration Tests (10 hours)
```
Day 1: ControllerIntegrationTests (6h) = 6h
Day 2: DatabaseIntegrationTests (4h) = 4h
Total: 10 hours
```

### Week 3: Additional Tests (8.5 hours)
```
Day 1: OllamaClientTest (2.5h) + InputSanitizerTest (2h) = 4.5h
Day 2: FrontendSecurityTests (2h) + ConfigurationTests (2h) = 4h
Total: 8.5 hours
```

---

## Test Infrastructure Setup

### 1. Test Dependencies (pom.xml)
```xml
<!-- Unit Testing -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Mocking -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>

<!-- Integration Testing -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.0</version>
    <scope>test</scope>
</dependency>

<!-- Kafka Testing -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <version>1.19.0</version>
    <scope>test</scope>
</dependency>

<!-- Code Coverage -->
<dependency>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
</dependency>
```

### 2. Test Configuration (application-test.yml)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
  kafka:
    bootstrap-servers: localhost:9092
    
jwt:
  secret: test-secret-key-32-chars-minimum-length
```

### 3. Base Test Class
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    @Autowired protected TestRestTemplate restTemplate;
    @Autowired protected EntityManager entityManager;
    
    @BeforeEach
    void cleanup() {
        entityManager.flush();
        entityManager.clear();
    }
}
```

---

## Coverage Target: 80%

### By Module:

```
Auth (AuthService, JwtUtil, JwtAuthFilter):        95% ✓
Rate Limiting (RateLimitFilter):                   90% ✓
Authorization (BOLA checks):                       85% ✓
Kafka Consumer (TailoringConsumer):                80% ✓
Resume Service (tailoring, scoring):               85% ✓
Controllers (REST endpoints):                      90% ✓
Database (repositories, migrations):               75% ✓
LLM Client (OllamaClient, circuit breaker):        70% ✓
Input Validation (sanitizer, guardrails):          80% ✓
Utilities (ATSScorer, KeywordExtractor):           95% ✓ (existing)

OVERALL TARGET:                                    80%
```

---

## Continuous Integration

### GitHub Actions Workflow

```yaml
name: Tests & Coverage
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up Java
        uses: actions/setup-java@v3
        with:
          java-version: 21
      - name: Run tests
        run: mvn clean test
      - name: Generate coverage report
        run: mvn jacoco:report
      - name: Check coverage (80% minimum)
        run: |
          COVERAGE=$(cat target/site/jacoco/index.html | grep -oP '(?<=<td class="ctr2">\d{1,3}%')
          if [ $COVERAGE -lt 80 ]; then exit 1; fi
      - name: Upload to CodeCov
        uses: codecov/codecov-action@v3
```

---

## Execution Checklist

- [ ] All test dependencies added to pom.xml
- [ ] Test configuration created (application-test.yml)
- [ ] Base test class created
- [ ] AuthServiceTest (3h)
- [ ] RateLimitFilterTest (2h)
- [ ] BOLATest (3h)
- [ ] KafkaConsumerTest (4h)
- [ ] JwtAuthFilterTest (2h)
- [ ] ResumeServiceTest (2h)
- [ ] ControllerIntegrationTests (6h)
- [ ] DatabaseIntegrationTests (4h)
- [ ] OllamaClientTest (2.5h)
- [ ] InputSanitizerTest (2h)
- [ ] FrontendSecurityTests (2h)
- [ ] ConfigurationTests (2h)
- [ ] Coverage report generated
- [ ] Coverage ≥ 80% verified
- [ ] All tests pass in CI/CD
- [ ] Code review completed

---

## Success Metrics

**Before Phase 5:**
- Coverage: 4%
- Confidence: LOW
- Production Risk: CRITICAL

**After Phase 5:**
- Coverage: 80%
- Confidence: HIGH
- Production Risk: LOW
- Test Count: 150+
- Execution Time: < 5 minutes (CI/CD)

---

## References

- [Testing Best Practices](https://martinfowler.com/articles/practical-test-pyramids.html)
- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)
- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)

---

**Status:** Strategy Complete | Implementation Ready  
**Next:** Start with Critical Tests (AuthServiceTest, RateLimitFilterTest, BOLATest)

Generated: 2026-06-08
