/* Generates all report figures as PNGs. */
const { Resvg } = require('@resvg/resvg-js');
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, 'figures');
fs.mkdirSync(OUT, { recursive: true });

const FONT = 'Helvetica, Arial, sans-serif';
const INK = '#1a1a1a';
const LINE = '#444';
const FILL_A = '#e8eef7';   // service
const FILL_B = '#eef6ec';   // infra
const FILL_C = '#fdf3e3';   // external
const FILL_D = '#f2f2f4';   // neutral

const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

function svg(w, h, body) {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}">
<rect width="${w}" height="${h}" fill="#ffffff"/>
<defs>
  <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
    <path d="M 0 0 L 10 5 L 0 10 z" fill="${LINE}"/>
  </marker>
  <marker id="openarrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse">
    <path d="M 0 0 L 10 5 L 0 10" fill="none" stroke="${LINE}" stroke-width="1.4"/>
  </marker>
  <marker id="diamond" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="11" markerHeight="11" orient="auto">
    <path d="M 0 6 L 6 2 L 12 6 L 6 10 z" fill="#fff" stroke="${LINE}" stroke-width="1.2"/>
  </marker>
</defs>
${body}
</svg>`;
}

const box = (x, y, w, h, label, fill = FILL_D, sub = null, fs_ = 15) => `
<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="5" fill="${fill}" stroke="${LINE}" stroke-width="1.3"/>
<text x="${x + w / 2}" y="${y + (sub ? h / 2 - 5 : h / 2 + 5)}" font-family="${FONT}" font-size="${fs_}" font-weight="600" fill="${INK}" text-anchor="middle">${esc(label)}</text>
${sub ? `<text x="${x + w / 2}" y="${y + h / 2 + 15}" font-family="${FONT}" font-size="12.5" fill="#555" text-anchor="middle">${esc(sub)}</text>` : ''}`;

const cyl = (x, y, w, h, label, sub = null) => `
<path d="M ${x} ${y + 12} a ${w / 2} 12 0 0 1 ${w} 0 v ${h - 24} a ${w / 2} 12 0 0 1 ${-w} 0 z" fill="${FILL_B}" stroke="${LINE}" stroke-width="1.3"/>
<path d="M ${x} ${y + 12} a ${w / 2} 12 0 0 0 ${w} 0" fill="none" stroke="${LINE}" stroke-width="1.3"/>
<text x="${x + w / 2}" y="${y + h / 2 + (sub ? 0 : 6)}" font-family="${FONT}" font-size="14" font-weight="600" fill="${INK}" text-anchor="middle">${esc(label)}</text>
${sub ? `<text x="${x + w / 2}" y="${y + h / 2 + 18}" font-family="${FONT}" font-size="12" fill="#555" text-anchor="middle">${esc(sub)}</text>` : ''}`;

const line = (x1, y1, x2, y2, marker = 'arrow', dash = null) =>
  `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${LINE}" stroke-width="1.5" ${dash ? `stroke-dasharray="${dash}"` : ''} marker-end="url(#${marker})"/>`;

const plain = (x1, y1, x2, y2, dash = null) =>
  `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${LINE}" stroke-width="1.4" ${dash ? `stroke-dasharray="${dash}"` : ''}/>`;

const txt = (x, y, s, size = 12.5, anchor = 'middle', weight = '400', color = '#333') =>
  `<text x="${x}" y="${y}" font-family="${FONT}" font-size="${size}" font-weight="${weight}" fill="${color}" text-anchor="${anchor}">${esc(s)}</text>`;

/* UML class box: name / attributes / methods */
function uml(x, y, w, name, attrs, methods, stereotype = null) {
  const lh = 17;
  const hHead = stereotype ? 44 : 28;
  const hA = attrs.length * lh + 8;
  const hM = methods.length * lh + 8;
  const h = hHead + hA + hM;
  let s = `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="#ffffff" stroke="${LINE}" stroke-width="1.3"/>`;
  s += `<rect x="${x}" y="${y}" width="${w}" height="${hHead}" fill="${FILL_A}" stroke="${LINE}" stroke-width="1.3"/>`;
  if (stereotype) {
    s += txt(x + w / 2, y + 17, `«${stereotype}»`, 11.5, 'middle', '400', '#555');
    s += txt(x + w / 2, y + 35, name, 14, 'middle', '700', INK);
  } else {
    s += txt(x + w / 2, y + 19, name, 14, 'middle', '700', INK);
  }
  let cy = y + hHead;
  s += plain(x, cy, x + w, cy);
  attrs.forEach((a, i) => { s += txt(x + 9, cy + 16 + i * lh, a, 11.8, 'start'); });
  cy += hA;
  s += plain(x, cy, x + w, cy);
  methods.forEach((m, i) => { s += txt(x + 9, cy + 16 + i * lh, m, 11.8, 'start'); });
  return { svg: s, h };
}

const figures = {};

/* ── Fig 2.01 — System architecture ───────────────────────────── */
figures['fig_2_01_architecture'] = () => {
  let b = '';
  b += box(40, 150, 130, 62, 'Web Client', FILL_D, 'Next.js 16');
  b += box(235, 150, 175, 62, 'resume-service', FILL_A, 'REST · JWT · JPA  :8081');
  b += box(235, 30, 175, 56, 'api-gateway', FILL_A, 'Spring Cloud GW  :8080');
  b += box(600, 150, 175, 62, 'worker-service', FILL_A, 'Consumer · LLM  :8082');
  b += box(455, 152, 100, 58, 'Kafka', FILL_B, 'topic');
  b += cyl(235, 275, 175, 76, 'PostgreSQL', 'Flyway V1–V8');
  b += cyl(600, 275, 175, 76, 'Redis', 'idempotency 24h');
  b += box(840, 150, 118, 62, 'Ollama', FILL_C, 'LLM API');

  b += line(170, 181, 233, 181);
  b += line(322, 148, 322, 88, 'arrow', '4 3');
  b += txt(340, 120, 'not on', 11, 'start');
  b += txt(340, 133, 'request path', 11, 'start');
  b += line(410, 181, 453, 181);
  b += line(555, 181, 598, 181);
  b += line(775, 181, 838, 181);
  b += line(322, 212, 322, 273);
  b += line(687, 212, 687, 273);
  b += line(598, 300, 412, 300);
  b += txt(505, 293, 'writes result', 11.5);
  b += txt(432, 172, 'publish', 11.5);
  b += txt(575, 172, 'consume', 11.5);
  b += txt(505, 200, 'resume.tailoring.requested', 10.5);
  b += txt(200, 172, 'HTTPS', 11);
  return svg(1000, 380, b);
};

/* ── Fig 2.02 — Tailoring lifecycle ───────────────────────────── */
figures['fig_2_02_lifecycle'] = () => {
  let b = '';
  const W = 140, H = 54;
  const X = { pending: 50, processing: 275, done: 520 };
  const yTop = 60, yLow = 190;

  b += box(X.pending, yTop, W, H, 'PENDING', FILL_D);
  b += box(X.processing, yTop, W, H, 'PROCESSING', FILL_A);
  b += box(X.done, yTop, W, H, 'COMPLETED', FILL_B);
  b += box(X.done, yLow, W, H, 'FAILED', FILL_C);

  // PENDING -> PROCESSING
  b += line(X.pending + W, yTop + H / 2, X.processing, yTop + H / 2);
  b += txt((X.pending + W + X.processing) / 2, yTop + H / 2 - 12, 'consumed', 11.5);

  // PROCESSING -> COMPLETED
  b += line(X.processing + W, yTop + H / 2, X.done, yTop + H / 2);
  b += txt((X.processing + W + X.done) / 2, yTop + H / 2 - 12, 'output validated', 11.5);

  // PROCESSING -> FAILED
  b += line(X.processing + W / 2, yTop + H, X.done, yLow + H / 2);
  b += txt(X.processing + W / 2 + 96, yLow + 6, 'LLM error or', 11.5);
  b += txt(X.processing + W / 2 + 96, yLow + 20, 'invalid output', 11.5);

  // FAILED -> PENDING (retry)
  b += `<path d="M ${X.done} ${yLow + H / 2} H ${X.pending + W / 2 + 40} q -40 0 -40 -40 V ${yTop + H + 4}"
        fill="none" stroke="${LINE}" stroke-width="1.5" stroke-dasharray="5 4" marker-end="url(#arrow)"/>`;
  b += txt(X.done - 130, yLow + H / 2 - 12, 'POST /tailored/{id}/retry — republishes the event', 11.5, 'end');
  return svg(740, 300, b);
};

/* ── Fig 3.01 — Use case diagram ──────────────────────────────── */
figures['fig_3_01_usecase'] = () => {
  let b = '';
  // actor
  const actor = (x, y, label) => `
    <circle cx="${x}" cy="${y}" r="13" fill="#fff" stroke="${LINE}" stroke-width="1.5"/>
    <line x1="${x}" y1="${y + 13}" x2="${x}" y2="${y + 45}" stroke="${LINE}" stroke-width="1.5"/>
    <line x1="${x - 18}" y1="${y + 24}" x2="${x + 18}" y2="${y + 24}" stroke="${LINE}" stroke-width="1.5"/>
    <line x1="${x}" y1="${y + 45}" x2="${x - 15}" y2="${y + 70}" stroke="${LINE}" stroke-width="1.5"/>
    <line x1="${x}" y1="${y + 45}" x2="${x + 15}" y2="${y + 70}" stroke="${LINE}" stroke-width="1.5"/>
    ${txt(x, y + 90, label, 13, 'middle', '600', INK)}`;
  const uc = (cx, cy, label) => `
    <ellipse cx="${cx}" cy="${cy}" rx="112" ry="27" fill="${FILL_A}" stroke="${LINE}" stroke-width="1.3"/>
    ${txt(cx, cy + 5, label, 12.5)}`;

  b += `<rect x="235" y="20" width="420" height="530" fill="none" stroke="${LINE}" stroke-width="1.3" stroke-dasharray="6 4"/>`;
  b += txt(445, 45, 'ResumeForge', 15, 'middle', '700', INK);

  b += actor(90, 200, 'Job Seeker');
  b += actor(790, 250, 'Worker Service');

  const cases = ['Register / Log in', 'Manage master resume', 'Submit job description',
                 'Request tailoring', 'View ATS score', 'Edit tailored resume',
                 'Chat to revise resume', 'Retry failed job'];
  cases.forEach((c, i) => { b += uc(445, 90 + i * 58, c); });

  cases.forEach((_, i) => { b += plain(120, 215, 336, 90 + i * 58); });
  [3, 4].forEach((i) => { b += plain(556, 90 + i * 58, 762, 265); });
  return svg(900, 580, b);
};

/* ── Fig 4.01 — Domain class diagram ──────────────────────────── */
figures['fig_4_01_domain'] = () => {
  let b = '';
  const u = uml(40, 40, 210, 'User',
    ['- id: UUID', '- email: String', '- passwordHash: String', '- name: String', '- plan: String'],
    ['+ getId(): UUID']);
  b += u.svg;

  const mr = uml(340, 40, 232, 'MasterResume',
    ['- id: UUID', '- userId: UUID', '- title: String', '- summary: String', '- version: int  «@Version»'],
    ['+ getSections(): List']);
  b += mr.svg;

  const mrs = uml(660, 40, 226, 'MasterResumeSection',
    ['- id: UUID', '- sectionType: SectionType', '- content: String', '- position: int'],
    ['+ getContent(): String']);
  b += mrs.svg;

  const jd = uml(40, 300, 210, 'JobDescription',
    ['- id: UUID', '- userId: UUID', '- companyName: String', '- jobTitle: String', '- description: String'],
    []);
  b += jd.svg;

  const tr = uml(340, 300, 232, 'TailoredResume',
    ['- id: UUID', '- masterResume: MasterResume', '- jobDescription: JobDescription', '- status: TailoringStatus', '- pdfPath: String'],
    ['+ getStatus()']);
  b += tr.svg;

  const trs = uml(660, 300, 226, 'TailoredResumeSection',
    ['- id: UUID', '- sectionType: SectionType', '- content: String'],
    []);
  b += trs.svg;

  const ats = uml(340, 500, 232, 'ATSScoreResult',
    ['- id: UUID', '- totalScore: int', '- keywordScore: int', '- sectionScore: int', '- actionVerbScore: int'],
    []);
  b += ats.svg;

  b += plain(250, 90, 340, 90); b += txt(295, 82, '1 : m', 11);
  b += `<line x1="572" y1="100" x2="660" y2="100" stroke="${LINE}" stroke-width="1.4" marker-start="url(#diamond)"/>`;
  b += txt(616, 92, '1 : m', 11);
  b += plain(250, 350, 340, 350); b += txt(295, 342, '1 : m', 11);
  b += `<line x1="572" y1="350" x2="660" y2="350" stroke="${LINE}" stroke-width="1.4" marker-start="url(#diamond)"/>`;
  b += txt(616, 342, '1 : m', 11);
  b += plain(456, 420, 456, 500); b += txt(478, 465, '1 : 1', 11, 'start');
  b += plain(145, 210, 145, 300); b += txt(167, 258, 'owns', 11, 'start');
  return svg(940, 620, b);
};

/* ── Fig 4.02 — Layered / service class diagram ───────────────── */
figures['fig_4_02_layers'] = () => {
  let b = '';
  const c = uml(40, 50, 250, 'ResumeController',
    ['- resumeService: ResumeService', '- chatService: ResumeChatService'],
    ['+ createMasterResume(): 201', '+ tailorResume(): 202', '+ getTailoredResume(): 200'], 'RestController');
  b += c.svg;

  const s = uml(370, 50, 268, 'ResumeService',
    ['- masterResumeRepository', '- tailoredResumeRepository', '- tailoringProducer'],
    ['- assertOwnership(UUID)', '+ createMasterResume()', '+ triggerTailoring()', '+ retryTailoring()'], 'Service');
  b += s.svg;

  const r = uml(720, 50, 244, 'MasterResumeRepository',
    ['«extends JpaRepository»'],
    ['+ findByUserId()', '+ findByUserIdWithSections()', '+ findByIdWithSections()'], 'Repository');
  b += r.svg;

  const f = uml(40, 300, 250, 'JwtAuthFilter',
    ['- jwtUtil: JwtUtil'],
    ['# doFilterInternal()'], 'Component');
  b += f.svg;

  const rl = uml(370, 300, 268, 'RateLimitFilter',
    ['- windows: Map<String, Deque>'],
    ['- isAllowed(rule, ip)', '- resolveClientIp(req)'], 'Component');
  b += rl.svg;

  const pr = uml(720, 300, 244, 'TailoringProducer',
    ['- kafkaTemplate'],
    ['+ publish(event)'], 'Component');
  b += pr.svg;

  b += line(290, 110, 368, 110, 'openarrow');
  b += line(638, 110, 718, 110, 'openarrow');
  b += line(504, 190, 504, 298, 'openarrow', '5 4');
  b += line(760, 190, 800, 298, 'openarrow', '5 4');
  b += txt(329, 102, 'uses', 11);
  b += txt(678, 102, 'uses', 11);
  return svg(1010, 430, b);
};

/* ── Fig 5.01 — ER diagram ────────────────────────────────────── */
figures['fig_5_01_er'] = () => {
  let b = '';
  const ent = (x, y, w, name, cols) => {
    const lh = 16.5, h = 30 + cols.length * lh + 6;
    let s = `<rect x="${x}" y="${y}" width="${w}" height="${h}" fill="#fff" stroke="${LINE}" stroke-width="1.3"/>`;
    s += `<rect x="${x}" y="${y}" width="${w}" height="30" fill="${FILL_B}" stroke="${LINE}" stroke-width="1.3"/>`;
    s += txt(x + w / 2, y + 20, name, 13.5, 'middle', '700', INK);
    cols.forEach((cl, i) => { s += txt(x + 8, y + 46 + i * lh, cl, 11.3, 'start'); });
    return { s, h };
  };
  const e1 = ent(40, 40, 210, 'users', ['PK id : uuid', 'email : varchar UQ', 'password_hash : varchar', 'name, plan, created_at']);
  const e2 = ent(330, 40, 232, 'master_resumes', ['PK id : uuid', 'FK user_id : uuid', 'title, summary', 'version : int', 'created_at']);
  const e3 = ent(650, 40, 250, 'master_resume_sections', ['PK id : uuid', 'FK master_resume_id NOT NULL', 'section_type : enum', 'content : text', 'position : int']);
  const e4 = ent(40, 270, 210, 'job_descriptions', ['PK id : uuid', 'FK user_id : uuid', 'company_name, job_title', 'description : text', 'required_skills']);
  const e5 = ent(330, 270, 232, 'tailored_resumes', ['PK id : uuid', 'FK master_resume_id', 'FK job_description_id', 'status : enum', 'pdf_path, version']);
  const e6 = ent(650, 270, 250, 'tailored_resume_sections', ['PK id : uuid', 'FK tailored_resume_id', 'section_type : enum', 'content : text']);
  const e7 = ent(330, 470, 232, 'ats_score_results', ['PK id : uuid', 'FK tailored_resume_id UQ', 'total_score : int', 'keyword_score, section_score', 'action_verb_score']);
  [e1, e2, e3, e4, e5, e6, e7].forEach((e) => { b += e.s; });

  const crow = (x1, y1, x2, y2, l, r) => plain(x1, y1, x2, y2) + txt((x1 + x2) / 2, y1 - 7, `${l} — ${r}`, 11);
  b += crow(250, 95, 330, 95, '1', 'm');
  b += crow(562, 95, 650, 95, '1', 'm');
  b += crow(250, 325, 330, 325, '1', 'm');
  b += crow(562, 325, 650, 325, '1', 'm');
  b += plain(446, 385, 446, 470); b += txt(470, 432, '1 — 1', 11, 'start');
  b += plain(145, 155, 145, 270); b += txt(168, 216, '1 — m', 11, 'start');
  return svg(950, 600, b);
};

/* ── Fig 6.01 — Request flow sequence ─────────────────────────── */
figures['fig_6_01_sequence'] = () => {
  let b = '';
  const actors = ['Client', 'ResumeController', 'ResumeService', 'PostgreSQL', 'Kafka', 'worker-service'];
  const xs = [70, 240, 430, 610, 760, 920];
  actors.forEach((a, i) => {
    b += box(xs[i] - 72, 25, 144, 42, a, i === 0 ? FILL_D : FILL_A, null, 12.5);
    b += plain(xs[i], 67, xs[i], 620, '4 4');
  });
  const msg = (from, to, y, label, dash = null) => {
    const x1 = xs[from], x2 = xs[to];
    return line(x1, y, x2, y, 'arrow', dash) +
      txt((x1 + x2) / 2, y - 8, label, 11.3);
  };
  b += msg(0, 1, 110, 'POST /{id}/tailor');
  b += msg(1, 2, 150, 'triggerTailoring()');
  b += msg(2, 3, 190, 'assertOwnership + load');
  b += msg(3, 2, 225, 'MasterResume', 'arrow');
  b += msg(2, 3, 265, 'save JobDescription');
  b += msg(2, 3, 300, 'save TailoredResume (PENDING)');
  b += msg(2, 4, 340, 'publish TailoringRequestedEvent');
  b += msg(2, 1, 380, 'TailoredResumeResponse');
  b += msg(1, 0, 415, '202 Accepted', null);
  b += `<rect x="${xs[4] - 10}" y="450" width="${xs[5] - xs[4] + 20}" height="0" fill="none"/>`;
  b += msg(4, 5, 470, 'consume');
  b += msg(5, 3, 510, 'idempotency + LLM + score');
  b += msg(5, 3, 550, 'write sections, status=COMPLETED');
  b += msg(0, 3, 595, 'GET /tailored/{id}  (poll)', '5 4');
  b += `<line x1="30" y1="432" x2="990" y2="432" stroke="#999" stroke-width="1" stroke-dasharray="3 3"/>`;
  b += txt(508, 447, 'asynchronous — client already has its response', 11.5, 'middle', '600', '#666');
  return svg(1010, 640, b);
};

/* ── Fig 6.02 — Benchmark chart ───────────────────────────────── */
figures['fig_6_02_benchmark'] = () => {
  let b = '';
  const H = 300, base = 330, left = 120;
  b += plain(left, base, 860, base);
  b += plain(left, base, left, 40);
  const bar = (x, val, max, label, sub, color) => {
    const h = (val / max) * H;
    return `<rect x="${x}" y="${base - h}" width="120" height="${h}" fill="${color}" stroke="${LINE}" stroke-width="1.2"/>` +
      txt(x + 60, base - h - 12, sub, 13, 'middle', '700', INK) +
      txt(x + 60, base + 22, label, 12.5, 'middle', '600') ;
  };
  // queries
  b += txt(300, 30, 'SQL statements', 13.5, 'middle', '700', INK);
  b += bar(190, 31, 31, 'lazy', '31', '#d9a4a4');
  b += bar(350, 1, 31, 'JOIN FETCH', '1', '#a4c3a4');
  // latency
  b += txt(700, 30, 'Median latency (ms)', 13.5, 'middle', '700', INK);
  b += bar(590, 3.487, 4.2, 'lazy', '3.487', '#d9a4a4');
  b += bar(750, 1.126, 4.2, 'JOIN FETCH', '1.126', '#a4c3a4');
  b += plain(480, 40, 480, base, '4 4');
  b += txt(490, base + 52, '30 master resumes × 6 sections · H2 in-memory · median of 20 runs after 5 warm-ups', 12, 'middle', '400', '#555');
  return svg(920, 400, b);
};

/* ── Fig 7.01 — AWS deployment ────────────────────────────────── */
figures['fig_7_01_aws'] = () => {
  let b = '';
  b += `<rect x="30" y="60" width="920" height="470" rx="6" fill="#fbfbfd" stroke="${LINE}" stroke-width="1.4"/>`;
  b += txt(60, 84, 'AWS Region  ap-south-1', 13.5, 'start', '700', INK);
  b += `<rect x="55" y="100" width="870" height="410" rx="5" fill="#fff" stroke="#7a8ba6" stroke-width="1.3" stroke-dasharray="6 4"/>`;
  b += txt(80, 122, 'VPC  10.0.0.0/16', 12.5, 'start', '700', '#41608c');

  b += `<rect x="80" y="140" width="400" height="160" rx="5" fill="#eef4fb" stroke="#9bb0cc" stroke-width="1.1"/>`;
  b += txt(100, 162, 'Public subnet  10.0.1.0/24', 12, 'start', '600', '#41608c');
  b += box(105, 178, 165, 52, 'ALB', FILL_A, 'HTTPS :443');
  b += box(295, 178, 165, 52, 'NAT Gateway', FILL_A, null, 13);
  b += txt(280, 285, 'Security Group: 443 from 0.0.0.0/0', 11.5, 'middle', '400', '#555');

  b += `<rect x="80" y="320" width="400" height="175" rx="5" fill="#eef7ee" stroke="#9ccc9b" stroke-width="1.1"/>`;
  b += txt(100, 342, 'Private subnet  10.0.2.0/24', 12, 'start', '600', '#3d7a3c');
  b += box(105, 358, 165, 50, 'EC2 / ECS', FILL_B, 'resume-service');
  b += box(295, 358, 165, 50, 'EC2 / ECS', FILL_B, 'worker-service');
  b += box(105, 424, 355, 46, 'Auto Scaling Group', FILL_B, 'Security Group: 8081–8082 from ALB SG only');

  b += `<rect x="510" y="140" width="400" height="355" rx="5" fill="#fdf6ec" stroke="#d6b98c" stroke-width="1.1"/>`;
  b += txt(530, 162, 'Private data subnet  10.0.3.0/24', 12, 'start', '600', '#8a6d3b');
  b += cyl(535, 178, 165, 78, 'RDS', 'PostgreSQL Multi-AZ');
  b += cyl(725, 178, 165, 78, 'ElastiCache', 'Redis');
  b += box(535, 285, 355, 52, 'Amazon MSK', FILL_C, 'managed Kafka — resume.tailoring.requested');
  b += box(535, 355, 355, 50, 'S3', FILL_C, 'generated PDF objects (planned)');
  b += box(535, 423, 355, 50, 'Secrets Manager', FILL_C, 'DB password · JWT secret');

  // ALB / NAT down into the app subnet
  b += line(187, 230, 187, 356);
  b += line(377, 230, 377, 356);

  // app tier -> data tier, routed through a shared channel so nothing crosses
  b += `<path d="M 270 372 H 505 V 217 H 533" fill="none" stroke="${LINE}" stroke-width="1.5" marker-end="url(#arrow)"/>`;
  b += `<path d="M 460 396 H 505 V 311 H 533" fill="none" stroke="${LINE}" stroke-width="1.5" marker-end="url(#arrow)"/>`;
  b += `<path d="M 460 358 V 348 H 807 V 258" fill="none" stroke="${LINE}" stroke-width="1.5" marker-end="url(#arrow)"/>`;

  // Shift the region right to make room for the external client
  const shifted = `<g transform="translate(150,0)">${b}</g>`;
  let out = shifted;
  out += box(15, 178, 112, 52, 'Client', FILL_D, null, 13);
  out += txt(71, 252, 'Internet', 11.5, 'middle', '600', '#555');
  out += `<path d="M 127 204 H 255" fill="none" stroke="${LINE}" stroke-width="1.5" marker-end="url(#arrow)"/>`;
  return svg(1130, 560, out);
};

/* ── render ───────────────────────────────────────────────────── */
let n = 0;
for (const [name, fn] of Object.entries(figures)) {
  const s = fn();
  const png = new Resvg(s, { fitTo: { mode: 'width', value: 1600 }, font: { loadSystemFonts: true } })
    .render().asPng();
  fs.writeFileSync(path.join(OUT, name + '.png'), png);
  console.log(`${name}.png  ${Math.round(png.length / 1024)} KB`);
  n++;
}
console.log(`\n${n} figures written to ${OUT}`);
