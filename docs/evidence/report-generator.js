const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType, PageBreak,
  Table, TableRow, TableCell, WidthType, ShadingType, BorderStyle, LevelFormat,
  TableOfContents, Footer, PageNumber, convertInchesToTwip,
} = require('docx');
const fs = require('fs');

/* ─────────────── helpers ─────────────── */

const ACCENT = '1F4E79';
const GREY = '595959';

const p = (text, opts = {}) => new Paragraph({
  spacing: { after: opts.after ?? 120, line: 276 },
  alignment: opts.align,
  indent: opts.indent,
  children: [new TextRun({ text, size: opts.size ?? 22, bold: opts.bold, italics: opts.italics, color: opts.color, font: opts.font })],
});

const h1 = (text) => new Paragraph({
  heading: HeadingLevel.HEADING_1,
  spacing: { before: 360, after: 180 },
  children: [new TextRun({ text, size: 32, bold: true, color: ACCENT })],
});

const h2 = (text) => new Paragraph({
  heading: HeadingLevel.HEADING_2,
  spacing: { before: 260, after: 130 },
  children: [new TextRun({ text, size: 26, bold: true, color: ACCENT })],
});

const h3 = (text) => new Paragraph({
  heading: HeadingLevel.HEADING_3,
  spacing: { before: 200, after: 100 },
  children: [new TextRun({ text, size: 23, bold: true, color: GREY })],
});

const bullet = (text, level = 0) => new Paragraph({
  numbering: { reference: 'bullets', level },
  spacing: { after: 80, line: 276 },
  children: [new TextRun({ text, size: 22 })],
});

// Code / diagram block: monospace, shaded, no bullet
const code = (lines) => lines.map((l, i) => new Paragraph({
  spacing: { after: i === lines.length - 1 ? 160 : 0, line: 240 },
  shading: { type: ShadingType.CLEAR, fill: 'F4F6F8' },
  indent: { left: convertInchesToTwip(0.2) },
  children: [new TextRun({ text: l || ' ', size: 17, font: 'Consolas' })],
}));

const pageBreak = () => new Paragraph({ children: [new PageBreak()] });

/* Table with correct dual widths (DXA) */
const TOTAL = 9360; // ~6.5in usable on A4 with 1in margins
function table(headers, rows, weights) {
  const w = weights || headers.map(() => 1);
  const sum = w.reduce((a, b) => a + b, 0);
  const cols = w.map((x) => Math.round((x / sum) * TOTAL));
  cols[cols.length - 1] = TOTAL - cols.slice(0, -1).reduce((a, b) => a + b, 0);

  const cell = (text, i, opts = {}) => new TableCell({
    width: { size: cols[i], type: WidthType.DXA },
    shading: opts.head ? { type: ShadingType.CLEAR, fill: ACCENT } : undefined,
    margins: { top: 60, bottom: 60, left: 100, right: 100 },
    children: [new Paragraph({
      spacing: { after: 0, line: 240 },
      children: [new TextRun({
        text, size: 19, bold: opts.head,
        color: opts.head ? 'FFFFFF' : undefined,
        font: opts.mono ? 'Consolas' : undefined,
      })],
    })],
  });

  return new Table({
    width: { size: TOTAL, type: WidthType.DXA },
    columnWidths: cols,
    rows: [
      new TableRow({
        tableHeader: true,
        children: headers.map((hd, i) => cell(hd, i, { head: true })),
      }),
      ...rows.map((r) => new TableRow({
        children: r.map((c, i) => cell(String(c), i, { mono: i === 0 && c.startsWith('/') })),
      })),
    ],
  });
}

const spacer = (n = 1) => Array.from({ length: n }, () => p('', { after: 0 }));

/* ─────────────── document ─────────────── */

const children = [];

/* ---- Title page ---- */
children.push(
  ...spacer(6),
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 100 },
    children: [new TextRun({ text: 'ResumeForge', size: 72, bold: true, color: ACCENT })],
  }),
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 600 },
    children: [new TextRun({ text: 'An Event-Driven Backend for AI Resume Tailoring and ATS Scoring', size: 28, color: GREY })],
  }),
  p('Final Capstone Project Report', { align: AlignmentType.CENTER, size: 24, bold: true }),
  ...spacer(2),
  p('Sai Kumaresh', { align: AlignmentType.CENTER, size: 24 }),
  p('Scaler — Backend Engineering Capstone', { align: AlignmentType.CENTER, size: 22, color: GREY }),
  ...spacer(3),
  p('Backend repository', { align: AlignmentType.CENTER, size: 20, bold: true, color: GREY }),
  p('github.com/saikumaresh/resumeforge-backend', { align: AlignmentType.CENTER, size: 20, font: 'Consolas' }),
  ...spacer(1),
  p('Frontend repository', { align: AlignmentType.CENTER, size: 20, bold: true, color: GREY }),
  p('github.com/saikumaresh/resumeforge-frontend', { align: AlignmentType.CENTER, size: 20, font: 'Consolas' }),
  pageBreak(),
);

