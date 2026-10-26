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

import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, join, relative, resolve } from 'node:path';

const HERE = dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = join(HERE, '..');

// Defaults to the real backlog. The optional argument exists so the checker can be run
// against a deliberately broken fixture, because a gate nobody has watched fail is not
// yet known to be a gate.
const BACKLOG = process.argv[2]
  ? resolve(process.cwd(), process.argv[2])
  : join(REPO_ROOT, 'docs', 'backlog.md');

// What to print in diagnostics: repo-relative where possible, so the output is clickable
// from the repository root and honest about which file was actually read.
const DISPLAY = relative(REPO_ROOT, BACKLOG).split('\\').join('/') || BACKLOG;

const ID_PATTERN = /^AC-(\d{2})\.\d+$/;
const VERIFIED_PATTERN = /^(Pending M\d+|T-[A-Z0-9]+-\d+)$/;
// Markdown permits up to three spaces of indentation before a heading, and a backlog
// excerpt embedded in a YAML block can arrive indented, so do not anchor hard at column 0.
const HEADING_PATTERN = /^\s{0,3}##\s+(.+?)\s*$/;
const STORY_NUMBER_PATTERN = /^US-(\d+)/;
const ORIGIN_PATTERN = /^Origin:\s*\S/;

// A section may declare where its open question is being tracked. Criteria that promise an
// issue exists are checked against this rather than taken on trust, so the disclosure
// cannot be quietly deleted while the criterion still claims to be satisfied.
const TRACKING_PATTERN = /^Tracking:\s*(https?:\/\/\S+)\s*$/;

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
      const [id, criterion, measure, verified] = cells(line);
      current.criteria.push({
        id, criterion, measure, verified,
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

    // The whole point of ADR-0002: a criterion without a number cannot be falsified.
    if (!c.measure) {
      problems.push({ line: c.line, id: c.id, message: 'measure is empty' });
    } else if (!/\d/.test(c.measure)) {
      problems.push({
        line: c.line, id: c.id,
        message: `measure states no number, so nothing can falsify it: "${c.measure}"`,
      });
    }

    // A criterion that asserts an issue exists has to point at one.
    if (/\bissues?\b/i.test(c.measure ?? '') && !c.section.tracking) {
      problems.push({
        line: c.line, id: c.id,
        message: 'measure promises an issue, but the section has no "Tracking: <url>" line',
      });
    }

    if (!VERIFIED_PATTERN.test(c.verified ?? '')) {
      problems.push({
        line: c.line, id: c.id,
        message: `"Verified by" must name a test such as T-RANK-001 or a milestone such as Pending M4, found "${c.verified ?? ''}"`,
      });
    }
  }

  return { problems, all };
}

function report(all) {
  const verified = all.filter((c) => /^T-/.test(c.verified ?? ''));
  const pending = all.filter((c) => /^Pending/.test(c.verified ?? ''));

  console.log(`criteria:  ${all.length}`);
  console.log(`verified:  ${verified.length}`);
  console.log(`pending:   ${pending.length}`);

  const byMilestone = new Map();
  for (const c of pending) {
    const m = c.verified.replace('Pending ', '');
    byMilestone.set(m, (byMilestone.get(m) ?? 0) + 1);
  }
  const order = [...byMilestone.keys()].sort(
    (a, b) => Number(a.slice(1)) - Number(b.slice(1)),
  );
  for (const m of order) {
    console.log(`  ${m}: ${byMilestone.get(m)} awaiting a test`);
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

  if (problems.length > 0) {
    console.error(`\n${problems.length} problem(s) in ${DISPLAY}:\n`);
    for (const p of problems.sort((a, b) => a.line - b.line)) {
      console.error(`  ${DISPLAY}:${p.line}  ${p.id}  ${p.message}`);
    }
    console.error('\nSee docs/adr/0002-measurable-acceptance-criteria.md for the rule.');
    process.exit(1);
  }

  console.log('\nT-CRIT-001 passed: every criterion has a numeric measure and a verification reference.');
}

await main();
