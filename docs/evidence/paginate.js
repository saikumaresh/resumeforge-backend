/*
 * Reads the exported PDF and reports the page on which every chapter,
 * table caption and figure caption appears. Output feeds the static
 * Table of Contents and the two page-number columns in thesis.js.
 *
 *   node paginate.js ~/Downloads/<exported>.pdf
 */
const fs = require('fs');

const CHAPTERS = [
  'Certification', 'DECLARATION', 'ACKNOWLEDGMENT', 'Table of Contents',
  'List of Tables', 'List of Figures', 'Applied Software Project', 'Abstract',
  'Project Description', 'Requirement Gathering', 'Class Diagrams',
  'Database Schema Design', 'Feature Development Process', 'Deployment Flow',
  'Technologies Used', 'Conclusion', 'References',
];

(async () => {
  const file = process.argv[2];
  if (!file || !fs.existsSync(file)) {
    console.error('usage: node paginate.js <exported.pdf>');
    process.exit(1);
  }

  const pdfjs = await import('pdfjs-dist/legacy/build/pdf.mjs');
  const doc = await pdfjs.getDocument({
    data: new Uint8Array(fs.readFileSync(file)),
    useSystemFonts: true,
  }).promise;

  console.log(`total pages: ${doc.numPages}\n`);

  const pages = [];
  for (let n = 1; n <= doc.numPages; n++) {
    const page = await doc.getPage(n);
    const content = await page.getTextContent();
    // join with spaces; captions and headings are what we're matching
    pages.push(content.items.map((i) => i.str).join(' ').replace(/\s+/g, ' '));
  }

  const firstPageContaining = (needle, from = 0) => {
    for (let i = from; i < pages.length; i++) {
      if (pages[i].includes(needle)) return i + 1;
    }
    return null;
  };

  console.log('=== CHAPTERS ===');
  const chapterPages = {};
  let cursor = 0;
  for (const c of CHAPTERS) {
    const p = firstPageContaining(c, cursor);
    chapterPages[c] = p;
    if (p) cursor = p - 1;           // chapters appear in order
    console.log(`  ${String(p ?? '?').padStart(3)}  ${c}`);
  }

  console.log('\n=== TABLES ===');
  const tablePages = {};
  for (let i = 0; i < pages.length; i++) {
    for (const m of pages[i].matchAll(/Table (\d+\.\d+):/g)) {
      if (!(m[1] in tablePages)) tablePages[m[1]] = i + 1;
    }
  }
  Object.entries(tablePages).sort().forEach(([k, v]) => console.log(`  ${String(v).padStart(3)}  Table ${k}`));

  console.log('\n=== FIGURES ===');
  const figurePages = {};
  for (let i = 0; i < pages.length; i++) {
    for (const m of pages[i].matchAll(/Figure (\d+\.\d+):/g)) {
      if (!(m[1] in figurePages)) figurePages[m[1]] = i + 1;
    }
  }
  Object.entries(figurePages).sort().forEach(([k, v]) => console.log(`  ${String(v).padStart(3)}  Figure ${k}`));

  fs.writeFileSync('pagination.json', JSON.stringify(
    { totalPages: doc.numPages, chapters: chapterPages, tables: tablePages, figures: figurePages }, null, 2));
  console.log('\nwrote pagination.json');
})().catch((e) => { console.error('FAILED:', e.message); process.exit(1); });