/* ---- TOC ---- */
children.push(
  h1('Contents'),
  new TableOfContents('Contents', { hyperlink: true, headingStyleRange: '1-2' }),
  pageBreak(),
);

/* ---- 1. Abstract ---- */
children.push(
  h1('1. Abstract'),
  p('ResumeForge is a backend system that adapts a candidate’s master resume to a specific job description using a large language model, then scores the adapted result for applicant-tracking-system (ATS) keyword coverage. Because language-model inference takes tens of seconds, the tailoring workload is decoupled from the HTTP request cycle: the API accepts a request, persists a PENDING record, publishes an event to Apache Kafka, and returns 202 Accepted immediately. A separate worker service consumes that event, calls the model, validates the output, computes the score, and writes the result back.'),
  p('The system is implemented as three Spring Boot 3.3 services on Java 21, backed by PostgreSQL with Flyway-managed schema migrations and Redis for consumer idempotency. Authentication is stateless JWT; authorization is enforced at the service layer through an ownership check applied to every data-touching operation, and is verified by an automated cross-tenant test suite that drives two distinct users over HTTP. The codebase carries 76 automated tests with measured JaCoCo coverage of 49.4% line and 52.2% instruction on the primary API service.'),
  p('This report describes the architecture and the reasoning behind it, documents the data model and API surface, explains the asynchronous processing pipeline and the guardrails applied around model output, presents the testing strategy and its measured results, and closes with an explicit account of the system’s current limitations and the work that would follow.'),
);

/* ---- 2. Problem ---- */
children.push(
  h1('2. Problem Statement'),
  p('A candidate applying to many roles faces a repetitive and error-prone task. Most job applications are first read by an applicant tracking system that filters on keyword overlap with the posting rather than on the substance of the candidate’s experience. A resume that is a strong match in reality can be filtered out because it uses different vocabulary from the job description. The practical response is to rewrite the resume for each application, which is slow, and to guess at which terms matter, which is unreliable.'),
  h2('2.1 Objective'),
  p('Build a backend that, given one master resume and one job description, produces a tailored version of the resume and a quantified score describing how well that version matches the posting — with the rewriting performed by a language model and the score computed deterministically so it can be explained and reproduced.'),
  h2('2.2 Scope'),
  bullet('Account creation and stateless authentication'),
  bullet('Storage of a structured master resume divided into typed sections'),
  bullet('Asynchronous tailoring of that resume against a supplied job description'),
  bullet('Deterministic ATS scoring of the tailored output, with a breakdown by component'),
  bullet('Manual editing of tailored output, and a conversational endpoint for model-assisted revision'),
  bullet('Retry of failed tailoring jobs'),
  h2('2.3 Explicit non-goals'),
  p('The following were deliberately excluded, and their absence is a scoping decision rather than an incomplete feature:'),
  bullet('Payments and subscription tiers. An earlier iteration integrated a payment gateway; it was removed because it added billing complexity without exercising any backend concept the project set out to demonstrate.'),
  bullet('Hosted PDF delivery. PDF rendering is implemented but not wired into the pipeline, because no object storage is provisioned. This is discussed in Section 12.'),
  bullet('Multi-tenancy beyond per-user isolation, and any administrative or role-based access tier.'),
);

