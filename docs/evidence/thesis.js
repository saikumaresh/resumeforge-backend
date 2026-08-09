/* ResumeForge — Applied Software Project Report
   Built to the Scaler Neovarsity / Woolf template:
     Times New Roman, 12pt body / 14pt headings, black
     Margins 1.25" left+right, 1" top+bottom
     1.5 line spacing in body, single for lists and references
     Chapter headings centred, subheadings left, body justified
     Figures captioned below, tables captioned above, numbered Chapter.NN
*/
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType, PageBreak,
  Table, TableRow, TableCell, WidthType, ShadingType, LevelFormat, ImageRun,
  TableOfContents, Footer, PageNumber, BorderStyle,
} = require('docx');
const fs = require('fs');
const path = require('path');

const FIG = path.join(__dirname, 'figures');

/* ── candidate details ─────────────────────────────────────────── */
const D = {
  name:        'Sai Kumaresh',
  email:       '⟨Registered Scaler Email ID⟩',
  supervisor:  'Naman Bhalla',
  monthYear:   '⟨Month, Year⟩',
  submitDate:  '⟨DD/MM/YYYY⟩',
  moduleStart: '⟨Module start date⟩',
  moduleEnd:   '⟨Module end date⟩',
};

/* ── typography ────────────────────────────────────────────────── */
const BODY = 24;        // 12pt (half-points)
const HEAD = 28;        // 14pt
const LINE15 = 360;     // 1.5 spacing
const LINE1 = 240;      // single
const SERIF = 'Times New Roman';

const t = (text, o = {}) => new TextRun({
  text, size: o.size ?? BODY, bold: o.bold, italics: o.italics,
  font: SERIF, color: '000000',
});

/** Justified body paragraph, 1.5 spacing. */
const p = (text, o = {}) => new Paragraph({
  alignment: o.align ?? AlignmentType.JUSTIFIED,
  spacing: { line: o.line ?? LINE15, after: o.after ?? 120 },
  indent: o.indent,
  children: [t(text, o)],
});

/** Chapter heading — centred, bold, 14pt. */
const chapter = (text) => new Paragraph({
  heading: HeadingLevel.HEADING_1,
  alignment: AlignmentType.CENTER,
  spacing: { before: 360, after: 220, line: LINE15 },
  children: [t(text, { size: HEAD, bold: true })],
});

/** Subheading — left, bold, 14pt. */
const sub = (text) => new Paragraph({
  heading: HeadingLevel.HEADING_2,
  alignment: AlignmentType.LEFT,
  spacing: { before: 280, after: 140, line: LINE15 },
  children: [t(text, { size: HEAD, bold: true })],
});

/** Third-level run-in heading. */
const sub3 = (text) => new Paragraph({
  heading: HeadingLevel.HEADING_3,
  alignment: AlignmentType.LEFT,
  spacing: { before: 200, after: 110, line: LINE15 },
  children: [t(text, { size: BODY, bold: true })],
});

/** Bulleted item — single spacing per the template's list rule. */
const li = (text, level = 0) => new Paragraph({
  numbering: { reference: 'bul', level },
  alignment: AlignmentType.JUSTIFIED,
  spacing: { line: LINE1, after: 70 },
  children: [t(text)],
});

/** Numbered item. */
const ni = (text, level = 0) => new Paragraph({
  numbering: { reference: 'num', level },
  alignment: AlignmentType.JUSTIFIED,
  spacing: { line: LINE1, after: 70 },
  children: [t(text)],
});

const blank = (n = 1) => Array.from({ length: n }, () =>
  new Paragraph({ spacing: { after: 0, line: LINE15 }, children: [t('')] }));

const pageBreak = () => new Paragraph({ children: [new PageBreak()] });

/* ── figures ───────────────────────────────────────────────────── */
function pngSize(file) {
  const b = fs.readFileSync(file);
  return { w: b.readUInt32BE(16), h: b.readUInt32BE(20), buf: b };
}

const MAX_W = 545; // px at 96dpi ≈ 5.68in, fits A4 with 1.25in side margins

/** Figure image + caption below (template: "Figure captions go below figures"). */
function figure(fileBase, num, caption) {
  const f = path.join(FIG, fileBase + '.png');
  const { w, h, buf } = pngSize(f);
  const scale = Math.min(1, MAX_W / w);
  return [
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 200, after: 80, line: LINE1 },
      children: [new ImageRun({
        type: 'png', data: buf,
        transformation: { width: Math.round(w * scale), height: Math.round(h * scale) },
      })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { after: 240, line: LINE1 },
      children: [t(`Figure ${num}: ${caption}`, { bold: true, size: 22 })],
    }),
  ];
}

/* ── tables ────────────────────────────────────────────────────── */
const TW = 8700; // twips; ≈5.7in usable width

/** Table caption above (template: "Table captions go above tables"). */
const tableCaption = (num, caption) => new Paragraph({
  alignment: AlignmentType.LEFT,
  spacing: { before: 220, after: 90, line: LINE1 },
  children: [t(`Table ${num}: ${caption}`, { bold: true, size: 22 })],
});

function table(headers, rows, weights) {
  const w = weights || headers.map(() => 1);
  const sum = w.reduce((a, b) => a + b, 0);
  const cols = w.map((x) => Math.round((x / sum) * TW));
  cols[cols.length - 1] = TW - cols.slice(0, -1).reduce((a, b) => a + b, 0);

  const cell = (text, i, head) => new TableCell({
    width: { size: cols[i], type: WidthType.DXA },
    shading: head ? { type: ShadingType.CLEAR, fill: 'D9D9D9' } : undefined,
    margins: { top: 70, bottom: 70, left: 110, right: 110 },
    children: [new Paragraph({
      spacing: { after: 0, line: LINE1 },
      alignment: AlignmentType.LEFT,
      children: [t(String(text), { bold: head, size: 22 })],
    })],
  });

  return new Table({
    width: { size: TW, type: WidthType.DXA },
    columnWidths: cols,
    rows: [
      new TableRow({ tableHeader: true, children: headers.map((h, i) => cell(h, i, true)) }),
      ...rows.map((r) => new TableRow({ children: r.map((c, i) => cell(c, i, false)) })),
    ],
  });
}

const spacerAfterTable = () => new Paragraph({ spacing: { after: 220, line: LINE1 }, children: [t('')] });

/* ═══════════════════════════════════════════════════════════════
   FRONT MATTER
   ═══════════════════════════════════════════════════════════════ */
const front = [];

const ctr = (text, o = {}) => new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { after: o.after ?? 140, line: LINE15 },
  children: [t(text, o)],
});

front.push(
  ...blank(3),
  ctr('Applied Software Project Report', { size: 36, bold: true, after: 400 }),
  ctr('By', { after: 300 }),
  ctr(D.name, { size: 30, bold: true, after: 500 }),
  ctr('A Master’s Project Report submitted to Scaler Neovarsity - Woolf in partial fulfillment of the requirements for the degree of Master of Science in Computer Science', { after: 500 }),
  ctr(D.monthYear, { bold: true, after: 600 }),
  ctr(`Scaler Mentee Email ID : ${D.email}`),
  ctr(`Thesis Supervisor : ${D.supervisor}`),
  ctr(`Date of Submission : ${D.submitDate}`),
  pageBreak(),
);

/* Certification */
front.push(
  chapter('Certification'),
  ...blank(1),
  p('I confirm that I have overseen / reviewed this applied project and, in my judgment, it adheres to the appropriate standards of academic presentation. I believe it satisfactorily meets the criteria, in terms of both quality and breadth, to serve as an applied project report for the attainment of Master of Science in Computer Science degree. This applied project report has been submitted to Woolf and is deemed sufficient to fulfill the prerequisites for the Master of Science in Computer Science degree.'),
  ...blank(6),
  ctr(D.supervisor, { bold: true, after: 60 }),
  ctr('…………………', { after: 60 }),
  ctr('Project Guide / Supervisor'),
  pageBreak(),
);

/* Declaration — reproduced verbatim from the template */
front.push(
  chapter('DECLARATION'),
  ...blank(1),
  p(`I confirm that this project report, submitted to fulfill the requirements for the Master of Science in Computer Science degree, completed by me from ${D.moduleStart} to ${D.moduleEnd}, is the result of my own individual endeavor. The Project has been made on my own under the guidance of my supervisor with proper acknowledgement and without plagiarism. Any contributions from external sources or individuals, including the use of AI tools, are appropriately acknowledged through citation. By making this declaration, I acknowledge that any violation of this statement constitutes academic misconduct. I understand that such misconduct may lead to expulsion from the program and/or disqualification from receiving the degree.`),
  ...blank(7),
  ctr(D.name, { bold: true, after: 400 }),
  new Paragraph({
    alignment: AlignmentType.LEFT,
    spacing: { line: LINE15 },
    children: [t('Signature of the Candidate: ……………………………                    Date: ……………………')],
  }),
  pageBreak(),
);

