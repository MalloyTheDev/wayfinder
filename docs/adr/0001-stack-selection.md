# ADR-0001: Spring Boot API with a React client over Postgres

- Status: Accepted, amended 2026-10-26
- Date: 2026-10-25
- Supersedes: nothing

## Amendment, 2026-10-26

Written against Spring Boot 3 and Java 21 before either was checked against what is
actually current. Both were out of date on the day they were written.

Spring Boot 4.1.2 is the current stable release; the 3.x line is no longer where new work
starts. Java 25 is the current LTS, superseding 21, and is the JDK available on the
development machine. The decision is amended to Spring Boot 4 on Java 25.

The reasoning below is unchanged, because none of it turned on those version numbers.
Recorded as an amendment rather than a quiet edit, since being able to see that a decision
was made on stale information is the point of writing it down.

## Context

Wayfinder implements a product backlog that was written during a CS 250 Scrum pilot but
never built. The course produced a single-class Java Swing slide show as a stand-in for
the whole product. The backlog describes something else entirely: ranked destinations,
preferences that persist across sessions, a personalized ordering, type filters, and
time-bounded deals.

Three forces shape the choice of stack.

The backlog is not a toy. Preferences that survive a session require identity and durable
storage. A personalized ranking that can be explained requires somewhere to put real
domain logic. Deals that expire require a clock the tests can control. A client-only
application with browser storage cannot satisfy AC-02.4, AC-02.6 or AC-05.4.

The ranking logic must be testable in isolation. AC-03.2 and AC-03.4 demand ordering
invariants and per-factor score explanations. That argues for a typed language with a
mature test ecosystem, and for keeping the scoring code free of transport and persistence
concerns.

The work continues a Java thread. The originating project was Java, and carrying that
forward makes the progression from coursework to production-grade work legible to a reader
rather than looking like an unrelated restart.

## Decision

**API:** Spring Boot 4 on Java 25 (LTS), built by Maven through a pinned wrapper, layered
as domain, persistence and web, with the domain and ranking logic in packages that depend
on neither Spring nor the database.

**Persistence:** PostgreSQL, schema managed by Flyway migrations. Chosen over an embedded
database so that tests exercise the same engine as production.

**Client:** React with TypeScript, built by Vite, consuming a typed client generated from
the API's OpenAPI description so that a contract change breaks the build rather than
production.

**Tests:** JUnit 5 with Testcontainers for anything touching persistence, and Playwright
for the end-to-end checks that map directly onto acceptance criteria.

**Delivery:** container images built in CI, deployed on a green pipeline.

## Consequences

Testcontainers requires a working Docker daemon in CI and on developer machines, and adds
seconds to the test run. Accepted: testing against a different database than production
is how ordering and constraint bugs reach users.

Two languages and two build tools mean two dependency graphs to keep current. Mitigated by
Dependabot covering both.

A separate client is more moving parts than server-rendered templates. Accepted because
AC-02.1 through AC-02.3 are user interface criteria concerning labelling, default state
and confirmation timing, and they are more honestly tested against a real client.

Java 25 rather than the newest release, because LTS is what the deployment targets and CI
images support without friction.

Maven rather than Gradle, for a reason that is about readers rather than engineering: a
declarative POM is legible to anyone who has touched a Java project, where a Gradle build
script is a program that has to be read. For a repository whose purpose includes being
reviewed by people who did not write it, that matters more than build speed. The wrapper
is pinned so a local build and a CI build run the same Maven.

## Alternatives considered

**Full TypeScript, Next.js with Prisma.** Fewer moving parts, one language, faster to a
deployed product. Rejected because it discards the Java continuity that gives this project
its narrative, and because the ranking logic benefits from a language where the domain
model can be expressed and tested without a framework around it.

**Spring Boot with server-rendered templates.** Simplest deployment and one build. Rejected
because the preference interface criteria are genuinely client-side concerns, and because
a server-rendered form makes AC-02.3's one second confirmation budget awkward to measure.

**Carrying on in Java Swing.** Preserves maximum continuity with the coursework. Rejected
outright: a desktop application is the wrong shape for a travel product, and it was only
ever a stand-in in the first place.