/* ---- 3. Architecture ---- */
children.push(
  pageBreak(),
  h1('3. System Architecture'),
  p('The system is a three-service Maven reactor. The split follows the dominant runtime characteristic of each workload rather than a domain boundary: one service serves fast, synchronous, transactional HTTP traffic, and a second performs slow, retryable, failure-prone work against an external model.'),
  ...code([
    '                        +---------------------+',
    '   HTTP client -------> |   resume-service    |',
    '                        |       :8081         |',
    '                        |                     |',
    '                        | REST + JWT auth     |',
    '                        | JPA persistence     |',
    '                        | Kafka producer      |',
    '                        +----+-----------+----+',
    '                             |           |',
    '                    PostgreSQL      Kafka topic',
    '                    (Flyway V1-V8)  resume.tailoring.requested',
    '                             |           |',
    '                             |           v',
    '                             |   +---------------------+',
    '                             |   |   worker-service    |',
    '                             |   |       :8082         |',
    '                             |   |                     |',
    '                             +---+ Kafka consumer      |',
    '                        writes    | Ollama LLM client   |',
    '                        result    | Output guardrails   |',
    '                                  | ATS scoring         |',
    '                                  +----------+----------+',
    '                                             |',
    '                                          Redis',
    '                                  (idempotency, 24h TTL)',
  ]),
  h2('3.1 Service responsibilities'),
  table(
    ['Service', 'Port', 'Responsibility'],
    [
      ['resume-service', '8081', 'REST API, JWT authentication and ownership authorization, JPA persistence, Flyway migrations, Kafka producer'],
      ['worker-service', '8082', 'Kafka consumer, Ollama client, output validation, ATS scoring, Redis-backed idempotency'],
      ['api-gateway', '8080', 'Spring Cloud Gateway; routes /api/v1/resumes/** to resume-service'],
    ],
    [2, 1, 5],
  ),
  p(''),
  h2('3.2 Why the workload is split'),
  p('A tailoring request takes as long as the language model takes to respond — typically tens of seconds, occasionally much longer, and sometimes it fails outright. Handling that inside the HTTP request would hold a servlet thread for the duration, expose the caller to model latency and model failure, and make the API’s throughput a function of the model’s speed.'),
  p('Placing a durable queue between the two means the API’s obligation ends once the request is persisted and the event is published. The worker can be slow, can be restarted, and can fail and retry without the client observing anything beyond a status field that has not yet moved to COMPLETED. The two halves can also be scaled independently, which matters because their bottlenecks are entirely different: the API is bound by database connections, the worker by model throughput.'),
  h2('3.3 Layering within resume-service'),
  p('The API service follows a conventional three-layer structure, with one deliberate rule about where authorization lives.'),
  bullet('Controller — HTTP concerns only: routing, deserialization, bean validation via @Valid, and status-code selection. Controllers contain no business logic.'),
  bullet('Service — business rules, transaction boundaries, and authorization. Every method that reads or writes user-owned data calls assertOwnership() before touching it.'),
  bullet('Repository — Spring Data JPA interfaces. Custom queries are JPQL with bound parameters; several use explicit LEFT JOIN FETCH to avoid N+1 selects.'),
  p('Authorization is enforced in the service layer rather than in controllers or a filter. A filter can only see the URL, which is not enough to decide ownership of a resource identified by an opaque UUID; and placing the check in the controller would let any future caller of the same service method bypass it. Enforcing it at the service boundary means the check cannot be skipped by adding a new entry point.'),
);

/* ---- 4. Tech choices ---- */
children.push(
  pageBreak(),
  h1('4. Technology Choices and Rationale'),
  table(
    ['Technology', 'Used for', 'Why this rather than the alternative'],
    [
      ['Java 21 / Spring Boot 3.3', 'All three services', 'Mature ecosystem for the exact concerns here — transactions, security, messaging — with first-class Kafka and JPA integration.'],
      ['PostgreSQL', 'Primary datastore', 'The data is relational: users own resumes, resumes own sections, tailored output references both a resume and a posting. Referential integrity and transactions are wanted, not avoided.'],
      ['Flyway', 'Schema migration', 'Versioned, ordered, reviewable SQL. Hibernate runs with ddl-auto=validate, so the schema is owned by migrations and drift fails at startup rather than silently mutating tables.'],
      ['Apache Kafka', 'Async tailoring queue', 'Durable and replayable. A crashed consumer resumes from its committed offset rather than losing in-flight work, which an in-memory queue would.'],
      ['Redis', 'Consumer idempotency', 'Kafka delivers at-least-once, so a redelivered event would otherwise cause a second paid model call. A short-lived keyed marker with a TTL is exactly the right shape for this, and is wasteful to model as a table.'],
      ['Ollama', 'LLM inference', 'An OpenAI-compatible chat-completions interface that can run locally, avoiding per-token cost during development while leaving the option of a hosted endpoint via configuration.'],
      ['JWT (jjwt 0.12.6)', 'Authentication', 'Stateless, so no session store is needed and any service instance can verify a token independently.'],
      ['JaCoCo', 'Coverage measurement', 'Replaces estimated coverage with a measured figure.'],
    ],
    [2, 2, 5],
  ),
);