/* Acknowledgment */
front.push(
  chapter('ACKNOWLEDGMENT'),
  ...blank(1),
  p('I would like to express my sincere gratitude to everyone who supported me through this program and the completion of this applied project.'),
  p(`I am grateful to my thesis supervisor, ${D.supervisor}, for his guidance and review of this project, and for the direction he provided at the points where the design decisions were least obvious.`),
  p('I thank the instructors and mentors at Scaler Neovarsity for the depth of the curriculum, particularly the modules on distributed systems, database design and system architecture, which shaped the approach taken in this project. The habit of asking why a component belongs in a design, rather than whether it can be added to one, came directly from that instruction.'),
  p('I am thankful to my peers in the program for the discussions and reviews that improved this work, and for the reminder that a design is only as good as one\'s ability to explain it to somebody else.'),
  p('Finally, I thank my family for their patience and encouragement throughout the duration of this program. Completing a Master\'s degree alongside professional work is only possible with that support, and I am grateful for it.'),
  pageBreak(),
);

/* Table of contents */
front.push(
  chapter('Table of Contents'),
  new TableOfContents('Contents', { hyperlink: true, headingStyleRange: '1-2' }),
  pageBreak(),
);

/* List of Tables */
front.push(
  chapter('List of Tables'),
  p('(To be written sequentially as they appear in the text)', { align: AlignmentType.LEFT, italics: true }),
  ...blank(1),
  table(
    ['Table No.', 'Title', 'Page No.'],
    [
      ['3.01', 'Functional requirements', ''],
      ['3.02', 'Non-functional requirements', ''],
      ['3.03', 'Feature set', ''],
      ['3.04', 'REST API surface', ''],
      ['4.01', 'Design patterns used in the low-level design', ''],
      ['5.01', 'Database tables and their purpose', ''],
      ['5.02', 'Foreign key constraints', ''],
      ['5.03', 'Cardinality of relations', ''],
      ['6.01', 'N+1 optimisation benchmark results', ''],
      ['8.02', 'Automated test suites', ''],
      ['8.03', 'Technology selection and rationale', ''],
      ['9.01', 'Known limitations of the delivered system', ''],
    ],
    [1.2, 5, 1.2],
  ),
  pageBreak(),
);

/* List of Figures */
front.push(
  chapter('List of Figures'),
  p('(List of Images, Graphs, Charts sequentially as they appear in the text)', { align: AlignmentType.LEFT, italics: true }),
  ...blank(1),
  table(
    ['Figure No.', 'Title', 'Page No.'],
    [
      ['2.01', 'ResumeForge system architecture', ''],
      ['2.02', 'Tailoring job state lifecycle', ''],
      ['3.01', 'Use case diagram', ''],
      ['4.01', 'Domain model class diagram', ''],
      ['4.02', 'Layered class diagram of the request path', ''],
      ['5.01', 'Entity relationship diagram', ''],
      ['6.01', 'Sequence diagram for the tailoring request', ''],
      ['6.02', 'N+1 optimisation: queries and latency, before and after', ''],
      ['7.01', 'Target AWS deployment architecture', ''],
    ],
    [1.2, 5, 1.2],
  ),
  pageBreak(),
);

/* ═══════════════════════════════════════════════════════════════
   BODY
   ═══════════════════════════════════════════════════════════════ */
const body = [];

body.push(chapter('Applied Software Project'), ...blank(1));

/* ── 1. Abstract ──────────────────────────────────────────────── */
body.push(
  chapter('Abstract'),
  p('ResumeForge is an event-driven backend system that adapts a candidate’s master resume to a specific job description using a large language model, and then scores the adapted document for the keyword coverage that applicant tracking systems measure. The problem it addresses is a practical one: most job applications are filtered by software before a person reads them, and that software matches on vocabulary rather than on substance, so a well-qualified candidate is routinely rejected for using different words from the posting.'),
  p('The system is built as three Spring Boot services on Java 21. Because language model inference takes tens of seconds and fails unpredictably, the tailoring workload is deliberately separated from the HTTP request cycle. The API validates and authorises a request, persists a record in a PENDING state, publishes an event to Apache Kafka and returns 202 Accepted immediately. A worker service consumes that event, invokes the model, validates the returned content against a schema, computes the score deterministically in code, and writes the result back. Persistence is PostgreSQL with Flyway-managed migrations; Redis provides consumer idempotency so that Kafka’s at-least-once delivery cannot cause a duplicate model invocation.'),
  p('The results are a working system of fourteen REST endpoints with stateless JWT authentication and object-level authorisation enforced at the service layer, verified by an automated cross-tenant test suite. The project carries 94 automated tests with JaCoCo-measured coverage of 64.6% of lines on the primary service. A measured optimisation of the resume listing path reduced the query count from 31 to 1 and median latency from 3.487 ms to 1.126 ms.'),
  p('The wider applicability is that the pattern demonstrated here — treating a slow, non-deterministic external model as an unreliable asynchronous dependency behind a durable queue, with schema validation on its output — generalises to any system integrating generative AI into a transactional application, which is now a common requirement across recruitment technology, document processing, customer support and healthcare administration.'),
  pageBreak(),
);

/* ── 2. Project Description ───────────────────────────────────── */
body.push(
  chapter('Project Description'),
  p('ResumeForge takes one stored master resume and one job description, and produces a rewritten resume targeted at that posting together with a numeric assessment of how well the rewrite matches it. The candidate maintains a single structured resume; the system produces a tailored variant per application.'),

  sub('2.1 Objectives'),
  ni('Allow a candidate to store one structured master resume, divided into typed sections such as summary, experience, skills and education.'),
  ni('Accept a job description and produce a tailored variant of the master resume against it, using a large language model to perform the rewriting.'),
  ni('Compute a deterministic ATS score for the tailored output, broken down by component so the candidate can see which dimension is weak.'),
  ni('Keep the API responsive regardless of how long the model takes, so that model latency and model failure are never visible to the caller as request latency or request failure.'),
  ni('Guarantee that a candidate can access only their own data, and prove that guarantee with automated tests rather than asserting it.'),
  ni('Allow manual editing of the tailored output and conversational revision through the model, and allow a failed job to be retried.'),

  sub('2.2 Relevance'),
  p('Applicant tracking systems are used by the large majority of medium and large employers to screen applications before human review. These systems parse an uploaded document and rank it against the posting, and the ranking is driven substantially by term overlap. The practical consequence is that a candidate must restate the same experience in the vocabulary of each posting. Done manually this is slow, repetitive, and easy to do badly, because the candidate is guessing at which terms the filter weights.'),
  p('Automating the rewrite addresses the labour, and scoring the result addresses the guesswork. Because the score is computed in code from an explicit formula rather than asked of the model, it is reproducible and explainable — a candidate can be told that their keyword coverage is weak while their section completeness is fine, and can act on that.'),

  sub('2.3 System architecture'),
  p('The system is organised as three services within a single Maven reactor. The division follows the runtime characteristics of the work rather than a domain boundary: one service handles fast, synchronous, transactional HTTP traffic, and a second handles slow, retryable work against an unreliable external dependency.'),
  ...figure('fig_2_01_architecture', '2.01', 'ResumeForge system architecture'),
  p('The resume service owns the database and all write paths. It authenticates the caller, authorises the operation against resource ownership, persists state, and publishes events. The worker service owns the integration with the language model; it consumes events, calls the model, validates what comes back, computes scores and writes results. The API gateway is a Spring Cloud Gateway instance which proxies the resume routes; as Section 9 records honestly, it is not currently on the primary request path.'),

  sub('2.4 Development process'),
  p('The project was developed incrementally, with each capability brought to a working state before the next was started. The initial milestone was the three-service scaffold with a shared parent POM and containerised infrastructure. The domain model and Flyway migrations followed, then the synchronous REST surface, then the Kafka producer and consumer, then the model integration, then authentication and authorisation, and finally the output guardrails and idempotency layer. Testing was retrofitted in a dedicated phase rather than written alongside, which in retrospect was the weakest process decision made and is discussed in Section 9.'),

  sub('2.5 Tailoring job lifecycle'),
  p('Because tailoring is asynchronous, a tailored resume is a stateful record rather than a synchronous return value. Its state machine is small and is the contract between the API and its clients.'),
  ...figure('fig_2_02_lifecycle', '2.02', 'Tailoring job state lifecycle'),
  p('A record is created in PENDING at the moment the request is accepted. The worker moves it to PROCESSING when it begins, and to COMPLETED once the model output has passed validation and the score has been written. If the model call fails or returns content that cannot be validated, the record moves to FAILED rather than remaining PENDING indefinitely — the distinction matters because a client polling the record must be able to tell "still working" from "did not succeed". A FAILED record can be resubmitted through the retry endpoint, which resets it to PENDING and republishes the event. Retry deliberately rejects records in any other state so that a completed result cannot be silently overwritten.'),
  pageBreak(),
);

