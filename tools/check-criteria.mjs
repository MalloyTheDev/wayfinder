#!/usr/bin/env node
/*
 * Enforces the rule set out in ADR-0002: every acceptance criterion in the backlog
 * carries a measure containing a concrete number, and names the test that checks it or
 * the milestone that will supply one.
 *
 * This is criterion AC-00.4, and this script is test T-CRIT-001, which makes the rule
 * subject to itself.
 *
 * No dependencies on purpose. It reads one Markdown file; a package tree would cost more
 * to maintain than the checking is worth.
 *
 * Exit code 0 when every criterion passes, 1 otherwise.
 */

import { readFile, readdir } from 'node:fs/promises';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join, relative, resolve } from 'node:path';

const HERE = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = join(HERE, '..');

// Defaults to the real backlog. The optional argument exists so the checker can be run
// against a deliberately broken fixture, because a gate nobody has watched fail is not
// yet known to be a gate.
/**
 * Arguments: an optional backlog path, and an optional `--changed-since <ref>`.
 *
 * The path exists so the checker can be run against a deliberately broken fixture,
 * because a gate nobody has watched fail is not yet known to be a gate.
 */
const ARGS = process.argv.slice(2);
const SINCE_INDEX = ARGS.indexOf('--changed-since');
const CHANGED_SINCE = SINCE_INDEX >= 0 ? ARGS[SINCE_INDEX + 1] : null;
// The guard on SINCE_INDEX matters: without it, an absent flag makes SINCE_INDEX -1 and
// the "skip the flag's value" rule silently swallows the first positional argument, so
// a fixture path would be ignored and the real backlog checked instead.
const SINCE_VALUE_INDEX = SINCE_INDEX >= 0 ? SINCE_INDEX + 1 : -1;
const POSITIONAL = ARGS.filter((arg, i) =>
  !arg.startsWith('--') && i !== SINCE_VALUE_INDEX);

const USING_REAL_BACKLOG = POSITIONAL.length === 0;
const BACKLOG = USING_REAL_BACKLOG
  ? join(REPO_ROOT, 'docs', 'backlog.md')
  : resolve(process.cwd(), POSITIONAL[0]);

// What to print in diagnostics: repo-relative where possible, so the output is clickable
// from the repository root and honest about which file was actually read.
const DISPLAY = relative(REPO_ROOT, BACKLOG).split('\\').join('/') || BACKLOG;

const ID_PATTERN = /^AC-(\d{2})\.\d+$/;
const TEST_ID_PATTERN = /T-[A-Z0-9]+-\d+/g;
const ONE_TEST_ID = /^T-[A-Z0-9]+-\d+$/;

/**
 * Where tests live, for the traceability checks below.
 *
 * Fixtures are excluded deliberately: they contain invented identifiers used to prove the
 * checker fails when it should, and treating those as real tests would make the fixture
 * suite impossible to maintain.
 */
const TEST_ROOTS = ['api/src/test', 'tools'];
const TEST_EXCLUDE = ['tools/fixtures'];
const TEST_EXTENSIONS = ['.java', '.mjs', '.js'];

/**
 * What kind of claim a criterion makes, and for numbers, where the number came from.
 *
 * `requiresNumber` is false for exactly one basis. Mandating a number everywhere was the
 * original design, and it was wrong: some properties are true or false, and dressing one
 * up as "0 malformed rows out of 1000" is worse than saying it parses. ADR-0002 records
 * why that changed.
 */
const BASIS = {
  measured:    { requiresNumber: true,  requiresCitation: true,  calibrated: true },
  sourced:     { requiresNumber: true,  requiresCitation: true,  calibrated: true },
  provisional: { requiresNumber: true,  requiresCitation: false, calibrated: false },
  invariant:   { requiresNumber: true,  requiresCitation: false, calibrated: true },
  categorical: { requiresNumber: false, requiresCitation: false, calibrated: true },
};

/** A citation is a markdown link or a bare URL somewhere in the row. */
const CITATION_PATTERN = /\[[^\]]+\]\([^)]+\)|https?:\/\/\S+/;
// Markdown permits up to three spaces of indentation before a heading, and a backlog
// excerpt embedded in a YAML block can arrive indented, so do not anchor hard at column 0.
const HEADING_PATTERN = /^\s{0,3}##\s+(.+?)\s*$/;
const STORY_NUMBER_PATTERN = /^US-(\d+)/;
const ORIGIN_PATTERN = /^Origin:\s*\S/;

// A section may declare where its open question is being tracked. Criteria that promise an
// issue exists are checked against this rather than taken on trust, so the disclosure
// cannot be quietly deleted while the criterion still claims to be satisfied.
const TRACKING_PATTERN = /^Tracking:\s*(https?:\/\/\S+)\s*$/;