/* ---- 5. Data model ---- */
children.push(
  pageBreak(),
  h1('5. Data Model'),
  p('Seven tables, created and evolved by eight Flyway migrations. Hibernate validates the entity mapping against this schema at startup.'),
  table(
    ['Table', 'Purpose', 'Notable columns and constraints'],
    [
      ['users', 'Accounts', 'Unique email; BCrypt hash in password_hash. No plaintext password is ever stored.'],
      ['master_resumes', 'Source resume', 'user_id owner; @Version column for optimistic locking.'],
      ['master_resume_sections', 'Typed sections', 'Enum section_type; position for ordering; NOT NULL FK to the parent resume.'],
      ['job_descriptions', 'Target posting', 'Company, title, description text, required skills.'],
      ['tailored_resumes', 'LLM output', 'FKs to master resume and job description; status enum PENDING / PROCESSING / COMPLETED / FAILED.'],
      ['tailored_resume_sections', 'Tailored sections', 'Per-section rewritten content.'],
      ['ats_score_results', 'Score breakdown', 'Total score plus the keyword, section and action-verb components.'],
    ],
    [3, 2, 5],
  ),
  p(''),
  h2('5.1 Optimistic locking'),
  p('A master resume can be edited from more than one place — the editor auto-saves, and a tailoring job reads the same row. MasterResume carries a @Version column, so a write against a stale version fails rather than silently discarding a concurrent edit. This is preferable to pessimistic locking here because contention is rare and holding row locks across a user’s editing session would be far more disruptive than occasionally asking a client to retry.'),
  h2('5.2 A cascade defect found during review'),
  p('The upsert path for a master resume clears the existing section collection and adds a replacement. The association was mapped with cascade = ALL but without orphanRemoval. For a mappedBy (inverse) collection Hibernate performs no collection-level DML — the foreign key is owned by the child — so the cleared children were never deleted. Because master_resume_sections.master_resume_id is NOT NULL, Hibernate could not detach them either. The observable effect was that every save appended a duplicate section row instead of replacing the existing one, on the most frequently exercised write path in the application.'),
  p('The fix was to add orphanRemoval = true to the association, which marks removed children for deletion. This defect is worth recording because it was invisible in normal use: no exception was raised, no test failed, and the API returned success each time.'),
  h2('5.3 Avoiding N+1 selects'),
  p('Loading a resume and then lazily reading its sections issues one query for the parent and one per child. Three repository methods declare an explicit LEFT JOIN FETCH so the parent and its collection are retrieved in a single statement. This is applied on the master-resume read paths; Section 12 notes where it has not yet been applied.'),
);

/* ---- 6. API ---- */
children.push(
  pageBreak(),
  h1('6. API Design'),
  p('Fourteen endpoints across two controllers. All paths are versioned under /api/v1. Every route except the authentication endpoints requires a bearer token.'),
  h2('6.1 Authentication'),
  table(
    ['Method', 'Path', 'Status', 'Description'],
    [
      ['POST', '/api/v1/auth/register', '201', 'Create an account; returns a JWT'],
      ['POST', '/api/v1/auth/login', '200', 'Exchange credentials for a JWT'],
      ['GET', '/api/v1/auth/me', '200', 'Current user profile'],
    ],
    [1, 4, 1, 3],
  ),
  p(''),
  h2('6.2 Resumes'),
  table(
    ['Method', 'Path', 'Status', 'Description'],
    [
      ['POST', '/api/v1/resumes/users/{userId}/master', '201', 'Create a master resume'],
      ['GET', '/api/v1/resumes/users/{userId}/master', '200', 'List a user’s master resumes'],
      ['PUT', '/api/v1/resumes/users/{userId}/master', '200', 'Upsert master resume content'],
      ['GET', '/api/v1/resumes/users/{userId}/master/first', '200 / 404', 'First master resume'],
      ['GET', '/api/v1/resumes/{resumeId}/with-sections', '200', 'Resume with all sections'],
      ['POST', '/api/v1/resumes/{masterResumeId}/tailor', '202', 'Queue a tailoring job'],
      ['GET', '/api/v1/resumes/tailored/{id}', '200', 'Tailored resume and ATS score'],
      ['GET', '/api/v1/resumes/users/{userId}/tailored', '200', 'All tailored resumes for a user'],
      ['POST', '/api/v1/resumes/tailored/{id}/retry', '202', 'Re-queue a FAILED job'],
      ['PUT', '/api/v1/resumes/tailored/{id}/sections', '200', 'Edit tailored sections'],
      ['POST', '/api/v1/resumes/tailored/{id}/chat', '200', 'Model-assisted revision'],
    ],
    [1, 5, 1, 3],
  ),
  p(''),
  h2('6.3 Status code semantics'),
  bullet('201 Created — returned when a resource is created, with the created representation in the body.'),
  bullet('202 Accepted — returned by the two asynchronous operations. The work has been accepted and durably recorded, but has not been performed. The client polls the corresponding GET endpoint until status leaves PENDING.'),
  bullet('403 Forbidden — returned when an authenticated caller requests a resource they do not own. Distinguished from 401, which indicates a missing or invalid token.'),
  h2('6.4 Validation and error handling'),
  p('Request bodies are annotated with Jakarta Bean Validation constraints and validated with @Valid at the controller boundary, so malformed input is rejected with 400 before reaching business logic. A @RestControllerAdvice translates exceptions into consistent JSON. Unexpected exceptions are logged in full with a generated correlation ID, but the client receives only that identifier — server.error.include-message is set to never, so stack traces and internal messages are not disclosed.'),
);