/* ── 3. Requirement Gathering ─────────────────────────────────── */
body.push(
  chapter('Requirement Gathering'),
  p('The requirements below were derived from the problem statement in Section 2 and refined during development as constraints emerged — in particular the discovery that model latency made a synchronous design untenable, which turned an implementation detail into the non-functional requirement NFR-01.'),

  sub('3.1 Functional requirements'),
  tableCaption('3.01', 'Functional requirements'),
  table(
    ['ID', 'Requirement', 'Priority'],
    [
      ['FR-01', 'A visitor can register with an email address and password, and receives an authentication token on success.', 'Must'],
      ['FR-02', 'A registered user can authenticate and receive a token valid for subsequent requests.', 'Must'],
      ['FR-03', 'A user can create and store a master resume divided into typed sections.', 'Must'],
      ['FR-04', 'A user can retrieve their master resumes, individually and as a list.', 'Must'],
      ['FR-05', 'A user can update the content of their master resume.', 'Must'],
      ['FR-06', 'A user can submit a job description and request that a master resume be tailored to it.', 'Must'],
      ['FR-07', 'The system rewrites the resume against the job description using a language model.', 'Must'],
      ['FR-08', 'The system computes an ATS score for the tailored output with a per-component breakdown.', 'Must'],
      ['FR-09', 'A user can retrieve a tailored resume together with its score and current status.', 'Must'],
      ['FR-10', 'A user can list all tailored resumes they own.', 'Should'],
      ['FR-11', 'A user can manually edit the sections of a tailored resume.', 'Should'],
      ['FR-12', 'A user can converse with the model to revise a tailored resume.', 'Should'],
      ['FR-13', 'A user can retry a tailoring job that failed.', 'Should'],
      ['FR-14', 'A user can access only resources they own; any other access is refused.', 'Must'],
    ],
    [1, 7, 1.2],
  ),
  spacerAfterTable(),

  sub('3.2 Non-functional requirements'),
  tableCaption('3.02', 'Non-functional requirements'),
  table(
    ['ID', 'Category', 'Requirement'],
    [
      ['NFR-01', 'Responsiveness', 'The API must return within normal web latency regardless of model response time. Tailoring must therefore be asynchronous.'],
      ['NFR-02', 'Durability', 'An accepted tailoring request must survive a worker crash and be reprocessed rather than lost.'],
      ['NFR-03', 'Idempotency', 'Redelivery of the same event must not cause a second model invocation or a duplicated result.'],
      ['NFR-04', 'Security — authentication', 'All resource endpoints require a validly signed token. Unsigned and tampered tokens are rejected.'],
      ['NFR-05', 'Security — authorisation', 'Every data-touching operation verifies resource ownership against the authenticated principal.'],
      ['NFR-06', 'Security — credentials', 'Passwords are stored only as BCrypt hashes. No credential is committed to the repository.'],
      ['NFR-07', 'Abuse resistance', 'Authentication endpoints are rate limited to blunt brute-force and automated registration.'],
      ['NFR-08', 'Data integrity', 'The schema is owned by versioned migrations and validated at startup; concurrent edits are detected.'],
      ['NFR-09', 'Observability', 'Requests are traceable through structured logs with correlation identifiers, and key operations are counted.'],
      ['NFR-10', 'Safety of model output', 'Model output is validated against a schema before persistence and is never trusted as well-formed.'],
    ],
    [1.2, 2, 6],
  ),
  spacerAfterTable(),

  sub('3.3 Users and use cases'),
  p('The system has one human actor and one system actor. The Job Seeker is the authenticated end user who owns resumes and requests tailoring. The Worker Service is a system actor that participates in the tailoring and retry use cases asynchronously; it is modelled as an actor rather than as internal behaviour because it executes outside the user’s request and its participation is what makes the tailoring use case complete.'),
  ...figure('fig_3_01_usecase', '3.01', 'Use case diagram'),

  sub3('Primary use case: Request tailoring'),
  p('Actor: Job Seeker. Precondition: the user is authenticated and owns at least one master resume. Main flow: the user submits a job description against a master resume; the system verifies ownership, persists the job description and a tailored-resume record in PENDING, publishes an event and returns 202 Accepted. The worker consumes the event, invokes the model, validates the output, computes the score and sets the record to COMPLETED. The user polls until the status changes. Alternative flow: if the model call fails or its output fails validation, the record is set to FAILED and the user may invoke the retry use case. Exception flow: if the user does not own the master resume, the request is refused with 403 and nothing is persisted.'),

  sub('3.4 Feature set'),
  tableCaption('3.03', 'Feature set'),
  table(
    ['Feature', 'Description', 'Status'],
    [
      ['Account management', 'Registration, login, profile retrieval, BCrypt password storage', 'Delivered'],
      ['Master resume management', 'Create, list, retrieve with sections, upsert content', 'Delivered'],
      ['Asynchronous tailoring', 'Kafka-backed job submission returning 202 with a PENDING record', 'Delivered'],
      ['LLM integration', 'Ollama chat-completions client with prompt construction', 'Delivered'],
      ['Input sanitisation', 'Size caps and prompt-injection screening before the model is called', 'Delivered'],
      ['Output validation', 'Section allow-list, length bounds and narration screening on model output', 'Delivered'],
      ['ATS scoring', 'Deterministic weighted score with per-component breakdown', 'Delivered'],
      ['Consumer idempotency', 'Redis-keyed duplicate suppression with a 24-hour window', 'Delivered'],
      ['Manual section editing', 'Direct edit of tailored sections with length validation', 'Delivered'],
      ['Conversational revision', 'Chat endpoint for model-assisted editing, authorised on the resume id', 'Delivered'],
      ['Retry of failed jobs', 'Resubmission of FAILED records only', 'Delivered'],
      ['Object-level authorisation', 'Ownership check on every data-touching operation', 'Delivered'],
      ['Rate limiting', 'Sliding window on login and registration', 'Delivered'],
      ['PDF export', 'Document generation implemented but not wired — no object storage provisioned', 'Partial'],
      ['API gateway routing', 'Route present for resume paths; authentication route absent', 'Partial'],
    ],
    [2.2, 6, 1.2],
  ),
  spacerAfterTable(),

  sub('3.5 API surface'),
  p('The functional requirements resolve to fourteen endpoints across two controllers. All are versioned under /api/v1 and all except the authentication routes require a bearer token.'),
  tableCaption('3.04', 'REST API surface'),
  table(
    ['Method', 'Path', 'Success', 'Requirement'],
    [
      ['POST', '/api/v1/auth/register', '201', 'FR-01'],
      ['POST', '/api/v1/auth/login', '200', 'FR-02'],
      ['GET', '/api/v1/auth/me', '200', 'FR-02'],
      ['POST', '/api/v1/resumes/users/{userId}/master', '201', 'FR-03'],
      ['GET', '/api/v1/resumes/users/{userId}/master', '200', 'FR-04'],
      ['PUT', '/api/v1/resumes/users/{userId}/master', '200', 'FR-05'],
      ['GET', '/api/v1/resumes/users/{userId}/master/first', '200 / 404', 'FR-04'],
      ['GET', '/api/v1/resumes/{resumeId}/with-sections', '200', 'FR-04'],
      ['POST', '/api/v1/resumes/{masterResumeId}/tailor', '202', 'FR-06'],
      ['GET', '/api/v1/resumes/tailored/{id}', '200', 'FR-09'],
      ['GET', '/api/v1/resumes/users/{userId}/tailored', '200', 'FR-10'],
      ['PUT', '/api/v1/resumes/tailored/{id}/sections', '200', 'FR-11'],
      ['POST', '/api/v1/resumes/tailored/{id}/chat', '200', 'FR-12'],
      ['POST', '/api/v1/resumes/tailored/{id}/retry', '202', 'FR-13'],
    ],
    [1.2, 5.5, 1.4, 1.3],
  ),
  spacerAfterTable(),
  p('The two operations that return 202 Accepted are the asynchronous ones. The status code is chosen deliberately: 200 would imply the work was performed, and 201 would imply a completed resource. 202 states accurately that the request has been accepted and durably recorded but not yet carried out.'),
  pageBreak(),
);