/**
 * A criterion is verified by a milestone it is waiting on, or by one or more tests.
 *
 * More than one is normal and should not be discouraged: AC-01.3 is checked in the domain,
 * against the database, and at the HTTP boundary, and naming only one of those three would
 * leave the other two looking like tests nothing asked for.
 */
function parseVerifiedBy(value) {
  const text = (value ?? '').trim();
  if (/^Pending M\d+$/.test(text)) {
    return { kind: 'pending', tests: [] };
  }
  const parts = text.split(',').map((part) => part.trim()).filter(Boolean);
  if (parts.length > 0 && parts.every((part) => ONE_TEST_ID.test(part))) {
    return { kind: 'tests', tests: parts };
  }
  return { kind: 'invalid', tests: [] };
}

/** Split a Markdown table row into trimmed cells, dropping the empty edges. */
function cells(line) {
  const parts = line.split('|');
  if (parts.length >= 2 && parts[0].trim() === '') parts.shift();
  if (parts.length >= 1 && parts[parts.length - 1].trim() === '') parts.pop();
  return parts.map((c) => c.trim());
}

/**
 * Walk the backlog once, collecting criteria and the section each belongs to.
 * Sections are tracked so a criterion can be blamed on the story that owns it, and so a
 * story that forgot to state where its requirement came from can be caught.
 */
function parse(text) {
  const sections = [];
  let current = null;

  text.split(/\r?\n/).forEach((line, index) => {
    const heading = HEADING_PATTERN.exec(line);
    if (heading) {
      current = {
        title: heading[1], line: index + 1, hasOrigin: false, tracking: null, criteria: [],
      };
      sections.push(current);
      return;
    }
    if (!current) return;

    if (ORIGIN_PATTERN.test(line.trim())) {
      current.hasOrigin = true;
      return;
    }

    const tracking = TRACKING_PATTERN.exec(line.trim());
    if (tracking) {
      current.tracking = tracking[1];
      return;
    }

    if (line.trimStart().startsWith('| AC-')) {
      const [id, criterion, measure, basis, verified] = cells(line);
      current.criteria.push({
        id, criterion, measure, basis, verified,
        line: index + 1,
        section: current,
      });
    }
  });

  return sections;
}

function check(sections) {
  const problems = [];
  const seen = new Map();
  const all = sections.flatMap((s) => s.criteria);

  for (const section of sections) {
    if (section.criteria.length > 0 && !section.hasOrigin) {
      problems.push({
        line: section.line,
        id: section.title,
        message: 'section defines criteria but states no Origin for the requirement',
      });
    }
  }

  for (const c of all) {
    const idMatch = ID_PATTERN.exec(c.id ?? '');

    if (!idMatch) {
      problems.push({ line: c.line, id: c.id || '(blank)', message: 'identifier must look like AC-01.2' });
    } else {
      const expected = STORY_NUMBER_PATTERN.exec(c.section.title);
      if (expected) {
        const want = expected[1].padStart(2, '0');
        if (idMatch[1] !== want) {
          problems.push({
            line: c.line, id: c.id,
            message: `identifier belongs to story ${idMatch[1]} but sits under ${c.section.title}`,
          });
        }
      }

      const prior = seen.get(c.id);
      if (prior !== undefined) {
        problems.push({ line: c.line, id: c.id, message: `duplicate identifier, already used on line ${prior}` });
      } else {
        seen.set(c.id, c.line);
      }
    }

    if (!c.criterion) {
      problems.push({ line: c.line, id: c.id, message: 'criterion text is empty' });
    }

    const basis = BASIS[c.basis ?? ''];

    if (!basis) {
      problems.push({
        line: c.line, id: c.id,
        message: `basis must be one of ${Object.keys(BASIS).join(', ')}, found "${c.basis ?? ''}"`,
      });
    }

    if (!c.measure) {
      problems.push({ line: c.line, id: c.id, message: 'measure is empty' });
    } else if (basis?.requiresNumber && !/\d/.test(c.measure)) {
      // The core of ADR-0002, now scoped: a criterion that claims to be numeric must
      // actually carry a number. A categorical one is exempt by declaration, not by
      // oversight, which is the difference between a considered exception and a hole.
      problems.push({
        line: c.line, id: c.id,
        message: `basis "${c.basis}" promises a number but the measure has none: "${c.measure}"`,
      });
    } else if (basis && !basis.requiresNumber && /\d/.test(c.measure)) {
      problems.push({
        line: c.line, id: c.id,
        message: `basis "categorical" means the claim is true or false, but the measure `
          + `states a number. Use a numeric basis, or drop the number: "${c.measure}"`,
      });
    }

    // A threshold said to come from measurement or an external standard has to show its
    // working. Without this, "measured" is just a more confident way of saying "invented".
    if (basis?.requiresCitation && !CITATION_PATTERN.test(`${c.criterion} ${c.measure}`)) {
      problems.push({
        line: c.line, id: c.id,
        message: `basis "${c.basis}" claims the figure has a source, but the row cites none`,
      });
    }

    // A criterion that asserts an issue exists has to point at one.
    if (/\bissues?\b/i.test(c.measure ?? '') && !c.section.tracking) {
      problems.push({
        line: c.line, id: c.id,
        message: 'measure promises an issue, but the section has no "Tracking: <url>" line',
      });
    }

    const verification = parseVerifiedBy(c.verified);
    if (verification.kind === 'invalid') {
      problems.push({
        line: c.line, id: c.id,
        message: `"Verified by" must name one or more tests such as T-RANK-001, `
          + `or a milestone such as Pending M4, found "${c.verified ?? ''}"`,
      });
    }
    c.tests = verification.tests;
  }

  return { problems, all };
}