/* ---- 7. Async pipeline ---- */
children.push(
  pageBreak(),
  h1('7. The Asynchronous Tailoring Pipeline'),
  p('This is the core flow of the system and the reason for its structure.'),
  ...code([
    ' 1. Client   POST /api/v1/resumes/{id}/tailor',
    '',
    ' 2. resume-service',
    '      - assertOwnership(masterResume.userId)',
    '      - persist JobDescription',
    '      - persist TailoredResume  (status = PENDING)',
    '      - publish TailoringRequestedEvent -> Kafka',
    '      - respond 202 Accepted with the PENDING record',
    '',
    ' 3. worker-service  (consumes resume.tailoring.requested)',
    '      - isDuplicate(tailoredResumeId)?  -> Redis, 24h TTL',
    '            yes -> acknowledge and stop',
    '      - call Ollama with master content + job description',
    '      - validate model output  (TailoringGuardrailValidator)',
    '      - compute ATS score',
    '      - write sections + score, set status = COMPLETED',
    '      - markAsProcessed(tailoredResumeId)',
    '',
    ' 4. Client   GET /api/v1/resumes/tailored/{id}   (polls)',
    '                -> PENDING ... then COMPLETED + score',
  ]),
  h2('7.1 Idempotency'),
  p('Kafka guarantees at-least-once delivery. A consumer that processes an event, calls the model, and then crashes before committing its offset will receive that event again on restart. Without protection this produces a second model invocation and a duplicated result.'),
  p('Before doing any work the worker checks Redis for a key derived from the tailored-resume identifier, and writes that key after completing. The window is 24 hours, which comfortably exceeds any plausible redelivery interval while ensuring the keyspace does not grow without bound. Redis is the right store for this: the data is short-lived, keyed, and worthless after expiry.'),
  h2('7.2 Failure handling'),
  p('If the model call fails or returns unusable output, the worker marks the record FAILED rather than leaving it PENDING indefinitely, so the client can distinguish "still working" from "did not succeed". A FAILED record can be resubmitted through the retry endpoint, which resets the status to PENDING and republishes the event. Retry deliberately rejects records in any other state, so a COMPLETED result cannot be silently overwritten.'),
);

/* ---- 8. LLM ---- */
children.push(
  pageBreak(),
  h1('8. Language Model Integration and Guardrails'),
  p('A language model is a non-deterministic component reached over the network, and its output is untrusted input. Two layers of validation surround it.'),
  h2('8.1 Input sanitisation'),
  p('Before any user text reaches a prompt, InputSanitizer enforces size limits and screens for prompt-injection patterns — attempts to make the model disregard its instructions. Content is capped at 2,000 characters for a chat message, 4,000 per resume section, and 12,000 for total assembled context. The caps bound cost and latency and also remove the simplest denial-of-service vector, which is submitting an enormous document. Text matching an injection pattern is stripped; text consisting only of an injection attempt is rejected outright rather than forwarded.'),
  h2('8.2 Output validation'),
  p('Model output is validated by TailoringGuardrailValidator before it is persisted:'),
  bullet('Section keys must be drawn from a known allow-list, and the required sections must be present — the model cannot invent or omit structure.'),
  bullet('Each section must fall between 5 and 6,000 characters, which catches both empty and runaway generations.'),
  bullet('Output is screened for narration signals — conversational filler such as an explanation of what the model has done — which is not resume content and must not be persisted as if it were.'),
  bullet('Injection signals are screened again on the way out, since a model can reflect adversarial input back into its response.'),
  p('The rule applied throughout is that the model is treated as an untrusted content source whose output must satisfy a schema before it is written, rather than as a service whose responses can be assumed well-formed.'),
  h2('8.3 A behaviour worth flagging'),
  p('When the model is unavailable, the current worker writes a placeholder resume and marks the record COMPLETED. This is the wrong behaviour: a caller cannot distinguish a genuine tailoring result from a fallback. It should mark the record FAILED so the retry path applies. This was identified during review and is recorded in Section 12 rather than presented as working.'),
);