/* ── 4. Class Diagrams ────────────────────────────────────────── */
body.push(
  chapter('Class Diagrams'),
  p('This chapter presents the low-level design: the domain entities that model the problem, and the collaborating classes that carry a request from the HTTP boundary to the database.'),

  sub('4.1 Domain model'),
  p('Seven JPA entities model the domain. The central relationship is that a master resume is the source document, a job description is the target, and a tailored resume is the product of the two, carrying its own sections and exactly one score result.'),
  ...figure('fig_4_01_domain', '4.01', 'Domain model class diagram'),
  p('Two design decisions in this model are worth drawing out. First, MasterResume carries a version field mapped with @Version, which enables optimistic locking; a resume can be edited by the user while simultaneously being read by a tailoring job, and a write against a stale version fails rather than silently discarding a concurrent edit. Second, both section collections are composition rather than association — a section has no meaning independent of its parent resume, is created and deleted with it, and is therefore mapped with cascade and orphan removal.'),
  p('The relationship between TailoredResume and ATSScoreResult is one-to-one because a score describes exactly one tailored document and has no independent identity. Modelling it as a separate entity rather than as columns on the tailored resume keeps the scoring concern separable, so a future change to the scoring algorithm can add columns without widening the primary table.'),

  sub('4.2 Request path'),
  p('The layered view below shows the classes that participate in a request. Each layer has one responsibility and the dependencies point in one direction.'),
  ...figure('fig_4_02_layers', '4.02', 'Layered class diagram of the request path'),
  p('The controller deals only with HTTP: routing, deserialisation, bean validation and status code selection. It contains no business logic and no authorisation decision. The service holds the business rules, the transaction boundary and the ownership check. The repository is a Spring Data interface whose custom queries are JPQL with bound parameters.'),

  sub3('Why authorisation lives in the service layer'),
  p('The placement of assertOwnership in the service rather than in a filter or a controller is a deliberate design decision and the most important one in this diagram. A servlet filter can only inspect the URL and the token; it cannot determine whether the opaque UUID in the path belongs to the caller without loading the resource, which is the service’s job. Placing the check in the controller would work, but it would mean that any future caller of the same service method — a scheduled task, a second controller, a message handler — would bypass it silently. Enforcing it at the service boundary makes the check unavoidable: there is no path to the data that does not pass through it.'),
  p('This design was validated during the project. A review found that the conversational endpoint declared a tailored-resume path variable and then discarded it, calling a service that performed no ownership check at all. The gap existed precisely because that one service had been written without the convention the rest of the codebase followed. It was closed by adding the same check, and a regression test now drives the endpoint as one user against another user’s resume and asserts that it is refused.'),

  sub('4.3 Supporting components'),
  li('JwtAuthFilter — a OncePerRequestFilter that extracts and verifies the bearer token and populates the security context with the user identifier as the principal.'),
  li('RateLimitFilter — a sliding-window limiter keyed per client and per protected path, holding request timestamps in a bounded deque.'),
  li('TailoringProducer — wraps KafkaTemplate and publishes the tailoring event, isolating the rest of the service from the messaging API.'),
  li('InputSanitizer — enforces size caps and screens for prompt-injection patterns before any user text reaches a prompt.'),
  li('TailoringGuardrailValidator — validates model output against the expected section schema before it is persisted.'),
  li('IdempotencyService — records processed event identifiers in Redis under a bounded time-to-live.'),
  li('GlobalExceptionHandler — a @RestControllerAdvice translating exceptions into consistent JSON responses carrying a correlation identifier.'),

  sub('4.4 Design patterns applied'),
  p('The low-level design uses several established patterns. They are named here because the choice of pattern, and the reason for it, is part of the design rather than an accident of the framework.'),
  tableCaption('4.01', 'Design patterns used in the low-level design'),
  table(
    ['Pattern', 'Where it is applied', 'Why'],
    [
      ['Repository', 'MasterResumeRepository, TailoredResumeRepository, JobDescriptionRepository', 'Separates the domain from persistence, so the service layer expresses business rules without embedding query mechanics. It is also what makes the service unit-testable, because a repository interface is trivial to substitute with a test double.'],
      ['Data Transfer Object', 'CreateMasterResumeRequest, TailoredResumeResponse and the other DTOs', 'Decouples the wire contract from the entity model, so the schema can change without altering the API and internal fields are never exposed accidentally.'],
      ['Producer–Consumer', 'TailoringProducer and TailoringConsumer across the Kafka topic', 'Decouples the two services in time as well as in code; the producer has no reference to the consumer and does not wait for it.'],
      ['Chain of Responsibility', 'The Spring Security filter chain — RateLimitFilter then JwtAuthFilter', 'Each filter handles one concern and either rejects the request or passes it on, so cross-cutting checks compose without any one filter knowing about the others.'],
      ['Proxy', 'Resilience4j circuit breaker and retry on OllamaApiCaller', 'The resilience policy wraps the call without the calling code knowing. This pattern is also the reason the annotation must sit on a public method of a separate bean, as Section 9.1 discusses.'],
      ['Facade', 'OllamaClient over OllamaApiCaller', 'Presents a single tailorResume operation while hiding prompt construction, truncation, the HTTP exchange and response parsing behind it.'],
      ['Guard Clause', 'assertOwnership at the head of every data-touching service method', 'Authorisation failures exit immediately and before any state change, which is what makes the check fail closed.'],
      ['Optimistic Offline Lock', 'The @Version column on MasterResume', 'Detects a concurrent modification at write time instead of holding a database lock across a user editing session.'],
    ],
    [2, 4, 6],
  ),
  spacerAfterTable(),
  p('Two patterns that were considered and not used are worth recording. A transactional outbox would remove the dual write described in Section 9.3, but it adds a table and a relay process, and the retry endpoint already provides a manual recovery path for the failure it guards against. A Strategy for the scoring algorithm was rejected because there is only one scoring implementation; introducing an interface for a single implementor would add indirection without adding capability.'),
  pageBreak(),
);

