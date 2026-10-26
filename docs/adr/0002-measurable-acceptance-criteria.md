# ADR-0002: Acceptance criteria carry measures and gate the release

- Status: Accepted, amended 2026-10-26
- Date: 2026-10-25
- Supersedes: nothing

## Amendment, 2026-10-26

The original decision required that **every** acceptance criterion carry a number. That
was wrong in two distinct ways, and the amended rule is narrower and stronger.

**Not every claim should be a number.** Google's SRE guidance warns that too many
indicators makes it impossible to attend to the ones that matter. Some properties are true
or false, and restating "the response names the accepted values" as "0 responses omitting
the accepted values out of 1000" is worse writing pretending to be better engineering. A
`categorical` basis now exists for those, and the checker rejects a categorical criterion
that smuggles a number back in.

**A number is not evidence.** A regular expression that demands a digit cannot distinguish
a threshold calibrated against data from one that was invented and sounded about right.
The original rule converted "is this falsifiable?" into "does this string contain a
numeral?", which is a weaker question wearing the costume of a stronger one. Every
criterion now declares a `Basis`, and the checker publishes how many thresholds are
`provisional`, meaning chosen by judgment and not yet calibrated. At the time of this
amendment that number is nine, and none are `measured`.

**Invariants were undervalued.** A relational property that holds for any parameterization,
such as "no destination below the cutoff outranks one at or above it, for any cutoff",
cannot be satisfied by choosing a convenient number. It is strictly stronger than a
threshold and immune to the criticism above. These are now a first-class basis, and where
a requirement can be expressed as one, it is. Twenty-four of thirty-six criteria are.

See [prior art](../prior-art.md) for what this borrows and from whom. The short version:
this is Tom Gilb's SCALE and METER, INCOSE's requirement-quality rules, and the
bidirectional traceability DO-178C has mandated for decades, and OpenFastTrace already
implements more of it than this repository does.

## Context

The sprint retrospective that closed the originating project reached two conclusions. The
first was that acceptance criteria needed measurable limits. The evidence was specific:
one story asked that unlikely destinations be "filtered out or ranked low" without saying
what made a destination unlikely or what ranking low meant, and no story set a limit on
page load time or on how long a caption could run.

Criteria like that fail in a particular way. They do not look wrong when written. They
pass review, they get built, and the disagreement surfaces later, during testing or after
release, when it is expensive. A criterion that cannot be falsified is a sentence, not a
requirement.

The second conclusion was that decisions need to live somewhere shared rather than in
individual inboxes. The same reasoning applies to the definitions behind a criterion. If
the threshold that separates a suitable destination from an unsuitable one exists only in
one developer's head, every later change is a negotiation.

Wayfinder exists to build that backlog properly. Repeating its central mistake would be a
poor way to start.

## Decision

Every acceptance criterion in `docs/backlog.md` carries three mandatory columns.

**Basis** declares what kind of claim the criterion makes, and for numbers, where the
number came from: `measured`, `sourced`, `provisional`, `invariant` or `categorical`. The
full definitions are in the backlog. `measured` and `sourced` must cite their figure, or
they are `provisional` wearing a better word.

**Measure** states how the criterion can be falsified. Not "fast", but "p95 latency 300 ms
or less at 50 concurrent requests". Not "ranked low", but "0 inversions where a
destination below the cutoff ranks above one at or above it, for any cutoff". A numeric
basis must carry a number; a `categorical` one must not.

**Verified by** names the automated test that checks it, or `Pending M<n>` naming the
milestone that will supply that test. A criterion may be unimplemented. It may not be
unfalsifiable.

`tools/check-criteria.mjs` enforces this on every push. It fails the build when a
criterion has no numeric measure, has no verification reference, duplicates an identifier,
or sits under a story with no stated origin. This is itself criterion AC-00.4, checked by
test `T-CRIT-001`, which makes the rule subject to the rule.

From M6, a failing criterion test blocks deployment rather than merely reporting.

## Consequences

Writing a criterion costs more up front. Choosing 0.25 as the threshold for an unsuitable
destination forces a conversation that "ranked low" lets you postpone. That conversation
is the point, and having it before the code exists is the cheapest time to have it.

Some thresholds will be wrong. A latency budget may prove too tight, a score cut-off too
blunt. Changing a number in version control, with the reason in the commit message, is a
visible decision with history. That is the outcome being bought.

Not everything worth caring about reduces to a number. Taste, clarity and whether an
interface feels trustworthy do not. Those are reviewed by people, and this ADR does not
pretend otherwise. The rule binds the criteria in the backlog, not every judgment in the
project.

The checker is a small amount of bespoke tooling to maintain. It has no dependencies and
reads one file, which is the cost ceiling worth accepting for a rule this central.

## Alternatives considered

**Convention without enforcement.** Write the standard in a contributing guide and trust
it. Rejected because that is close to what the original project did. The standard was
understood by the end, in the retrospective, which is exactly too late.

**A third-party requirements or BDD tool.** Gherkin with a runner would give traceability
out of the box. Rejected for now as heavier than the problem: the backlog is one Markdown
file, and a dependency plus a new syntax is a large tax for what eighty lines of
dependency-free script does. Worth revisiting if the backlog outgrows a single file.

**Coverage thresholds alone.** Simpler, and already standard practice. Rejected as
insufficient: coverage measures whether code ran, not whether the thing the user asked for
is true. AC-00.3 keeps a coverage floor anyway, as a complement rather than a substitute.