/* ---- 9. ATS scoring ---- */
children.push(
  h1('9. ATS Scoring'),
  p('Scoring is deliberately deterministic. Asking the model to rate its own output would make the score unexplainable and unreproducible, and would let the same component that produced the text also grade it. The score is computed in code from three weighted components:'),
  table(
    ['Component', 'Weight', 'What it measures'],
    [
      ['Keyword overlap', '50%', 'Proportion of significant terms from the job description that appear in the tailored resume, after stop-word removal'],
      ['Section completeness', '30%', 'Whether the expected resume sections are present'],
      ['Action verbs', '20%', 'Density of strong action verbs, a widely used resume-quality heuristic'],
    ],
    [3, 1, 6],
  ),
  p(''),
  p('The weighting reflects the problem being solved. Keyword overlap dominates because that is what an ATS filters on. Section completeness is next because a missing section can cause a parse failure regardless of content. Action verbs are the smallest term as a writing-quality signal rather than a filtering criterion. Each component is returned alongside the total, so a user can see which dimension is weak instead of receiving an unexplained number.'),
);

/* ---- 10. Security ---- */
children.push(
  pageBreak(),
  h1('10. Security'),
  h2('10.1 Authentication'),
  p('Passwords are hashed with BCrypt and never stored or logged in plaintext. Authentication issues a signed JWT parsed with jjwt 0.12.6 using verifyWith(...).parseSignedClaims(...), which requires a valid signature — unsigned tokens and the alg:none attack are rejected by the parser rather than by application code. Sessions are stateless, so no server-side session store exists to be compromised.'),
  h2('10.2 Authorization and BOLA'),
  p('Broken Object Level Authorization is the most common serious flaw in APIs of this kind: an endpoint authenticates the caller but never checks that the requested object belongs to them, so changing an identifier in the URL returns another user’s data. Every service method that touches user-owned data calls assertOwnership(), which compares the resource owner against the authenticated principal and raises 403 on mismatch.'),
  p('This is verified rather than asserted. BOLATest creates two real users with two real tokens and drives the HTTP API, confirming that each can reach their own data and that every cross-tenant attempt is refused.'),
  h2('10.3 An authorization gap found and closed'),
  p('Review of the chat endpoint found that it declared a tailoredResumeId path variable and then discarded it — the controller passed only the request body to the service, which performed no ownership check. Any authenticated user could therefore invoke the model against any identifier. The service now loads the tailored resume, compares its owner against the token principal, and returns 403 on mismatch, bringing it in line with every other data-touching method. A regression test drives the endpoint as one user against another user’s resume and asserts 403.'),
  h2('10.4 Rate limiting'),
  p('A sliding-window filter limits the authentication endpoints — ten login attempts and five registrations per minute per client — to blunt credential brute-forcing and automated account creation. Both are per-key windows over request timestamps rather than fixed buckets, so a caller cannot burst across a window boundary.'),
  h2('10.5 Injection'),
  p('All repository access in resume-service is parameterised JPQL. No query is assembled by concatenating user input, and there is no JdbcTemplate or EntityManager string building on the request path, so the SQL injection surface is closed by construction rather than by filtering.'),
  h2('10.6 Secrets'),
  p('No credential is committed. Every production value resolves from an environment variable and fails closed when unset — the database password has no working default, so a misconfigured deployment refuses to start rather than running with a known password. The repository history was scanned across all refs for key material and none was found. Section 12 records two configuration files that fall short of this standard.'),
  h2('10.7 Error disclosure'),
  p('Unhandled exceptions return a correlation identifier and nothing else. The full exception is written to the structured log where an operator can find it by that identifier, so diagnosability is preserved without exposing internals to a caller.'),
);