/* ── 5. Database Schema Design ────────────────────────────────── */
body.push(
  chapter('Database Schema Design'),
  p('The schema is owned by Flyway migrations V1 to V8 and is validated against the entity mapping at application startup, so a divergence between code and schema fails fast rather than silently corrupting data. Hibernate is configured with ddl-auto set to validate; it never alters the schema.'),

  sub('5.1 Schema described textually'),
  p('Tables, with primary keys and the columns that carry meaning:'),

  sub3('users'),
  li('id — uuid, Primary Key'),
  li('email — varchar, unique'),
  li('password_hash — varchar, BCrypt output'),
  li('name — varchar'),
  li('plan — varchar'),
  li('email_verified — boolean'),
  li('created_at — timestamp'),

  sub3('master_resumes'),
  li('id — uuid, Primary Key'),
  li('user_id — uuid, Foreign Key to users(id)'),
  li('title — varchar'),
  li('summary — text'),
  li('version — integer, optimistic lock counter'),
  li('created_at, updated_at — timestamp'),

  sub3('master_resume_sections'),
  li('id — uuid, Primary Key'),
  li('master_resume_id — uuid, Foreign Key to master_resumes(id), NOT NULL'),
  li('section_type — enum (SUMMARY, EXPERIENCE, EDUCATION, SKILLS, PROJECTS, OTHER)'),
  li('content — text'),
  li('position — integer, ordering within the resume'),

  sub3('job_descriptions'),
  li('id — uuid, Primary Key'),
  li('user_id — uuid, Foreign Key to users(id)'),
  li('company_name, job_title — varchar'),
  li('description — text'),
  li('required_skills — text'),

  sub3('tailored_resumes'),
  li('id — uuid, Primary Key'),
  li('master_resume_id — uuid, Foreign Key to master_resumes(id)'),
  li('job_description_id — uuid, Foreign Key to job_descriptions(id)'),
  li('status — enum (PENDING, PROCESSING, COMPLETED, FAILED)'),
  li('pdf_path — varchar, nullable'),
  li('version — integer'),
  li('created_at — timestamp'),

  sub3('tailored_resume_sections'),
  li('id — uuid, Primary Key'),
  li('tailored_resume_id — uuid, Foreign Key to tailored_resumes(id)'),
  li('section_type — enum'),
  li('content — text'),

  sub3('ats_score_results'),
  li('id — uuid, Primary Key'),
  li('tailored_resume_id — uuid, Foreign Key to tailored_resumes(id), unique'),
  li('total_score — integer'),
  li('keyword_score, section_score, action_verb_score — integer'),

  tableCaption('5.01', 'Database tables and their purpose'),
  table(
    ['Table', 'Purpose'],
    [
      ['users', 'Account records and hashed credentials'],
      ['master_resumes', 'The candidate’s source resume, version-controlled for concurrent edits'],
      ['master_resume_sections', 'Typed, ordered sections composing a master resume'],
      ['job_descriptions', 'Target postings supplied by the candidate'],
      ['tailored_resumes', 'One rewrite of a master resume against one posting, with lifecycle status'],
      ['tailored_resume_sections', 'Rewritten section content produced by the model'],
      ['ats_score_results', 'Score breakdown for exactly one tailored resume'],
    ],
    [2.5, 7],
  ),
  spacerAfterTable(),

  tableCaption('5.02', 'Foreign key constraints'),
  table(
    ['Child table (column)', 'References'],
    [
      ['master_resumes(user_id)', 'users(id)'],
      ['master_resume_sections(master_resume_id)', 'master_resumes(id)'],
      ['job_descriptions(user_id)', 'users(id)'],
      ['tailored_resumes(master_resume_id)', 'master_resumes(id)'],
      ['tailored_resumes(job_description_id)', 'job_descriptions(id)'],
      ['tailored_resume_sections(tailored_resume_id)', 'tailored_resumes(id)'],
      ['ats_score_results(tailored_resume_id)', 'tailored_resumes(id)'],
    ],
    [5.5, 4],
  ),
  spacerAfterTable(),

  tableCaption('5.03', 'Cardinality of relations'),
  table(
    ['Relation', 'Cardinality'],
    [
      ['users to master_resumes', '1 : m'],
      ['master_resumes to master_resume_sections', '1 : m'],
      ['users to job_descriptions', '1 : m'],
      ['master_resumes to tailored_resumes', '1 : m'],
      ['job_descriptions to tailored_resumes', '1 : m'],
      ['tailored_resumes to tailored_resume_sections', '1 : m'],
      ['tailored_resumes to ats_score_results', '1 : 1'],
    ],
    [6, 3],
  ),
  spacerAfterTable(),

  sub('5.2 Schema described diagrammatically'),
  ...figure('fig_5_01_er', '5.01', 'Entity relationship diagram'),

  sub('5.3 Design decisions'),
  sub3('UUID primary keys'),
  p('Every table uses a UUID primary key rather than an auto-incrementing integer. The reason is that identifiers appear in URLs. A sequential integer key would let any authenticated user enumerate the identifier space and discover how many records exist and whether a particular one does — information disclosure even where the authorisation check correctly refuses access. A UUID is not itself an access control, and the ownership check remains the actual protection, but it removes enumeration as a reconnaissance technique.'),

  sub3('Optimistic locking'),
  p('The master resume carries a version column. The editing interface saves automatically while the user types, and a tailoring job reads the same row; two writes can therefore overlap. Optimistic locking is preferable to pessimistic locking here because contention is rare and holding a row lock for the duration of an editing session would be far more disruptive than occasionally asking a client to retry a save.'),

  sub3('A cascade defect discovered during review'),
  p('The upsert path clears the section collection and adds a replacement. The association was mapped with cascade = ALL but without orphanRemoval. For an inverse collection — one mapped with mappedBy, where the foreign key is owned by the child — Hibernate performs no collection-level DML. Without orphanRemoval the cleared children are never marked for deletion, and because the child’s foreign key column is NOT NULL, Hibernate cannot detach them either. The observable effect was that every save appended a duplicate section row rather than replacing the existing one, on the most frequently exercised write path in the application.'),
  p('The defect is worth recording because of how it presented. No exception was raised, no test failed, and the API returned a success response every time. It was found by reasoning about cascade semantics rather than by observing a failure. The correction was to add orphanRemoval to the association, and the general lesson taken from it is that a successful API response is not evidence of a correct write.'),

  sub3('Query design and the N+1 problem'),
  p('Loading a resume and subsequently reading its sections issues one query for the parent and one for each child collection — the N+1 select problem. Three repository methods declare an explicit LEFT JOIN FETCH so that a parent and its collection are retrieved in a single statement. The measured effect of this is presented as a benchmark in Section 6.'),
  pageBreak(),
);

/* ── 6. Feature Development Process ───────────────────────────── */
body.push(
  chapter('Feature Development Process'),
  p('This chapter follows one feature end to end: asynchronous resume tailoring, the central capability of the system. It covers the request flow through the application, the design decisions taken during implementation, and a measured performance optimisation with benchmarks before and after.'),

  sub('6.1 The feature'),
  p('The feature is invoked by POST /api/v1/resumes/{masterResumeId}/tailor. The user supplies a job description; the system returns a tailored resume and an ATS score. Because the rewriting is performed by a language model, the work cannot complete within the request.'),

  sub('6.2 API request payload'),
  p('The request body is a TailorResumeRequest, validated at the controller boundary with Jakarta Bean Validation:'),
  li('companyName — string, required'),
  li('jobTitle — string, required'),
  li('jobDescription — string, required, the full posting text'),
  li('requiredSkills — string, optional'),
  p('The path variable masterResumeId identifies the source resume. A request failing validation is rejected with 400 before any business logic executes; a request for a resume the caller does not own is rejected with 403 and nothing is persisted.'),
  p('The response is a TailoredResumeResponse carrying the new record’s identifier and a status of PENDING, returned with 202 Accepted.'),

  sub('6.3 Request flow through the application'),
  ...figure('fig_6_01_sequence', '6.01', 'Sequence diagram for the tailoring request'),
  p('The flow through the MVC layers is as follows. ResumeController receives the request, Spring deserialises and validates the body, and the controller delegates immediately to ResumeService without inspecting it further. ResumeService loads the master resume with its sections in a single fetch-joined query, calls assertOwnership to compare the resume’s owner against the authenticated principal, and refuses with 403 on mismatch.'),
  p('Within a single transaction the service then persists the job description, persists a tailored-resume record in PENDING, assembles the master content from the resume’s sections, and publishes a TailoringRequestedEvent carrying the identifiers and the assembled content. The controller wraps the returned DTO in a 202 Accepted response and the request completes. At this point the user has a response and the work has not started.'),
  p('The worker consumes the event from the topic. Before performing any work it checks Redis for a key derived from the tailored-resume identifier; if the key is present the event is a redelivery and is acknowledged without reprocessing. Otherwise it sanitises the inputs, invokes the model, validates the returned sections against the expected schema, computes the ATS score in code, writes the sections and score, sets the status to COMPLETED, and records the idempotency key.'),

  sub('6.4 Design decisions taken during implementation'),
  sub3('Why a durable queue rather than an in-process executor'),
  p('An earlier design used an asynchronous executor within the API service. It was replaced because a restart lost every in-flight job with no record that they had been accepted. Kafka makes an accepted request durable: a consumer that crashes mid-processing resumes from its last committed offset. The cost is an additional infrastructure dependency; the benefit is that the durability requirement NFR-02 is met by the platform rather than by application code.'),

  sub3('Why the score is computed in code'),
  p('The obvious alternative is to ask the model to score its own output. This was rejected. A model-generated score is not reproducible — the same input can yield different numbers — and it cannot be explained to a user beyond restating it. Worse, the component that produced the text would also be grading it. Computing the score in code from an explicit weighted formula makes it deterministic, explainable and independent of the generator.'),

  sub3('Why model output is validated'),
  p('Early iterations persisted whatever the model returned, which produced conversational narration stored as resume content and occasionally malformed structure. The output is now validated against an allow-list of section keys with length bounds and narration screening. The operating rule is that the model is an untrusted content source whose output must satisfy a schema before it is written.'),

  sub('6.5 Performance optimisation and benchmarking'),
  p('The listing path — retrieving all of a user’s master resumes together with their sections — exhibited the N+1 select problem. The derived query findByUserId returns the parent rows, and each subsequent access to a resume’s section collection triggers an additional select. For a user with N resumes the operation costs N+1 round trips to the database, and the cost grows linearly with the size of the user’s data.'),
  p('The optimisation was to add a repository method declaring an explicit LEFT JOIN FETCH, so that parents and their sections are retrieved in one statement:'),
  new Paragraph({
    spacing: { before: 120, after: 160, line: LINE1 },
    indent: { left: 400 },
    shading: { type: ShadingType.CLEAR, fill: 'F2F2F2' },
    children: [new TextRun({
      text: '@Query("SELECT r FROM MasterResume r LEFT JOIN FETCH r.sections WHERE r.userId = :userId ORDER BY r.createdAt DESC")',
      font: 'Courier New', size: 19, color: '000000',
    })],
  }),

  sub3('Benchmark method'),
  p('The two implementations were measured against identical data by an automated test, NPlusOneBenchmarkTest, which is part of the repository and can be re-run. The dataset is 30 master resumes of 6 sections each, giving 180 child rows. Statement counts are taken from Hibernate’s Statistics API; timings are the median of 20 measured iterations following 5 warm-up iterations, with the persistence context cleared before each iteration so that no measurement is served from the first-level cache. The database is H2 running in memory, so absolute latencies are lower than they would be against PostgreSQL over a network; the statement-count reduction is exact and machine-independent, and it is the reduction in round trips that dominates real-world cost.'),

  sub3('Results'),
  tableCaption('6.01', 'N+1 optimisation benchmark results'),
  table(
    ['Implementation', 'SQL statements', 'Median latency', 'Improvement'],
    [
      ['findByUserId (lazy loading)', '31', '3.487 ms', 'baseline'],
      ['findByUserIdWithSections (JOIN FETCH)', '1', '1.126 ms', '31× fewer statements, 67.7% faster'],
    ],
    [4, 2, 2, 3],
  ),
  spacerAfterTable(),
  ...figure('fig_6_02_benchmark', '6.02', 'N+1 optimisation: queries and latency, before and after'),
  p('The statement count falls from 31 to 1 — one query for the parents plus one per resume, collapsed into a single fetch-joined query. Median latency falls from 3.487 ms to 1.126 ms, an improvement of 67.7%. The proportional gain would be substantially larger against a networked database, because each eliminated statement there costs a network round trip rather than an in-process call.'),
  p('The result also scales differently. The lazy implementation is O(N) in database round trips, so its cost grows with the number of resumes a user owns; the fetch-joined implementation is O(1) in round trips regardless. The optimisation therefore matters most for the users who have used the product most, which is the correct place for it to matter.'),
  pageBreak(),
);

