# ResumeForge — AI-Powered Resume Tailoring Backend

An event-driven backend that tailors a master resume to a specific job description using an LLM, then scores the result against that description for ATS keyword coverage. Tailoring runs asynchronously over Kafka so the API stays responsive while the model works.

**Frontend:** [saikumaresh/resumeforge-frontend](https://github.com/saikumaresh/resumeforge-frontend) (Next.js 16 / React 19)

---

## Architecture

```
                    ┌──────────────────┐
   Client  ───────▶ │ resume-service   │ ──── Kafka ────▶ ┌─────────────────┐
                    │      :8081       │  resume.tailoring │ worker-service  │
                    │                  │     .requested    │      :8082      │
                    │ REST · JWT auth  │                   │                 │
                    │ JPA · ATS score  │ ◀── writes back ──│ Ollama LLM      │
                    └────────┬─────────┘                   │ ATS scoring     │
                             │                             └────────┬────────┘
                        PostgreSQL                                  │
                        (Flyway V1–V8)                            Redis
                                                            (idempotency keys)
```

`api-gateway` (:8080) is a Spring Cloud Gateway instance that proxies `/api/v1/resumes/**`. It is **not** on the primary request path — see [Known limitations](#known-limitations).

### Services

| Service | Port | Responsibility |
|---------|------|----------------|
| `resume-service` | 8081 | REST API, JWT auth, JPA persistence, Kafka producer |
| `worker-service` | 8082 | Kafka consumer, Ollama LLM tailoring, ATS scoring, idempotency |
| `api-gateway` | 8080 | Spring Cloud Gateway route for `/api/v1/resumes/**` |

### Tech stack

Java 21 · Spring Boot 3.3 · Spring Security · Spring Data JPA · Spring Cloud Gateway · Apache Kafka · PostgreSQL · Flyway · Redis · Ollama · Micrometer/Prometheus · Docker · JUnit 5 · JaCoCo

---

## Quick Start

### Prerequisites

- **JDK 21** (the build sets `<java.version>21</java.version>`; JDK 17 will not compile)
- Maven 3.8+
- Docker Desktop — for PostgreSQL, Kafka and Redis
- An Ollama endpoint. Defaults to hosted `https://ollama.com`; set `OLLAMA_API_URL` to point at a local instance.

### 1. Start infrastructure

```bash
cd infrastructure/docker && docker-compose up -d
```

This starts PostgreSQL, Kafka, Zookeeper, Redis, Prometheus and Grafana. It does **not** start the application services.

### 2. Configure the environment

No service has a usable default for its database password or JWT secret — both fail closed rather than shipping a working default. Export these before starting anything:

```bash
export DATABASE_PASSWORD=resumeforge123          # matches docker-compose.yml
export JWT_SECRET=local-dev-secret-at-least-32-characters-long
export OLLAMA_API_URL=http://localhost:11434/v1/chat/completions
export OLLAMA_MODEL=llama3.2                     # or leave unset for gemma3:12b
```

See [ENV_SETUP_GUIDE.md](ENV_SETUP_GUIDE.md) for all 22 supported variables.

### 3. Run the services

```bash
mvn -pl resume-service spring-boot:run    # :8081
mvn -pl worker-service spring-boot:run    # :8082
mvn -pl api-gateway   spring-boot:run     # :8080
```

Flyway applies migrations V1–V8 on first start of `resume-service`.

---

## API Reference

All endpoints are under `http://localhost:8081`. Every route except `/api/v1/auth/**` requires `Authorization: Bearer <jwt>`.

### Authentication

| Method | Endpoint | Success | Description |
|--------|----------|---------|-------------|
| POST | `/api/v1/auth/register` | 201 | Create account, returns JWT |
| POST | `/api/v1/auth/login` | 200 | Exchange credentials for JWT |
| GET | `/api/v1/auth/me` | 200 | Current user profile |

### Resumes

| Method | Endpoint | Success | Description |
|--------|----------|---------|-------------|
| POST | `/api/v1/resumes/users/{userId}/master` | 201 | Create a master resume |
| GET | `/api/v1/resumes/users/{userId}/master` | 200 | List a user's master resumes |
| PUT | `/api/v1/resumes/users/{userId}/master` | 200 | Upsert master resume content |
| GET | `/api/v1/resumes/users/{userId}/master/first` | 200 / 404 | First master resume |
| GET | `/api/v1/resumes/{resumeId}/with-sections` | 200 | Master resume with sections |
| POST | `/api/v1/resumes/{masterResumeId}/tailor` | **202** | Queue a tailoring job |
| GET | `/api/v1/resumes/tailored/{id}` | 200 | Tailored resume + ATS score |
| GET | `/api/v1/resumes/users/{userId}/tailored` | 200 | All tailored resumes for a user |
| POST | `/api/v1/resumes/tailored/{id}/retry` | **202** | Re-queue a FAILED job |
| PUT | `/api/v1/resumes/tailored/{id}/sections` | 200 | Edit tailored sections |
| POST | `/api/v1/resumes/tailored/{id}/chat` | 200 | Ask the LLM about a resume |

Asynchronous operations return **202 Accepted** with a `PENDING` record; poll the `GET` endpoint for completion.

### Kafka topics

| Topic | Producer | Consumer |
|-------|----------|----------|
| `resume.tailoring.requested` | resume-service | worker-service |

---

## Security

- **Passwords** — BCrypt via `BCryptPasswordEncoder`
- **Authentication** — stateless JWT (jjwt 0.12.6, `verifyWith().parseSignedClaims()`, so unsigned and `alg:none` tokens are rejected)
- **Authorization** — every data-touching service method calls `ResumeService.assertOwnership()`, which compares the resource owner against the JWT principal and returns 403 on mismatch. Verified by `BOLATest`, which drives two real users over HTTP.
- **Rate limiting** — sliding window on `/auth/login` (10/min) and `/auth/register` (5/min)
- **Injection** — all repository access is parameterised JPQL; there are no native query strings built from user input in `resume-service`
- **Secrets** — every production value in the services resolves from an environment variable and fails closed if unset; no credential is committed in application code or config. The Kubernetes manifests are the exception and still carry a plaintext database password — see [Known limitations](#known-limitations).
- **Error handling** — `GlobalExceptionHandler` returns a correlation ID rather than a stack trace; `server.error.include-message: never`

---

## Testing

```bash
mvn test                                  # 155 tests
mvn test && open resume-service/target/site/jacoco/index.html   # coverage
```

**Current state — 155 tests, 0 failures, 0 errors:**

| Suite | Tests | Covers |
|-------|-------|--------|
| `ResumeServiceTest` | 17 | Service logic with mocked collaborators; ownership, Kafka publish, retry state machine |
| `BOLATest` | 11 | Cross-tenant authorization over HTTP, incl. chat endpoint (403/401) |
| `AuthServiceTest` | 13 | Registration, login, hashing, email normalisation |
| `RateLimitFilterTest` | 7 | Sliding-window limits, per-IP isolation, 429 |
| `ATSScorerTest` + edge cases | 21 | Scoring maths and boundary conditions |
| `KeywordExtractorTest` + edge cases | 24 | Tokenisation, stop-words, extraction |
| `ATSScorerTest` (worker) | 18 | The scorer that actually runs in production |
| `TailoringGuardrailValidatorTest` | 14 | LLM output validation and rejection paths |
| `IdempotencyServiceTest` | 7 | Redis-backed duplicate suppression |
| `JwtUtilTest` (gateway) | 9 | Token validation at the edge: forged, expired, unsigned, malformed |
| `JwtGatewayConfigTest` (gateway) | 6 | Which paths are reachable without a token |
| `JwtAuthFilterTest` (gateway) | 7 | 401 paths, and the `X-User-Id` header added on success |

**Measured coverage (JaCoCo):** `api-gateway` 84.0% line, `resume-service` 64.6% line, `worker-service` 28.6% line, 55.0% across all three.

---

## Known limitations

Stated explicitly rather than left for a reader to discover:

- **PDF export is implemented but not wired.** `ResumePDFGenerator` works, but `TailoringConsumer` sets `pdfPath = null` because no object storage is configured, so `pdfDownloadUrl` is absent from responses.
- **Kubernetes manifests hold a plaintext password.** `infrastructure/kubernetes/*/deployment.yaml` sets `SPRING_DATASOURCE_PASSWORD` to a literal value, contradicting the environment-variable model the services use. Should be a Kubernetes Secret.
- **Scoring logic is duplicated.** `ATSScorer` exists in both services and the copies have diverged. The production copy in `worker-service` is now tested; the `resume-service` copy is dead code and should be removed once the logic is extracted into a shared module.
- **Kafka publish happens inside the database transaction** — a dual-write with no outbox, so a broker failure after commit loses the event. `retryTailoring` is the manual recovery path.
- **The gateway's unprotected-route check matches on prefixes.** `JwtGatewayConfig.isUnprotected()` uses `String.startsWith`, so a path that merely begins with an open route is let through without a token. No route in the system is shaped that way today, and `JwtGatewayConfigTest` records the behaviour; the fix is to match whole path segments.
- **Kubernetes manifests are illustrative.** No manifests exist for PostgreSQL, Kafka or Redis, and no images are published to a registry.

---

## Database schema

| Table | Purpose |
|-------|---------|
| `users` | Accounts and BCrypt password hashes |
| `master_resumes` / `master_resume_sections` | Source resume, `@Version` optimistic locking |
| `job_descriptions` | Target postings |
| `tailored_resumes` / `tailored_resume_sections` | LLM output per job |
| `ats_score_results` | Score breakdown per tailored resume |

Three further tables are created by the migrations but mapped by no entity and touched by no code: `batch_requests` and `batch_items` (V3, abandoned batch feature) and `subscriptions` (V8, removed payment integration). A database built from V1–V8 therefore holds ten tables, not seven.

Migrations `V1`–`V8` in `resume-service/src/main/resources/db/migration`. `spring.jpa.hibernate.ddl-auto=validate` — the schema is owned by Flyway, not Hibernate.

---

## Repository layout

```
resumeforge-backend/
├── api-gateway/            Spring Cloud Gateway
├── resume-service/         REST API, persistence, Kafka producer
├── worker-service/         Kafka consumer, LLM, scoring
└── infrastructure/
    ├── docker/             docker-compose (Postgres, Kafka, Redis, Prometheus, Grafana)
    ├── kubernetes/         Deployment manifests
    ├── monitoring/         Prometheus scrape config
    └── grafana/            Dashboards and provisioning
```