/* ---- 11. Testing ---- */
children.push(
  pageBreak(),
  h1('11. Testing Strategy and Results'),
  p('76 tests, all passing. The build is verified reproducibly: the committed tree is exported with git archive into a clean directory and built there, so the result reflects the repository rather than a developer machine’s local state.'),
  h2('11.1 Suites'),
  table(
    ['Suite', 'Tests', 'What it verifies'],
    [
      ['BOLATest', '11', 'Cross-tenant authorization over HTTP: own-data access succeeds; every cross-user read, write, list and chat attempt returns 403; missing and tampered tokens return 401'],
      ['AuthServiceTest', '13', 'Registration, duplicate-email conflict, email normalisation, BCrypt hashing, login failure paths, exact status codes'],
      ['RateLimitFilterTest', '7', 'Requests below the limit pass, the next is refused with 429, and limits are isolated per client'],
      ['ATSScorerTest + edge cases', '21', 'Scoring arithmetic, component weighting, and boundary conditions including empty and oversized input'],
      ['KeywordExtractorTest + edge cases', '24', 'Tokenisation, stop-word removal, punctuation and case handling'],
    ],
    [3, 1, 6],
  ),
  p(''),
  h2('11.2 Measured coverage'),
  p('Coverage is measured with JaCoCo rather than estimated. On resume-service it is 49.4% of lines and 52.2% of instructions. The figure is reported honestly, including where it is weak: worker-service and api-gateway have no test suite, so repository-wide coverage is substantially lower than the resume-service figure.'),
  p('One structural weakness is worth stating plainly. Forty-five of the tests exercise copies of the ATS scorer and keyword extractor that live in resume-service, but the code that actually runs during tailoring is the worker’s copy, and the two have already diverged — the worker uses a considerably larger stop-word list. Those tests are therefore describing behaviour that production does not execute. The correct remedy is to extract the scoring logic into a shared module tested once; Section 13 covers this.'),
  h2('11.3 Defects found by testing during this cycle'),
  p('Bringing the suite to green surfaced several genuine defects, which is the argument for having it:'),
  bullet('An invalid enum value in the test configuration was preventing the Spring context from loading, so 32 tests were erroring rather than running.'),
  bullet('BOLATest was exercising routes that do not exist on the controller, so it was returning 500 and testing nothing.'),
  bullet('The rate-limit suite shared one filter instance across tests, so each test inherited the previous test’s consumed budget.'),
  bullet('Thirty tests existed only as untracked files and would not have reached the repository at all.'),
  bullet('The missing orphanRemoval described in Section 5.2, and the chat authorization gap in Section 10.3.'),
);

/* ---- 12. Limitations ---- */
children.push(
  pageBreak(),
  h1('12. Known Limitations'),
  p('Recorded explicitly. Each is a real constraint of the delivered system, stated here rather than left for a reader to find.'),
  table(
    ['Limitation', 'Detail'],
    [
      ['PDF export is not wired', 'ResumePDFGenerator is implemented, but the consumer sets pdfPath to null because no object storage is provisioned, so no download URL is returned.'],
      ['The gateway is not on the request path', 'api-gateway routes /api/v1/resumes/** but has no /api/v1/auth/** route, so authentication cannot traverse it; the frontend calls resume-service directly.'],
      ['Circuit breaker is inert', 'The Resilience4j annotation sits on a private method invoked via self-invocation, so the Spring AOP proxy never applies it. It provides no protection despite being present.'],
      ['Kafka publish is a dual write', 'The event is published inside the database transaction. A broker failure after commit loses the event; the record remains PENDING and must be recovered through retry. A transactional outbox is the correct fix.'],
      ['No LLM client timeout', 'Neither model client sets a connect or read timeout, so an unresponsive endpoint can hold a thread indefinitely.'],
      ['Model-unavailable path is wrong', 'A failed model call currently yields a placeholder marked COMPLETED instead of FAILED (Section 8.3).'],
      ['Duplicated scoring logic', 'ATSScorer and KeywordExtractor exist in both services and have diverged; only the untested worker copy runs in production.'],
      ['Cross-service table writes', 'worker-service owns no JPA entities and writes to resume-service tables with native SQL, which bypasses the optimistic-locking version check.'],
      ['Hardcoded password in k8s manifests', 'Two Kubernetes manifests carry a literal database password, inconsistent with the environment-variable approach used everywhere else.'],
      ['Rate-limit key is spoofable', 'The limiter trusts an X-Real-IP header, which a client can set directly when no trusted proxy strips it.'],
      ['Kubernetes manifests are illustrative', 'No manifests exist for PostgreSQL, Kafka or Redis and no images are published, so the manifests are not deployable as they stand.'],
      ['Migrations are untested', 'Tests run against H2 with Flyway disabled and schema generated from entities, so the V1-V8 chain is never executed by the suite.'],
    ],
    [3, 7],
  ),
);