/* ── 7. Deployment Flow ───────────────────────────────────────── */
body.push(
  chapter('Deployment Flow'),
  p('This chapter describes the target deployment architecture on Amazon Web Services. Section 7.7 records honestly what is currently deployed and how it maps onto this design.'),
  ...figure('fig_7_01_aws', '7.01', 'Target AWS deployment architecture'),

  sub('7.1 Virtual Private Cloud'),
  p('The whole system sits inside a single VPC addressed 10.0.0.0/16, which provides an isolated network in which the deployer controls addressing, routing and reachability. Within it three subnets separate components by exposure. A public subnet, 10.0.1.0/24, holds only what must be reachable from the internet. A private application subnet, 10.0.2.0/24, holds the service instances. A private data subnet, 10.0.3.0/24, holds the stateful services. Only the public subnet has a route to an internet gateway.'),
  p('The reason for this division is blast radius. A database in a public subnet is one misconfigured security group away from being exposed to the internet. A database in a private subnet with no route to an internet gateway cannot be reached from outside the VPC even if its security group is wrong, because there is no network path. Correctness therefore does not depend on a single rule being right.'),

  sub('7.2 EC2 and the compute tier'),
  p('The resume service and the worker service run on EC2 instances in the private application subnet, managed by an Auto Scaling Group across at least two availability zones. Containerising the services and running them on ECS with an EC2 or Fargate capacity provider is the preferable variant, because the project already produces Docker images for all three services; the network design is unchanged either way.'),
  p('The two services scale on different signals, which is the operational payoff of having separated them. The resume service scales on request rate and CPU. The worker service scales on Kafka consumer lag — the count of unprocessed events — because its load is a queue depth rather than a request rate. A backlog of tailoring jobs adds worker instances without adding API instances that are not needed.'),

  sub('7.3 Application Load Balancer'),
  p('An Application Load Balancer in the public subnet terminates TLS and is the only component addressable from the internet. It performs health checks against the Spring Boot Actuator health endpoint and removes failing instances from rotation. Because it operates at layer seven it can route by path, which is where the API gateway’s routing responsibility would be consolidated in a production deployment.'),

  sub('7.4 Security Groups'),
  p('Security groups are stateful instance-level firewalls, and the design uses them as a chain in which each tier accepts traffic only from the tier in front of it:'),
  li('ALB security group — inbound 443 from 0.0.0.0/0. This is the only rule in the system that admits the public internet.'),
  li('Application security group — inbound 8081 and 8082 from the ALB security group only, referenced by group rather than by IP address so the rule stays correct as instances are replaced.'),
  li('Database security group — inbound 5432 from the application security group only.'),
  li('Cache security group — inbound 6379 from the application security group only.'),
  p('Referencing security groups rather than address ranges is what makes this durable under auto-scaling: instances come and go with new private addresses, and no rule needs to change.'),

  sub('7.5 RDS and ElastiCache'),
  p('PostgreSQL runs on RDS in a Multi-AZ configuration, which maintains a synchronous standby in a second availability zone and fails over automatically. RDS also provides automated backups and point-in-time recovery, which for a system holding a user’s only copy of their resume is a requirement rather than an enhancement. Flyway migrations run at application startup, so a deployment applies pending migrations before serving traffic.'),
  p('Redis runs on ElastiCache and holds the idempotency keys described in Section 6. Its contents are deliberately disposable: every key carries a 24-hour expiry, and losing the cache degrades the system to at-least-once processing rather than breaking it. This is the correct durability posture for the data it holds.'),
  p('Kafka is provided by Amazon MSK rather than self-managed brokers, because broker operation — rebalancing, patching, storage management — is substantial work that is not part of this project’s subject matter.'),

  sub('7.6 Secrets and supporting services'),
  p('Database credentials and the JWT signing secret are held in AWS Secrets Manager and injected at container start, never baked into an image or committed. This matches the application’s existing configuration model, in which every production value resolves from an environment variable and the application fails to start if a required one is absent. S3 provides object storage for generated PDF documents; as Section 9 records, PDF generation is implemented but not currently wired, and the absence of object storage is precisely why.'),

  sub('7.7 What is currently deployed'),
  p('The architecture above is the target design. The system as delivered is deployed to Render using the render.yaml descriptor in the repository, with a managed PostgreSQL instance and an external Kafka provider. Render was chosen during development because it removes VPC, subnet and security group configuration entirely, which shortened the deployment feedback loop at a stage when the application itself was changing daily.'),
  p('The components map onto one another directly: Render web services correspond to the EC2 or ECS tier, Render’s managed PostgreSQL to RDS, and the external Kafka provider to MSK. What the Render deployment does not provide is the network isolation described in Section 7.1, the security group chain of Section 7.4, or Multi-AZ database failover. Those are the specific reasons the AWS design above is the target rather than the current state, and migrating to it is listed as future work in Section 9.'),
  pageBreak(),
);

