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
Twenty-four of this backlog's thirty-six criteria are invariants, and that ratio is the
design rather than an accident.

Where a number genuinely is the honest form, [the backlog](docs/backlog.md) declares where
it came from: `measured`, `sourced`, or `provisional`. **Nine thresholds are provisional**,
meaning chosen by judgment and not yet calibrated, and **none are measured.** The gate
prints that on every run, because a number nobody has checked should not get to sit
alongside ones that were.

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

36 acceptance criteria are defined: 24 invariants, 9 provisional thresholds, 3 categorical.
5 are verified today; the rest name the milestone that will verify them. The gate prints
both ratios on every run, so verification debt and uncalibrated numbers are visible rather
than discovered later.

## What is here now

```
docs/backlog.md                 36 criteria, each declaring a basis and a verification reference
docs/prior-art.md               what this borrows, from whom, and what is still missing
docs/adr/0001-stack-selection.md            Spring Boot, Postgres, React, and why
docs/adr/0002-measurable-acceptance-criteria.md  the rule, why it is enforced, and how it was wrong
tools/check-criteria.mjs        the gate, T-CRIT-001, no dependencies
tools/fixtures/                 the gate's own qualification suite, one case per rule

api/src/main/java/.../domain/          Destination and its invariants, free of Spring and JPA
api/src/main/java/.../persistence/     the JPA adapter behind the domain's catalog port
api/src/main/java/.../web/             the read API and its problem responses
api/src/main/resources/db/migration/   schema, then 30 seeded reference destinations
api/src/test/                          58 tests, including ArchUnit rules and real Postgres
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

The gate has its own qualification suite, because a gate nobody has watched fail is not
yet known to be a gate. One fixture breaks every rule once; the other uses every rule
correctly. CI asserts the first is rejected and the second accepted, since a checker that
refused everything would pass the first test while being useless.

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

That runs all 58 tests. The database ones start a real Postgres container, apply both
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