/**
 * Every test identifier that appears in the test sources, mapped to the file it lives in.
 *
 * Scanning the sources rather than trusting the backlog is the whole point: the backlog
 * is the thing being checked, so it cannot also be the evidence.
 */
async function scanTestIds() {
  const index = new Map();

  async function walk(directory) {
    let entries;
    try {
      entries = await readdir(directory, { withFileTypes: true });
    } catch {
      return; // A root that does not exist yet is not an error.
    }
    for (const entry of entries) {
      const full = join(directory, entry.name);
      const rel = relative(REPO_ROOT, full).split('\\').join('/');
      if (TEST_EXCLUDE.some((skip) => rel === skip || rel.startsWith(`${skip}/`))) {
        continue;
      }
      if (entry.isDirectory()) {
        await walk(full);
      } else if (TEST_EXTENSIONS.some((ext) => entry.name.endsWith(ext))) {
        const text = await readFile(full, 'utf8');
        for (const match of text.matchAll(TEST_ID_PATTERN)) {
          if (!index.has(match[0])) {
            index.set(match[0], rel);
          }
        }
      }
    }
  }

  for (const root of TEST_ROOTS) {
    await walk(join(REPO_ROOT, root));
  }
  return index;
}

/**
 * Traceability, in both directions.
 *
 * DO-178C treats an artifact that traces to nothing as a finding in its own right, not
 * merely as untidiness, and the reasoning carries over. A criterion naming a test that
 * does not exist is a backlog telling a comfortable lie. A test claiming an identifier
 * that no criterion asks for is work whose purpose nobody wrote down, and it is also how
 * a rename quietly severs the link: the old identifier vanishes from the sources while
 * the backlog goes on citing it.
 */
function checkTraceability(all, testIndex) {
  const problems = [];
  const claimed = new Set();

  for (const c of all) {
    for (const testId of c.tests ?? []) {
      claimed.add(testId);
      if (!testIndex.has(testId)) {
        problems.push({
          line: c.line, id: c.id,
          message: `names ${testId}, which appears in no test source. `
            + `Either the test was renamed or it was never written.`,
        });
      }
    }
  }

  for (const [testId, file] of [...testIndex.entries()].sort()) {
    if (!claimed.has(testId)) {
      problems.push({
        line: 0, id: testId,
        message: `is declared in ${file} but no criterion claims it. `
          + `Add it to a criterion's "Verified by", or stop labelling it.`,
      });
    }
  }

  return problems;
}

/**
 * Change-impact analysis, borrowed from IEC 62304 clause 7.3.3.
 *
 * The rule there is that a change upstream forces review of everything downstream. The
 * failure it prevents is the one that quietly ruins living documentation: a criterion is
 * reworded, its test is not revisited, and the two drift apart while the build stays
 * green and the backlog keeps claiming the test proves something it no longer proves.
 *
 * This does not demand the test change. Rewording a criterion for clarity is legitimate.
 * It demands that somebody say so, by adding a `Reviewed-criteria: AC-01.4` trailer to a
 * commit message in the range. The cost of the rule is one line; its value is that the
 * acknowledgement is explicit and recorded rather than assumed.
 */