/* ---- 13. Challenges ---- */
children.push(
  pageBreak(),
  h1('13. Engineering Challenges and Decisions'),
  h2('13.1 Keeping model latency out of the request path'),
  p('The central design problem. Resolved with Kafka and a 202-plus-polling contract, described in Section 7. The cost is a more complex system and a client that must poll; the benefit is an API whose latency and availability do not depend on an external model’s.'),
  h2('13.2 Duplicate work under at-least-once delivery'),
  p('Kafka’s delivery guarantee makes redelivery normal rather than exceptional, and every redelivery would otherwise mean another model call. Resolved with a Redis idempotency key checked before work begins and written after it completes.'),
  h2('13.3 Treating model output as untrusted'),
  p('Early iterations persisted whatever the model returned, which produced conversational narration stored as resume content and occasional malformed structure. Resolved by validating output against an allow-list of section keys with length bounds and narration screening before anything is written.'),
  h2('13.4 A silent data defect'),
  p('The missing orphanRemoval described in Section 5.2 corrupted data on the most common write path while raising no error and failing no test. The lesson taken from it is that a successful API response is not evidence of a correct write, and that ORM cascade semantics — particularly for inverse collections — need to be verified against the database rather than assumed.'),
  h2('13.5 Removing a feature'),
  p('A payment integration was built and then deleted, across several commits that removed the gateway, its schema, and the plan-quota logic that depended on it. Removing a feature cleanly proved harder than adding one: the dependencies reached into the data model and into request handling, and the compiler only found some of them. It was the right call — billing added no backend concept the project needed to demonstrate — but it consumed real time.'),
  h2('13.6 Documentation drifting from code'),
  p('Several planning documents in the repository described work as outstanding that had in fact been completed, and in one case specified a controller that was never written. Left in place they would have actively misrepresented the state of the system to a reader. They were removed and the README rewritten against the actual code, with every claim checked — endpoint list, status codes, test count, coverage figure, and Java version. Documentation that contradicts the code is worse than no documentation.'),
);

/* ---- 14. Future work ---- */
children.push(
  h1('14. Future Work'),
  p('In priority order, judged by risk reduced per unit of effort:'),
  bullet('Extract scoring into a shared module so one tested implementation runs in production, eliminating the divergence described in Section 11.2.'),
  bullet('Add a transactional outbox so event publication cannot be lost relative to the database commit.'),
  bullet('Set connect and read timeouts on both model clients, and repair the circuit breaker by moving the annotation to a proxied method.'),
  bullet('Mark failed model calls FAILED rather than writing a placeholder marked COMPLETED.'),
  bullet('Add a test suite for worker-service, which contains the tailoring pipeline and is currently untested.'),
  bullet('Run the Flyway chain against a real PostgreSQL instance in tests, using Testcontainers, so migrations are verified.'),
  bullet('Route authentication through the gateway and move the frontend onto it, so the gateway is genuinely on the request path.'),
  bullet('Provision object storage and wire the existing PDF generator into the pipeline.'),
  bullet('Derive the rate-limit key from a trusted proxy header, and move the window into Redis so limits hold across instances.'),
);

/* ---- 15. Conclusion ---- */
children.push(
  h1('15. Conclusion'),
  p('ResumeForge implements an event-driven backend that decouples slow, unreliable language-model work from a synchronous API using a durable queue, an idempotent consumer, and a polling contract. The relational model is versioned and validated at startup, authorization is enforced at the service layer and verified by cross-tenant tests, and model output is treated as untrusted input subject to schema validation before persistence.'),
  p('The system carries 76 passing tests with measured rather than estimated coverage, and this report has been explicit about where that coverage is thin and where the implementation falls short of what a production deployment would require. Several of the defects documented here — a silent data-duplication bug, an unauthorized endpoint, an inert circuit breaker, and a test suite that was not running at all — were found by reviewing the system against its own claims rather than by its tests, which is itself the most useful result of the exercise.'),
);

/* ─────────────── build ─────────────── */

const doc = new Document({
  creator: 'Sai Kumaresh',
  title: 'ResumeForge — Capstone Project Report',
  description: 'Event-driven backend for AI resume tailoring and ATS scoring',
  numbering: {
    config: [{
      reference: 'bullets',
      levels: [
        { level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: convertInchesToTwip(0.3), hanging: convertInchesToTwip(0.18) } } } },
        { level: 1, format: LevelFormat.BULLET, text: '◦', alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: convertInchesToTwip(0.6), hanging: convertInchesToTwip(0.18) } } } },
      ],
    }],
  },
  styles: {
    default: { document: { run: { font: 'Calibri', size: 22 } } },
  },
  sections: [{
    properties: { page: { margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } } },
    footers: {
      default: new Footer({
        children: [new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [new TextRun({ children: ['ResumeForge — Capstone Report    |    ', PageNumber.CURRENT], size: 18, color: GREY })],
        })],
      }),
    },
    children,
  }],
});

Packer.toBuffer(doc).then((buf) => {
  const out = process.argv[2] || 'ResumeForge_Report.docx';
  fs.writeFileSync(out, buf);
  console.log('wrote ' + out + '  (' + Math.round(buf.length / 1024) + ' KB)');
});
