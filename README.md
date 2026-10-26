# Wayfinder

[![CI](https://github.com/MalloyTheDev/wayfinder/actions/workflows/ci.yml/badge.svg)](https://github.com/MalloyTheDev/wayfinder/actions/workflows/ci.yml)

A travel destination platform built from a real product backlog, where every acceptance
criterion carries a number and names the test that proves it.

## Why this exists

The backlog this implements came out of a Scrum pilot run during CS 250. The project
produced user stories, acceptance criteria, test cases and a sprint retrospective, and
then shipped a single-class Java Swing slide show as a stand-in for the entire product.
The real application was never built.

The retrospective that closed it reached a specific conclusion: the acceptance criteria
needed measurable limits. One story asked that unlikely destinations be "filtered out or
ranked low" without defining either term. No story set a limit on page load time or on how
long a caption could run. Criteria like that read fine, pass review, get built, and then
produce an argument during testing.

Wayfinder is that conclusion applied, with one correction the conclusion itself did not
make: **the strongest criteria are not thresholds.** They are invariants, relational
properties that hold for any parameterization.

"Match score below 0.25" is a number somebody chose. "No destination below the cutoff
outranks one at or above it, **for any cutoff**" cannot be satisfied by choosing a
convenient number, which is the single strongest objection to quantified requirements.
Thirty-one of this backlog's forty-seven criteria are invariants, and that ratio is the
design rather than an accident.

Where a number genuinely is the honest form, [the backlog](docs/backlog.md) declares where
it came from: `measured`, `sourced`, or `provisional`. **Nine thresholds are provisional**,
meaning chosen by judgment and not yet calibrated. Four are `measured`: two from the ranking
evaluation below and two from the deployment measurements. The gate prints that breakdown on every run, because a
number nobody has checked should not get to sit alongside ones that were.

These are [architectural fitness functions](https://www.thoughtworks.com/radar/techniques/architectural-fitness-function),
and none of it is new. It is Tom Gilb's SCALE and METER, INCOSE's requirement-quality
rules, and the bidirectional traceability DO-178C has mandated for decades.
[Prior art](docs/prior-art.md) records what this borrows, from whom, what OpenFastTrace
already does better, and what is still missing here.

Originating coursework and artifacts:
[CS250-SNHU-Travel-Scrum-Pilot](https://github.com/MalloyTheDev/CS250-SNHU-Travel-Scrum-Pilot).

## Status

**Milestone 1.** The catalog is real: a domain model, a migrated schema, 30 seeded
destinations and a read API over them, tested against Postgres. Nothing is ranked yet and
nothing is deployed yet, and this README will not pretend otherwise.

| Milestone | Delivers | State |
| --- | --- | --- |
| M0 | Backlog with measurable criteria, ADRs, criteria gate | Done |
| M1 | Domain model, migrations, seeded reference dataset, destinations API | Done |
| M2 | Ranked top five and landing page, US-01 end to end, deployed | Next |
| M3 | Accounts and preference persistence, US-02 | Planned |
| M4 | Personalized ranking and type filters, US-03 and US-04 | Planned |
| M5 | Hot deals, US-05 | Planned |
| M6 | Traceability report, observability, budgets enforced as gates | Planned |
| M7 | Demo data, documentation, release | Planned |

47 acceptance criteria are defined: 31 invariants, 9 provisional thresholds, 3 categorical,
4 measured. 19 are verified today; the rest name the milestone that will verify them. The
gate prints both ratios on every run, so verification debt and uncalibrated numbers are
visible rather than discovered later.

## Deployment: prepared, not performed

The container, the migration step and the startup measurements are verified locally. The
Fly configuration and the deploy workflow are written and reasoned about but have never
run, because deploying needs an account, a database and a payment method.
[docs/deployment.md](docs/deployment.md) states precisely which parts are proven and which
are not.

What is measured, median of three runs each way:

| Environment | Without AOT cache | With AOT cache | Reduction |
| --- | --- | --- | --- |
| 512 MB / 1 CPU container | 9.10 s | 4.22 s | **53.6%** |
| Unconstrained host | 4.17 s | 2.34 s | 43.8% |

The cache is built inside the image against the exact jar and JVM that will run it, since
it is invalid for any other combination. It costs 106 MB of image and buys faster
blue-green cutovers rather than faster requests, which is a trade worth naming.

The container also runs unprivileged, settles at 206 MB of a 512 MB limit, and its
migration step exits non-zero against an unreachable database, which is what stops a bad
deploy.

## Ranking quality, measured

The personalized ranking is evaluated against hand-labelled relevance judgments using
NDCG@10, and the result gates the build. This is the one number in the project that can
genuinely get worse when the code changes, which is why it exists: an assertion that an
endpoint returns HTTP 200 cannot regress in a way that means anything.

| Persona | Ranked | Unranked baseline | Gain |
| --- | --- | --- | --- |
| budget-wellness | 0.9656 | 0.1908 | +0.7747 |
| luxury-beach-asia | 0.9483 | 0.1142 | +0.8341 |
| adventure-moderate | 0.8895 | 0.3323 | +0.5572 |
| culture-europe | 0.9842 | 0.3423 | +0.6419 |
| **Mean** | **0.9469** | **0.2449** | **+0.7020** |

The baseline is the catalog in its natural order, which is what the unpersonalized
endpoint returns today. The build fails if the mean drops below 0.94, or if any single
persona stops beating its own baseline, because a mean can hide a scorer that is excellent
for one traveller and worse than useless for another.

**The judgments are editorial, not generated.** Several labels deliberately disagree with
what the current weights produce: a budget Himalayan trek is labelled relevant to a
wellness traveller, and a luxury onsen is labelled relevant to someone who asked for
beaches. Ground truth derived from the formula under test would report a perfect score no
matter how bad the ranking was, so those disagreements are the entire source of signal.
They are also why `adventure-moderate` scores lowest: the scorer treats vacation type as
binary and cannot see that a rainforest river journey is adventure travel in all but
label.

## What is here now

```
docs/backlog.md                 36 criteria, each declaring a basis and a verification reference
docs/prior-art.md               what this borrows, from whom, and what is still missing
docs/deployment.md              the intended stack, what is verified, and what is not
api/Dockerfile                  three stages; the AOT cache is built against the jar it will run
docs/adr/0001-stack-selection.md            Spring Boot, Postgres, React, and why
docs/adr/0002-measurable-acceptance-criteria.md  the rule, why it is enforced, and how it was wrong
tools/check-criteria.mjs        the gate, T-CRIT-001, no dependencies
tools/fixtures/                 the gate's own qualification suite, one case per rule

api/src/main/java/.../domain/          Destination and its invariants, free of Spring and JPA
api/src/main/java/.../persistence/     the JPA adapter behind the domain's catalog port
api/src/main/java/.../web/             the read API and its problem responses
api/src/main/resources/db/migration/   schema, then 30 seeded reference destinations
api/src/main/java/.../domain/DestinationScorer.java   the ranking, framework-free and explainable
api/src/test/resources/eval/           hand-labelled personas and relevance judgments
api/src/test/java/.../eval/            NDCG and the quality gate
api/src/test/                          69 tests, including ArchUnit rules and real Postgres
```

## Running the gate

Requires Node 22 or newer. Nothing to install.

```bash
node tools/check-criteria.mjs
```

It prints the criteria count, the breakdown by basis, how many are verified against how
many are pending, and the milestone each pending one is waiting on. It exits non-zero when
a criterion declares no recognised basis, states a measure inconsistent with that basis,
claims a measured or sourced figure without citing it, has no verification reference, has
a duplicate or mismatched identifier, promises an issue it does not link, or sits under a
story that never says where the requirement came from.

It also checks traceability in **both** directions. A criterion naming a test that does
not exist fails the build, which is what catches a rename silently making the backlog
lie. So does a test labelled with an identifier no criterion claims, following DO-178C's
treatment of an artifact that traces to nothing as a finding in its own right. Turning
that on surfaced thirteen orphan tests, one of which was a whole capability the API had
built and the backlog had never mentioned.

With `--changed-since <ref>` it adds change-impact analysis, borrowed from IEC 62304
clause 7.3.3: if a criterion's text changed and its named test did not, the build asks for
a `Reviewed-criteria: AC-01.4` trailer on a commit in the range. It does not demand the
test change, because rewording for clarity is legitimate. It demands that somebody say
they looked.

The gate has its own qualification suite, because a gate nobody has watched fail is not
yet known to be a gate. One fixture breaks every rule once; the other uses every rule
correctly. CI asserts the first is rejected and the second accepted, since a checker that
refused everything would pass the first test while being useless. Traceability gets its
own probe: CI plants an orphan identifier, requires the build to go red, removes it, and
requires it to pass again.

```bash
node tools/check-criteria.mjs tools/fixtures/invalid-unfalsifiable.md   # must fail
node tools/check-criteria.mjs tools/fixtures/valid-control.md           # must pass
```

## Running the API

Requires JDK 25 and a running Docker daemon. Maven arrives through the pinned wrapper, so
there is nothing to install.

```bash
cd api
./mvnw verify
```

That runs all 69 tests. The database ones start a real Postgres container, apply both
migrations and assert against the result, because ADR-0001 rejected an embedded substitute
for exactly the CHECK-constraint and ordering behaviour these tests depend on.

To run the service against a local database:

```bash
docker run -d --name wayfinder-db -e POSTGRES_DB=wayfinder \
  -e POSTGRES_USER=wayfinder -e POSTGRES_PASSWORD=wayfinder -p 5432:5432 postgres:17-alpine
cd api && ./mvnw spring-boot:run
```

Then `GET /api/destinations` returns all 30 ordered by slug,
`GET /api/destinations/{slug}` returns one, and an unknown slug returns an RFC 9457
problem response. Health is at `/actuator/health`.

## Architecture

Spring Boot 4 on Java 25 over PostgreSQL with Flyway migrations, with a React and
TypeScript client arriving in M2.

The domain package depends on neither Spring nor JPA. `Destination` is a record whose
constructor enforces the invariants, so an invalid one cannot be built whatever route the
data arrived by, and the ranking that M4 adds can be tested without starting a framework
or a database. Persistence sits behind a port the domain declares. The same invariants are
enforced a second time as CHECK constraints in the migration, so a row that bypassed the
application still cannot exist.

The reasoning, including the alternatives that were rejected and an amendment correcting
the framework and Java versions, is in [ADR-0001](docs/adr/0001-stack-selection.md).

## A known gap

The source documents state the focus group produced six backlog items and name five. The
sixth is not recoverable from the surviving records. It is tracked as US-06 rather than
quietly dropped, because a backlog that silently rounds six down to five is the same class
of problem this project exists to avoid.