function checkChangeImpact(all, testIndex, sinceRef) {
  const git = (args) =>
    execFileSync('git', args, { cwd: REPO_ROOT, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });

  try {
    git(['rev-parse', '--verify', `${sinceRef}^{commit}`]);
  } catch {
    console.log(`\nchange impact: skipped, cannot resolve "${sinceRef}" in this checkout`);
    return [];
  }

  const backlogPath = relative(REPO_ROOT, BACKLOG).split('\\').join('/');
  const diff = git(['diff', '--unified=0', sinceRef, '--', backlogPath]);

  const touched = new Set();
  for (const line of diff.split('\n')) {
    if (!/^[+-]\s*\|\s*AC-/.test(line)) continue;
    const match = /AC-\d{2}\.\d+/.exec(line);
    if (match) touched.add(match[0]);
  }

  if (touched.size === 0) {
    console.log('\nchange impact: no criterion text changed in this range');
    return [];
  }

  const changedFiles = new Set(
    git(['diff', '--name-only', sinceRef]).split('\n').map((f) => f.trim()).filter(Boolean));
  const messages = git(['log', '--format=%B', `${sinceRef}..HEAD`]);

  const problems = [];
  const reviewed = [];

  for (const id of [...touched].sort()) {
    const criterion = all.find((c) => c.id === id);
    // A criterion that was deleted outright has nothing left to drift from.
    if (!criterion) continue;

    const acknowledged =
      new RegExp(`Reviewed-criteria:[^\\n]*\\b${id.replace('.', '\\.')}\\b`).test(messages);
    if (acknowledged) {
      reviewed.push(id);
      continue;
    }

    const stale = (criterion.tests ?? []).filter((testId) => {
      const file = testIndex.get(testId);
      return file && !changedFiles.has(file);
    });

    if (stale.length > 0) {
      problems.push({
        line: criterion.line, id,
        message: `text changed but ${stale.join(', ')} did not, and no commit in this range `
          + `carries "Reviewed-criteria: ${id}". Revisit the test, or record that you did.`,
      });
    }
  }

  console.log(`\nchange impact: ${touched.size} criterion row(s) changed since ${sinceRef}`
    + (reviewed.length ? `, ${reviewed.length} acknowledged (${reviewed.join(', ')})` : ''));
  return problems;
}

function report(all) {
  const verified = all.filter((c) => /^T-/.test(c.verified ?? ''));
  const pending = all.filter((c) => /^Pending/.test(c.verified ?? ''));

  console.log(`criteria:  ${all.length}`);
  console.log(`verified:  ${verified.length}`);
  console.log(`pending:   ${pending.length}`);

  // Printed on every run because it is the number most worth being embarrassed by. A
  // provisional threshold is a number someone chose and nobody has checked against
  // reality yet; publishing the count is what stops it quietly becoming settled fact.
  console.log('');
  console.log('basis:');
  for (const name of Object.keys(BASIS)) {
    const n = all.filter((c) => c.basis === name).length;
    const note = name === 'provisional' ? '  <- chosen by judgment, not yet calibrated' : '';
    console.log(`  ${name.padEnd(12)} ${String(n).padStart(3)}${note}`);
  }

  const byMilestone = new Map();
  for (const c of pending) {
    const m = c.verified.replace('Pending ', '');
    byMilestone.set(m, (byMilestone.get(m) ?? 0) + 1);
  }
  const order = [...byMilestone.keys()].sort(
    (a, b) => Number(a.slice(1)) - Number(b.slice(1)),
  );
  if (order.length > 0) {
    console.log('');
    console.log('awaiting a test:');
    for (const m of order) {
      console.log(`  ${m.padEnd(12)} ${String(byMilestone.get(m)).padStart(3)}`);
    }
  }
}

async function main() {
  let text;
  try {
    text = await readFile(BACKLOG, 'utf8');
  } catch (error) {
    console.error(`cannot read ${DISPLAY}: ${error.message}`);
    process.exit(1);
  }

  const sections = parse(text);
  const { problems, all } = check(sections);

  if (all.length === 0) {
    console.error('no acceptance criteria found, which means the backlog or this parser is broken');
    process.exit(1);
  }

  report(all);

  // Traceability and change impact are about this repository's real backlog and its real
  // tests. Running them against a fixture, whose identifiers are invented to exercise the
  // other rules, would report nothing but noise.
  if (USING_REAL_BACKLOG) {
    const testIndex = await scanTestIds();
    console.log(`\ntest identifiers found in sources: ${testIndex.size}`);
    problems.push(...checkTraceability(all, testIndex));

    if (CHANGED_SINCE) {
      problems.push(...checkChangeImpact(all, testIndex, CHANGED_SINCE));
    }
  }

  if (problems.length > 0) {
    console.error(`\n${problems.length} problem(s) in ${DISPLAY}:\n`);
    for (const p of problems.sort((a, b) => a.line - b.line)) {
      console.error(`  ${DISPLAY}:${p.line}  ${p.id}  ${p.message}`);
    }
    console.error('\nSee docs/adr/0002-measurable-acceptance-criteria.md for the rule.');
    process.exit(1);
  }

  console.log('\nT-CRIT-001 passed: every criterion declares a basis, a measure consistent '
    + 'with it, and a verification reference.');
}

await main();
