# Prior art

This project enforces that acceptance criteria declare how they can be falsified. That is
not a new idea. It is an old idea from several fields at once, and there is shipping
open-source tooling that does more of it than this repository does.

This document exists because the alternative, implying novelty, would not survive contact
with a reviewer who knows requirements engineering. Knowing exactly what something is and
choosing the smallest version that fits is a better position than inventing it badly.

## The direct ancestor: Gilb's Planguage

Tom Gilb's Planguage is a keyword-based specification language built to make quality
requirements measurable. Its core keywords are **SCALE** (the unit of measure), **METER**
(how that scale is actually measured), and graded target levels: MUST, PLAN, GOAL,
STRETCH, WISH. Gilb's argument for it is the one this project rests on, written decades
earlier: a defined scale of measure lets you numerically quantify an attribute, and
"changing requirements is much easier when requirements are unclear, so a precise
definition is easier to control."

Read this backlog against that and the mapping is exact. `Measure` is SCALE plus a target
level. `Verified by` is METER. Our worked example, turning "ranked low" into "match score
below 0.25 on a 0.0 to 1.0 scale", is a SCALE declaration with a MUST level.

- [How to Quantify Quality: Finding Scales of Measure, ACCU Overload 70](https://accu.org/journals/overload/13/70/gilb_299)
- [Specifying Quality Requirements With Planguage](https://www.modernanalyst.com/Resources/Articles/tabid/115/ID/2926/Specifying-Quality-Requirements-With-Planguage.aspx)

## The rules already have names: INCOSE

The INCOSE Guide to Writing Requirements defines characteristics and rules for requirement
quality. Every check in `tools/check-criteria.mjs` is a pre-existing named rule:

| This project's check | INCOSE rule |
| --- | --- |
| A numeric basis must carry a number | R34, provide specific measurable performance targets |
| Units and ranges must be explicit | R6, use appropriate units; R33, define quantities with a range |
| Reject vague wording | R7, avoid vague terms |
| Reject escape clauses such as "where possible" | R8, avoid escape clauses |
| Verification reference required | C7, Verifiable |
| Duplicate identifiers rejected | A15, unique identifier |
| A section must state where its requirement came from | A3, trace to source |

The original SNHU Travel criterion, "filtered out or ranked low", is a textbook R8
violation. Correcting it into a measurable statement is an R34 correction. Neither of
those observations is original to this project.

INCOSE also anticipated the automation: its rules are written so that a clear check is
possible, and some are explicitly noted as automatable.

- [INCOSE GtWR summary sheet](https://www.incose.org/docs/default-source/working-groups/requirements-wg/guidetowritingrequirements/incose_rwg_gtwr_v4_summary_sheet.pdf)
- [Review of the INCOSE Guide for Writing Requirements](https://www.se-trends.de/en/review-incose-guide-for-writing-requirements/)
- Verifiability as a named requirement-quality attribute predates all of this: IEEE 830-1998, now superseded by [ISO/IEC/IEEE 29148](https://store.sfs.fi/en/ieee-29148-2011).

## Traceability where it is legally mandatory

Safety-critical standards have required bidirectional requirements traceability for
decades. DO-178C (avionics) requires documented traces in both directions between
requirements, code and the test cases that verify them, and treats code that traces to no
requirement as a finding in itself. IEC 62304 (medical devices) requires the same chain
plus change-impact analysis: when an upstream requirement changes, every affected
downstream artifact must be reviewed. ISO 26262 (automotive) mandates traceability across
the development lifecycle.

Two things here are worth borrowing and are named in the gaps section below.

**DO-330 tool qualification** is the precedent for this repository's fixture suite. When a
tool automates a verification process, the tool itself needs requirements and evidence
that it works. `tools/fixtures/invalid-unfalsifiable.md` breaks every rule once and
`valid-control.md` uses every rule correctly, and CI asserts the checker rejects the first
and accepts the second. That is a miniature tool-qualification argument, not an original
technique. The same pattern is standard linter engineering: ESLint's `RuleTester` takes
valid and invalid case arrays.

- [DO-178C overview](https://quality.arc42.org/standards/do-178c)
- [DO-178C requirements traceability](https://www.parasoft.com/learning-center/do-178c/requirements-traceability/)
- [IEC 62304 traceability framework](https://www.dqsglobal.com/en/explore/blog/iec62304-traceability-framework)

## The name for what the CI gate is: a fitness function

Thoughtworks' Technology Radar defines an **architectural fitness function** as "an
objective integrity assessment of some architectural characteristics," notes that it may
encompass existing verification criteria such as unit tests, metrics and monitors, and
frames the value as validating and preserving architectural characteristics in an
automated, continual manner. The term comes from *Building Evolutionary Architectures*.

That is what the criteria gate and the ArchUnit rules in `ArchitectureTest` are. Using the
established term is more accurate than inventing one.

- [Thoughtworks Radar: architectural fitness function](https://www.thoughtworks.com/radar/techniques/architectural-fitness-function)
- [Thoughtworks Radar: lightweight ADRs](https://www.thoughtworks.com/radar/techniques/lightweight-architecture-decision-records)

## Numeric targets, and the warning that comes with them

Google's SRE book is the canonical source for numeric reliability targets, and its
guidance is also the reason this project stopped requiring a number for every criterion.
It warns that choosing too many indicators makes it hard to pay attention to the ones that
matter, that each target should be worth defending in a priority discussion, and against
anchoring a target on current performance. ADR-0002 records how that changed the design.

Martin Fowler's argument against coverage targets applies directly to a solo project:
if you make a level of coverage a target, people will attain it. On a project where one
person writes the code, the threshold and the test, there is no adversary, so the honest
counterweight is declaring which thresholds are uncalibrated rather than pretending none
are.

- [Google SRE: Service Level Objectives](https://sre.google/sre-book/service-level-objectives/)
- [Martin Fowler: TestCoverage](https://martinfowler.com/bliki/TestCoverage.html)

## Tools that already do this, several of them better

**[OpenFastTrace](https://github.com/itsallcode/openfasttrace)** is the closest match and
does more than this repository's script. It parses requirements written in Markdown, takes
tags placed in source code that mark coverage, and traces requirements from specification
through to implementation and tests. Its `Needs: dsn, itest` field is a declarative,
machine-checked version of "names the test that verifies it", and it ships Maven and
Gradle plugins, an HTML tracing report, and version-numbered identifiers so that revising
a requirement invalidates stale coverage.

**[StrictDoc](https://strictdoc.readthedocs.io/)** keeps requirements in plain text under
version control with parent-child links, source traceability markers, cycle detection and
a static HTML traceability graph. **[Doorstop](https://doorstop.readthedocs.io/)** does
requirements management with one YAML file per item and integrity checks across the tree.
**[AWS Duvet](https://github.com/awslabs/duvet)** inverts the direction, quoting
specification text inside code comments and deriving the report, which makes it immune to
the rename problem described below. Commercially, **QVscribe** implements most of the
INCOSE rule set with NLP, including checks it calls "Immeasurable Quantification" and
"Not Testable", which is this project's first rule sold as a product.

### Why a script instead of adopting one of these

Three reasons, in order of weight.

The backlog is one Markdown file with 36 criteria. OpenFastTrace's model earns its keep
across documents and repositories; here it would be more configuration than requirement.

The rules this project needs are not quite OFT's. The `Basis` column, which distinguishes
a calibrated threshold from an invented one, does not correspond to anything in the tools
surveyed, and it is the check that most directly answers the strongest criticism of the
approach.

And this category has a history of vendor abandonment. SpecFlow's open-source project
ended in December 2024 and its repositories were deleted; SpecFlow+ LivingDoc, the
traceability component, was never open source and could not be forked. FIT's core was
described by a former project lead as intentionally resistant to change. Concordion has
not been pushed since May 2025. Against that record, 200 dependency-free lines is a
defensible position rather than a shortcut.

- [SpecFlow end of life](https://reqnroll.net/news/2025/01/specflow-end-of-life-has-been-announced/)

## What this is not: Specification by Example and BDD

A reviewer may reach for Specification by Example or BDD, and the comparison does not
quite hold. Those traditions centre on the **concrete example** as a shared artifact that
a non-technical stakeholder can read and ideally write. This project centres on the
**quantified threshold and the invariant**, which is Gilb's tradition, not Gojko Adzic's.

The decision not to adopt Gherkin is deliberate and the evidence supports it:

- Dan North, who created BDD, has said that Cucumber-like tools are "overkill for almost any use case (but fantastic when they aren't)" ([20 Years of BDD](https://www.phpconference.co.uk/videos/20-years-of-bdd-daniel-terhorst-north)).
- Aslak Hellesoy, who wrote Cucumber: "If all you need is a testing tool for driving a mouse and a keyboard, don't use Cucumber."
- James Shore, a former FIT project lead, on why the table-based executable-specification category declined: "Customers don't generate the documents, and that was the whole idea" ([Is Fit Dead?](https://www.jamesshore.com/v2/presentations/2009/is-fit-dead-conversation-summary)).
- Binamungu et al. found in a practitioner survey that large BDD suites "can be hard to understand, maintain and extend" ([Characterising the Quality of BDD Specifications](https://pmc.ncbi.nlm.nih.gov/articles/PMC7251619/)).

Shore's point is the important one, and this project avoids it by not making the claim he
is criticising. Nothing here assumes a non-technical stakeholder writes or reads the
backlog. The gate exists for the author's own discipline, which is a narrower and more
defensible claim.

## Where the merit actually is

Not in the idea that criteria should be measurable. In three narrower places.

**The invariants.** AC-03.2 requires zero inversions where a destination below the cutoff
ranks above one at or above it, for any cutoff. That is a relational property, not a
threshold: it cannot be satisfied by choosing a convenient number, which is the single
strongest criticism of quantified-requirement schemes. Twenty-four of this backlog's
thirty-six criteria are invariants, and that ratio is the point rather than an accident.

**The declared basis.** A gate that only checks for the presence of a numeral cannot tell
a calibrated threshold from a guess. Publishing that nine thresholds are provisional, and
zero are measured, makes the backlog an honest instrument rather than a confident one.

**The qualification suite.** Proving the gate fails is cheap, rare in practice, and the
difference between verification and ceremony.

## Gaps: what the prior art does that this does not

Recorded honestly rather than discovered by a reviewer.

- **Traceability is one-directional.** The backlog names a test; the test does not name the criterion. DO-178C treats an artifact that traces to nothing as a finding, and this project cannot currently detect an orphan test.
- **Identifiers are not versioned.** OpenFastTrace's `req~name~1` form means changing a requirement invalidates existing coverage automatically. A bare duplicate check is the weaker version.
- **No change-impact analysis.** IEC 62304 requires that an upstream change forces downstream review. Detecting that a criterion's text changed while its test did not is cheap in git and is probably the most useful thing still missing.
- **The link is rename-fragile.** Renaming a test silently makes the backlog lie. Duvet's approach, putting the reference in the code where the refactor happens, is the more robust direction.
