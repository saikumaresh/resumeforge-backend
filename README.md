# ResumeForge — AI-Powered Resume Tailoring Backend

> An event-driven, cloud-native backend that automatically tailors resumes to job descriptions using local AI (Ollama) and computes ATS scores.

## Architecture

```
Client → [API Gateway :8080] → [Resume Service :8081] → Kafka → [Worker Service :8082]
                                        ↓                               ↓
                                  PostgreSQL                    Ollama LLM + PDF
                                  Redis Cache                   ATS Scorer
                                  Prometheus ← ← ← ← ← ← Grafana
```

## Services

| Service | Port | Responsibility |
|---------|------|----------------|
| api-gateway | 8080 | Spring Cloud Gateway, JWT auth header validation, Redis rate limiting |
| resume-service | 8081 | REST APIs, JPA model, Kafka producer, ATS scoring engine |
| worker-service | 8082 | Kafka consumer, Ollama LLM integration, PDF export |

## Tech Stack

Java 17 · Spring Boot 3.3 · Spring Cloud Gateway · Spring Data JPA · Apache Kafka · Redis · PostgreSQL · Flyway · Docker · Kubernetes · Prometheus · Grafana · OpenPDF · JUnit 5 · Mockito · Ollama (local LLM)

## Prerequisites

- Java 17+, Maven 3.8+, Docker Desktop, Git
- [Ollama](https://ollama.ai) installed and running locally
- Pull a model: `ollama pull llama3.2`

## Quick Start

```bash
# 1. Start infrastructure
cd infrastructure/docker && docker-compose up -d

# 2. Pull Ollama model (first time only)
ollama pull llama3.2

# 3. Start services (in separate terminals)
cd resume-service && mvn spring-boot:run
cd worker-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

## API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/resumes/users/{userId}/master` | Create master resume |
| GET | `/api/v1/resumes/users/{userId}/master` | List user's master resumes |
| GET | `/api/v1/resumes/{resumeId}/with-sections` | Get resume with all sections |
| POST | `/api/v1/resumes/{resumeId}/tailor` | Trigger AI tailoring + ATS scoring |
| GET | `/api/v1/resumes/tailored/{tailoredResumeId}` | Get tailored resume + ATS score |

## Kafka Topics

| Topic | Producer | Consumer |
|-------|----------|----------|
| `resume.tailoring.requested` | resume-service | worker-service |

## Monitoring

| Tool | URL | Credentials |
|------|-----|-------------|
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3000 | admin / admin |

## Running Tests

```bash
cd resume-service && mvn test
# Expected: Tests run: 15, Failures: 0, Errors: 0
```

## Git Workflow

GitFlow — `feature/*` branches off `develop`, merged via PR, released to `main`.

```
main
  └── develop
        ├── feature/project-scaffold
        ├── feature/infrastructure-docker
        ├── feature/jpa-model
        ├── feature/resume-service-api
        ├── feature/ats-scoring-engine
        ├── feature/worker-service-ollama-pdf
        └── feature/api-gateway-routing
```

## Database Schema

- `users` — user accounts
- `master_resumes` + `master_resume_sections` — source resume (with `@Version` optimistic locking)
- `job_descriptions` — target job postings
- `tailored_resumes` + `tailored_resume_sections` — AI-tailored output
- `ats_score_results` — ATS score breakdown per tailored resume
- `batch_requests` + `batch_items` — bulk tailoring support

## Author

Sai — GetSetAI | saikumaresh | Scaler Academy Module 16 Capstone