/* ── 8. Technologies Used ─────────────────────────────────────── */
body.push(
  chapter('Technologies Used'),
  p('This chapter describes each significant technology in the system: what it does, why it was chosen here, and where it is used in industry.'),

  sub('8.1 Java 21 and Spring Boot 3.3'),
  p('Java 21 is a long-term-support release of a statically typed, garbage-collected language running on the JVM. Spring Boot is an application framework providing dependency injection, auto-configuration, and integration with data access, security and messaging.'),
  p('It was chosen because the concerns in this project — declarative transactions, a security filter chain, JPA persistence and Kafka integration — are all first-class in the Spring ecosystem rather than assembled from unrelated libraries. Static typing also carries weight on a system with seven interrelated entities: a rename or a signature change surfaces at compile time rather than in production.'),
  p('In industry, Spring Boot is the dominant backend framework in enterprise Java. Netflix built much of its microservice platform on Spring, contributing components later absorbed into Spring Cloud. Banks including Goldman Sachs and HSBC run core transaction processing on the JVM, where a mature concurrency model and predictable garbage collection matter more than language novelty.'),

  sub('8.2 Apache Kafka'),
  p('Kafka is a distributed, append-only commit log. Producers append events to a topic; consumers read forward at their own pace, tracking position by committed offset. Unlike a traditional message queue, reading does not destroy the message: it is retained for a configured period, so a consumer can replay history.'),
  p('It was chosen for durability. An in-memory queue loses accepted work when the process restarts. With Kafka, an accepted tailoring request survives a worker crash: the consumer resumes from its last committed offset and reprocesses. It also decouples the two services in time, so the worker can be slower than the API without applying back-pressure to callers, and the offset model makes at-least-once delivery explicit — which is why the idempotency layer described in Section 6 exists.'),
  p('Kafka originated at LinkedIn for activity stream processing and is now infrastructure at very large scale. Uber uses it for trip events and real-time pricing; Netflix ingests hundreds of billions of events per day through it for viewing telemetry; and it is widely used in financial services for trade event pipelines, where the ability to replay a day’s events for audit is a regulatory requirement rather than a convenience.'),

  sub('8.3 PostgreSQL and Flyway'),
  p('PostgreSQL is an open-source relational database with strong standards compliance, mature transaction support and rich indexing. Flyway is a schema migration tool that applies versioned SQL scripts in order and records what has been applied.'),
  p('A relational database was chosen because the data is relational in the strict sense: users own resumes, resumes own sections, tailored output references both a source resume and a posting, and the integrity of those references matters. A document store would have required the application to enforce that integrity itself. Flyway was chosen because a schema that evolves through reviewable, ordered SQL is auditable, whereas one generated from entity classes at startup is not; the application runs with Hibernate’s validate mode so that any drift fails at startup.'),
  p('PostgreSQL is used at scale by Instagram, which ran its primary user data on sharded PostgreSQL through its period of fastest growth, and by Apple across internal services. Flyway and its counterpart Liquibase are standard in regulated environments, where the ability to demonstrate exactly which schema change was applied and when is an audit requirement.'),

  sub('8.4 Redis'),
  p('Redis is an in-memory data structure store, commonly used as a cache, a distributed lock, a rate limiter or a short-lived key store, with optional persistence and native key expiry.'),
  p('It is used here for consumer idempotency. When the worker begins processing an event it records a key derived from the tailored-resume identifier with a 24-hour expiry, and checks that key before doing work. The requirement is a fast, keyed existence check on short-lived data — Redis is exactly shaped for that, and modelling it as a database table would mean a durable write and a cleanup job for data that is worthless after a day.'),
  p('Twitter uses Redis for timeline caching; Stack Overflow uses it as a distributed cache serving a large fraction of page views; and it is the standard backing store for rate limiters and session state in web architectures generally.'),

  sub('8.5 Large language models via Ollama'),
  p('Ollama runs large language models locally and exposes an OpenAI-compatible chat-completions API, so the same client code works against a local model or a hosted endpoint by changing a URL.'),
  p('It was chosen to avoid per-token cost during development, where the tailoring prompt was invoked repeatedly during iteration, and because the compatible interface means the deployment can move to a hosted provider through configuration alone. The rewriting task is one where a language model is genuinely the right tool: rephrasing experience in the vocabulary of a posting requires handling language that no rule-based transformation would manage well.'),
  p('Generative models are now in production across recruitment technology, customer support triage, code assistance and document summarisation. The engineering lesson from this project generalises beyond the specific model: a language model is a slow, non-deterministic network dependency producing untrusted output, and it should be integrated with the same defensive posture as any third-party service — timeouts, retries, asynchronous execution, and schema validation of everything it returns.'),

  sub('8.6 Spring Security and JSON Web Tokens'),
  p('A JWT is a signed, self-describing token carrying claims about the bearer. Because the signature is verifiable using a key the server already holds, no server-side session lookup is needed to authenticate a request.'),
  p('Statelessness was the deciding factor: any instance behind the load balancer can verify a token independently, so instances can be added and removed freely. The implementation uses jjwt 0.12.6 with the verifyWith and parseSignedClaims API, which requires a valid signature at parse time and so rejects unsigned tokens and the alg:none substitution attack at the library level rather than in application code.'),
  p('Token-based authentication underpins OAuth 2.0 and OpenID Connect and is the standard mechanism for API authentication across the industry, including every major cloud provider’s API.'),

  sub('8.7 Docker and containerisation'),
  p('Docker packages an application with its dependencies into an image that runs identically wherever a container runtime exists. All three services build multi-stage images: a build stage compiles with Maven, and a runtime stage copies only the resulting artifact onto a slim JRE base, keeping images small and excluding build tooling from the deployed surface. Containers run as a non-root user.'),
  p('Containerisation is close to universal in modern deployment and is the packaging format underlying Kubernetes, ECS and most managed platforms — including Render, where this project is currently deployed.'),

  sub('8.8 Testing: JUnit 5, Mockito and JaCoCo'),
  p('JUnit 5 provides the test framework, Mockito supplies the test doubles, Spring Boot Test provides context and MockMvc support for driving the HTTP layer without a running server, and JaCoCo measures coverage by instrumenting bytecode during the test run. The choice to measure coverage rather than estimate it is deliberate: an earlier revision of this project documentation asserted a coverage figure that had never been measured and was wrong by an order of magnitude.'),
  p('The suite comprises 94 tests, all passing, and is verified reproducibly — the committed tree is exported with git archive into a clean directory and built there, so the result reflects the repository rather than a developer machine.'),

  tableCaption('8.02', 'Automated test suites'),
  table(
    ['Suite', 'Tests', 'What it verifies'],
    [
      ['ResumeServiceTest', '17', 'Service logic in isolation with mocked collaborators: a refused ownership check performs no writes, the published event carries the correct identifiers and assembled content, the retry state machine rejects records that are not FAILED'],
      ['BOLATest', '11', 'Cross-tenant authorisation over HTTP: own-data access succeeds, every cross-user read, write, list and chat attempt returns 403, and missing or tampered tokens return 401'],
      ['AuthServiceTest', '13', 'Registration, duplicate-email conflict, email normalisation, BCrypt hashing and login failure paths'],
      ['RateLimitFilterTest', '7', 'Requests below the limit pass, the next is refused with 429, and limits are isolated per client'],
      ['ATSScorerTest and edge cases', '21', 'Scoring arithmetic, component weighting and boundary conditions'],
      ['KeywordExtractorTest and edge cases', '24', 'Tokenisation, stop-word removal, punctuation and case handling'],
      ['NPlusOneBenchmarkTest', '1', 'Measures the JOIN FETCH optimisation reported in Section 6.5'],
    ],
    [2.6, 1, 6.4],
  ),
  spacerAfterTable(),

  sub3('Test doubles and why the distinction matters'),
  p('A test double is any object substituted for a real collaborator. The suite uses three kinds, and they are not interchangeable:'),
  li('Stub — an object configured to return prepared answers, supplying the state a scenario needs. The repositories are stubbed with when(...).thenReturn(...) so a test can describe a resume that exists and is owned by a particular user, without a database.'),
  li('Mock — a double whose interactions are themselves the assertion. TailoringProducer is a mock: the observable behaviour of triggerTailoring is that it publishes exactly one event with particular contents, so the test asserts with verify() and an ArgumentCaptor rather than by inspecting a return value. Equally important is the negative case, verified with never() and verifyNoInteractions(): when an ownership check fails, no job description is saved, no tailored resume is written, and nothing is published.'),
  li('Fake — a real but simplified implementation. SimpleMeterRegistry is used in place of the production MeterRegistry because ResumeService constructs a Counter from it; a mock would return null and the constructor would fail. A fake is the correct double whenever the collaborator has behaviour the subject genuinely depends on.'),
  p('The distinction has practical consequences. A suite that only stubs can confirm what a method returns but not what it did, so a service that silently skipped publishing its event would still pass. Verifying the interaction is what turns the test into a specification of behaviour.'),

  tableCaption('8.03', 'Technology selection and rationale'),
  table(
    ['Technology', 'Role in the system', 'Primary reason for selection'],
    [
      ['Java 21 / Spring Boot 3.3', 'All three services', 'First-class support for transactions, security and messaging'],
      ['Apache Kafka', 'Asynchronous tailoring queue', 'Durability and replay; work survives a consumer crash'],
      ['PostgreSQL', 'Primary datastore', 'The data is relational and referential integrity is wanted'],
      ['Flyway', 'Schema migration', 'Versioned, reviewable, auditable schema evolution'],
      ['Redis', 'Consumer idempotency', 'Fast keyed existence check on data with a natural expiry'],
      ['Ollama', 'LLM inference', 'Local execution during development; portable API'],
      ['Spring Security + JWT', 'Authentication', 'Stateless verification; no shared session store'],
      ['Docker', 'Packaging', 'Reproducible artifacts across environments'],
      ['JUnit 5 / JaCoCo', 'Testing and coverage', 'Measured rather than estimated quality signal'],
    ],
    [2.5, 3, 4.5],
  ),
  spacerAfterTable(),
  pageBreak(),
);

/* ── 9. Conclusion ────────────────────────────────────────────── */
body.push(
  chapter('Conclusion'),
  p('ResumeForge implements an event-driven backend that separates slow, unreliable language model work from a synchronous API using a durable queue, an idempotent consumer and a polling contract. The relational schema is versioned and validated at startup, authorisation is enforced at the service layer and verified by cross-tenant tests, and model output is treated as untrusted input subject to schema validation before persistence.'),

  sub('9.1 Key takeaways'),
  sub3('Asynchronous decomposition is a design decision, not an optimisation'),
  p('The most valuable lesson was that the boundary between the two services is defined by the runtime characteristics of the work rather than by the domain. The API is bound by database connections and must be fast and predictable; the worker is bound by model throughput and is permitted to be slow and to fail. Once that boundary is drawn, questions that were previously difficult become straightforward: what to scale, what to retry, what a client should be told while work is outstanding.'),

  sub3('Delivery guarantees dictate application design'),
  p('Understanding that Kafka provides at-least-once rather than exactly-once delivery changed the consumer’s design. Redelivery is normal operation rather than an error, so the consumer must be idempotent. This is a general property of distributed messaging and not a Kafka limitation, and internalising it is the difference between a system that works in testing and one that works under failure.'),

  sub3('A successful response is not evidence of a correct write'),
  p('The cascade defect described in Section 5.3 corrupted data on the most frequently used write path while raising no error and failing no test. Finding it required reasoning about ORM semantics rather than observing a failure. The general lesson is that an ORM abstracts the database but does not remove the need to understand what it emits.'),

  sub3('Authorisation belongs where it cannot be bypassed'),
  p('Placing the ownership check at the service boundary rather than in controllers was validated when a review found an endpoint that had been written without it. Because the convention was to enforce in the service, the gap was localised to one class and closing it was a small change with a regression test, rather than an audit of every controller.'),

  sub3('Documentation that contradicts the code is worse than none'),
  p('Several planning documents in the repository described work as outstanding that had been completed, and one specified a component that was never written. A reader encountering them would have concluded the system was half-finished. They were removed and the project README rewritten against the actual code with every claim verified. Accurate documentation is part of the deliverable, not an accompaniment to it.'),

  sub('9.2 Practical applications'),
  p('The immediate application is the one the system was built for: reducing the manual effort of tailoring a resume per application while giving the candidate a reproducible measure of how well the result matches the posting.'),
  p('The architectural pattern generalises well beyond that. Any system integrating a generative model into a transactional application faces the same constraints — the model is slow, non-deterministic, occasionally unavailable, and produces output that must not be trusted. The combination used here of a durable queue, an idempotent consumer, schema validation on output and a deterministic scoring step computed outside the model applies directly to document processing in insurance and legal work, to clinical note summarisation in healthcare, to support ticket triage, and to any workflow where a model proposes content that a system must then store and act upon.'),
  p('The scoring approach is worth isolating as a transferable idea. Where a generative component produces output that must be assessed, computing the assessment in deterministic code rather than asking the model to grade itself yields a metric that is reproducible, explainable to an end user, and independent of the generator.'),

  sub('9.3 Limitations'),
  p('The following are real constraints of the delivered system, recorded explicitly.'),
  tableCaption('9.01', 'Known limitations of the delivered system'),
  table(
    ['Limitation', 'Detail and remediation'],
    [
      ['PDF export is not wired', 'Document generation is implemented but the consumer sets the path to null because no object storage is provisioned. Remedied by provisioning S3 and wiring the generator.'],
      ['Event publication is a dual write', 'The event is published inside the database transaction, so a broker failure after commit loses it. Remedied by a transactional outbox.'],
      ['Scoring logic is duplicated', 'The scorer exists in both services and the copies have diverged; only the untested worker copy runs. Remedied by extracting a shared module.'],
      ['Cross-service table writes', 'The worker writes to the resume service’s tables with native SQL, bypassing the optimistic lock. Remedied by routing writes through an owning service.'],
      ['Uneven test coverage', 'Measured coverage is 64.6% of lines on the resume service; the worker service and gateway still have no suite of their own.'],
      ['Migrations are not exercised by tests', 'Tests run against H2 with the schema generated from entities, so the migration chain is unverified. Remedied by running migrations against PostgreSQL under Testcontainers.'],
    ],
    [3, 7],
  ),
  spacerAfterTable(),

  sub3('Cost implications'),
  p('The AWS design in Section 7 is materially more expensive than the current deployment. Multi-AZ RDS approximately doubles database cost against a single instance; MSK carries a per-broker charge with a three-broker minimum for production; and NAT gateways are charged hourly plus per gigabyte processed. For a system at this stage the managed-platform deployment is the economically correct choice, and the AWS design becomes justified when the availability and isolation guarantees are worth their cost. Language model inference is the other significant variable cost, and it is the reason the input size caps described in Section 6 exist.'),

  sub3('Suggestions for improvement'),
  ni('Extract the scoring logic into a shared module so one tested implementation runs in production.'),
  ni('Introduce a transactional outbox so event publication cannot diverge from the database commit.'),
  ni('Add a test suite for the worker service, which contains the tailoring pipeline and is currently untested.'),
  ni('Execute the Flyway chain against PostgreSQL in tests using Testcontainers.'),
  ni('Provision object storage and complete the PDF export feature.'),
  ni('Move the rate limiter’s window into Redis so limits hold across instances, and key it on a trusted proxy header.'),
  pageBreak(),
);

/* ── 10. References ───────────────────────────────────────────── */
const ref = (text) => new Paragraph({
  numbering: { reference: 'num', level: 0 },
  alignment: AlignmentType.LEFT,
  spacing: { line: LINE1, after: 110 },
  children: [t(text)],
});

body.push(
  chapter('References'),
  ref('Apache Software Foundation, Apache Kafka Documentation — Design and Delivery Semantics, kafka.apache.org/documentation, referred August 2026.'),
  ref('VMware Tanzu, Spring Boot Reference Documentation, version 3.3, docs.spring.io/spring-boot/docs/current/reference/html, referred August 2026.'),
  ref('VMware Tanzu, Spring Security Reference — Architecture and Authorization, docs.spring.io/spring-security/reference, referred August 2026.'),
  ref('Red Hat, Hibernate ORM 6.5 User Guide — Associations, Fetching and Locking, docs.jboss.org/hibernate/orm/6.5/userguide, referred August 2026.'),
  ref('PostgreSQL Global Development Group, PostgreSQL 16 Documentation — Indexes and Concurrency Control, postgresql.org/docs/16, referred August 2026.'),
  ref('Redgate, Flyway Documentation — Migrations and Versioning, documentation.red-gate.com/flyway, referred August 2026.'),
  ref('Redis Ltd., Redis Documentation — Key Expiration and Data Types, redis.io/docs, referred August 2026.'),
  ref('Amazon Web Services, Amazon VPC User Guide and Security Group Rules Reference, docs.aws.amazon.com/vpc, referred August 2026.'),
  ref('Amazon Web Services, Amazon RDS User Guide — Multi-AZ Deployments, docs.aws.amazon.com/AmazonRDS, referred August 2026.'),
  ref('Amazon Web Services, Amazon Managed Streaming for Apache Kafka Developer Guide, docs.aws.amazon.com/msk, referred August 2026.'),
  ref('OWASP Foundation, OWASP API Security Top 10 — API1:2023 Broken Object Level Authorization, owasp.org/API-Security, referred August 2026.'),
  ref('OWASP Foundation, OWASP Top 10 for Large Language Model Applications — LLM01 Prompt Injection, owasp.org/www-project-top-10-for-large-language-model-applications, referred August 2026.'),
  ref('Jones M., Bradley J. and Sakimura N., RFC 7519: JSON Web Token (JWT), Internet Engineering Task Force, 2015.'),
  ref('Fielding R. and Reschke J., RFC 7231: HTTP/1.1 Semantics and Content — Section 6.3.3 (202 Accepted), Internet Engineering Task Force, 2014.'),
  ref('Provos N. and Mazières D., A Future-Adaptable Password Scheme, USENIX Annual Technical Conference, 1999.'),
  ref('Kleppmann M., Designing Data-Intensive Applications, O’Reilly Media, 2017.'),
  ref('Newman S., Building Microservices: Designing Fine-Grained Systems, 2nd edition, O’Reilly Media, 2021.'),
  ref('Richardson C., Microservices Patterns — Chapter 3, Transactional Messaging and the Outbox Pattern, Manning Publications, 2018.'),
  ref('Fowler M., Patterns of Enterprise Application Architecture — Optimistic Offline Lock, Addison-Wesley, 2002.'),
  ref('EclEmma, JaCoCo Java Code Coverage Library Documentation, jacoco.org/jacoco/trunk/doc, referred August 2026.'),
);

/* ═══════════════════════════════════════════════════════════════ */
const doc = new Document({
  creator: D.name,
  title: 'ResumeForge — Applied Software Project Report',
  description: 'Master of Science in Computer Science — Scaler Neovarsity / Woolf',
  numbering: {
    config: [
      {
        reference: 'bul',
        levels: [
          { level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 520, hanging: 260 } } } },
          { level: 1, format: LevelFormat.BULLET, text: '◦', alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 900, hanging: 260 } } } },
        ],
      },
      {
        reference: 'num',
        levels: [
          { level: 0, format: LevelFormat.DECIMAL, text: '%1.', alignment: AlignmentType.LEFT,
            style: { paragraph: { indent: { left: 560, hanging: 300 } } } },
        ],
      },
    ],
  },
  styles: { default: { document: { run: { font: SERIF, size: BODY, color: '000000' } } } },
  sections: [{
    properties: {
      page: { margin: { top: 1440, bottom: 1440, left: 1800, right: 1800 } }, // 1" / 1.25"
    },
    footers: {
      default: new Footer({
        children: [new Paragraph({
          alignment: AlignmentType.CENTER,
          spacing: { line: LINE1 },
          children: [new TextRun({ children: [PageNumber.CURRENT], font: SERIF, size: 20, color: '000000' })],
        })],
      }),
    },
    children: [...front, ...body],
  }],
});

Packer.toBuffer(doc).then((buf) => {
  const out = process.argv[2] || 'ResumeForge_Applied_Software_Project_Report.docx';
  fs.writeFileSync(out, buf);
  console.log(`wrote ${out}  (${Math.round(buf.length / 1024)} KB)`);
});
