# Product backlog

Every acceptance criterion here states how it can be falsified, and names the test that
does it. The backlog it descends from used criteria such as "filtered out or ranked low"
with no definition of either, and set no limit on page load time or caption length, which
is the specific failure the sprint retrospective called out.

This is not a novel idea. It is Tom Gilb's SCALE and METER, INCOSE's rules on measurable
requirements, and the bidirectional traceability that DO-178C has mandated for decades.
See [prior art](prior-art.md) for what this borrows and from whom, and
[ADR-0002](adr/0002-measurable-acceptance-criteria.md) for why it is enforced in CI.

## Reading this document

`Basis` says what kind of claim a criterion makes and, for numbers, where the number came
from. It is the honest part. A threshold nobody calibrated is still useful, but it should
not be able to hide among thresholds that were.

| Basis | Meaning |
| --- | --- |
| `measured` | A numeric threshold derived from observing this system. The figure is cited. |
| `sourced` | A numeric threshold taken from an external standard or published guidance, cited. |
| `provisional` | A numeric threshold chosen by judgment and not yet calibrated. Honest default. |
| `invariant` | A relational or structural property that holds for any parameterization. The number is a count that falls out of the property, not a tuning choice. |
| `categorical` | A property that is true or false. Forcing a number onto it would be dishonest. |

The distinction between `provisional` and `invariant` is the one that matters most. "Match
score below 0.25" is a number someone chose; change it and the criterion changes meaning.
"No destination below the cutoff ranks above one at or above it" holds for *any* cutoff,
so it cannot be satisfied by picking a convenient number. Invariants are the stronger
form, and where a requirement can be expressed as one, it is.

`Verified by` is either a test identifier or `Pending M<n>` naming the milestone that will
supply it. `tools/check-criteria.mjs` enforces the whole format on every push: a criterion
cannot be added without declaring how it could fail.

Thresholds are stated against the seeded reference dataset defined in M1, so the numbers
mean the same thing in every run.

---

## US-01 Ranked destination list

**As a** visitor, **I want** to reach a ranked list of the top five destinations from the
landing page, **so that** I can see what the site recommends before configuring anything.

Origin: SNHU Travel backlog item, Top Five Destinations list. The original criterion
required that the user reach the list by clicking a link, which is what exposed that a
bare list was not a finished story.

| ID | Acceptance criterion | Measure | Basis | Verified by |
| --- | --- | --- | --- | --- |
| AC-01.1 | The landing page exposes a link to the destination list | the primary navigation contains a link to the destination list, and following it succeeds | categorical | Pending M2 |
| AC-01.2 | The list returns a fixed-size ranked set to an anonymous visitor | exactly 5 destinations returned against the seeded dataset | invariant | Pending M2 |
| AC-01.3 | Each entry carries enough detail to choose between options | 4 required fields present and non-empty: name, vacation type, region, summary | invariant | T-DATA-001 |
| AC-01.4 | Summary text is bounded so layout cannot be broken by content | summary length between 1 and 140 characters inclusive | provisional | T-DATA-002 |
| AC-01.5 | The list is fast enough to feel immediate | p95 latency 300 ms or less at 50 concurrent requests | provisional | Pending M6 |
| AC-01.6 | Ranking is reproducible | 100 consecutive calls with identical inputs produce 1 distinct ordering | invariant | Pending M2 |

---

## US-02 Save travel preferences

**As a** returning traveler, **I want** the preferences I set to be saved, **so that** the
site reflects the kind of trips I actually take.

Origin: SNHU Travel backlog item, Save Travel Preferences. The original criteria covered
labels, an unset default state, and a save confirmation. The value statement implied
persistence across visits, which was never written down as a criterion.

| ID | Acceptance criterion | Measure | Basis | Verified by |
| --- | --- | --- | --- | --- |
| AC-02.1 | Every preference control is labelled for assistive technology | 100% of controls expose a non-empty accessible name, 0 violations | invariant | Pending M3 |
| AC-02.2 | No preference is chosen on the user's behalf | 0 controls in a selected state on first visit | invariant | Pending M3 |
| AC-02.3 | Saving is acknowledged, and the acknowledgement says what was saved | confirmation visible within 1 second, naming at least 1 changed field | provisional | Pending M3 |
| AC-02.4 | Preferences survive the end of a session | values returned after sign out and sign in match the saved values, 0 fields lost | invariant | Pending M3 |
| AC-02.5 | Invalid input is rejected without partial writes | HTTP 422 with at least 1 field-level message, and 0 rows changed | invariant | Pending M3 |
| AC-02.6 | A user can only read and write their own preferences | 100% of cross-account attempts return HTTP 404, leaking 0 bytes of another user's data | invariant | Pending M3 |

---

## US-03 Personalized destination ranking

**As a** traveler with saved preferences, **I want** destinations ordered by how well they
suit me, **so that** the ones I would never book stop occupying the top of the list.

Origin: SNHU Travel backlog item, Personalized Travel Destination List. The original said
unlikely destinations should be "filtered out or ranked low" without defining either term,
which left it impossible to test. AC-03.1 supplies the missing definition, and AC-03.2 is
the invariant that makes the definition's exact value stop mattering.

| ID | Acceptance criterion | Measure | Basis | Verified by |
| --- | --- | --- | --- | --- |
| AC-03.1 | "Unlikely" has a single definition the whole system shares | match score below 0.25 on a 0.0 to 1.0 scale | provisional | Pending M4 |
| AC-03.2 | Unlikely destinations never outrank suitable ones | 0 inversions where a destination below the cutoff ranks above one at or above it, for any cutoff in 0.0 to 1.0 | invariant | T-RANK-001 |
| AC-03.3 | Personalization demonstrably changes the result | at least 2 personas receive orderings differing by 1 or more positions | invariant | T-RANK-005 |
| AC-03.4 | A score can be explained, not just produced | per-factor contributions sum to the total score within 0.001 | invariant | T-RANK-002 |
| AC-03.5 | Scoring is reproducible | 100 consecutive calls for one persona produce 1 distinct ordering | invariant | T-RANK-003 |
| AC-03.6 | Scoring is order-independent | shuffling the input 100 times produces 1 distinct output ordering | invariant | T-RANK-004 |
| AC-03.7 | Personalization does not cost noticeable speed | p95 latency 400 ms or less at 50 concurrent requests | provisional | Pending M6 |
| AC-03.8 | Personalized ranking beats showing the catalog unranked | mean NDCG@10 of 0.9469 against 0.2449 for catalog order, measured against the labels in [judgments.csv](../api/src/test/resources/eval/judgments.csv), and higher for every persona individually | measured | T-EVAL-001 |
| AC-03.9 | Ranking quality does not regress | mean NDCG@10 at or above 0.94, the measured value rounded down, against [judgments.csv](../api/src/test/resources/eval/judgments.csv) | measured | T-EVAL-002 |

---

## US-04 Vacation type filters

**As a** traveler, **I want** to narrow destinations to the kinds of trip I am considering,
**so that** I am not reading about trips I have already ruled out.

Origin: SNHU Travel backlog item, vacation type filters.

| ID | Acceptance criterion | Measure | Basis | Verified by |
| --- | --- | --- | --- | --- |
| AC-04.1 | A filter admits nothing outside the requested types | 0 results outside the requested type set | invariant | Pending M4 |
| AC-04.2 | Multiple selected types widen rather than narrow the result | result equals the union of each type's set, 0 missing members | invariant | Pending M4 |
| AC-04.3 | Filtering preserves personalized order within what remains | 0 pairs reordered relative to the unfiltered personalized ranking | invariant | Pending M4 |
| AC-04.4 | An empty result is a valid answer, not an error | HTTP 200 with 0 items and 1 empty-state message | invariant | Pending M4 |
| AC-04.5 | An unrecognized type is rejected helpfully | the response is a client error that names the accepted values | categorical | Pending M4 |

---

## US-05 Hot deals

**As a** price-sensitive traveler, **I want** to see current discounts, **so that** I can
act on one before it ends.

Origin: SNHU Travel backlog item, hot deals.

| ID | Acceptance criterion | Measure | Basis | Verified by |
| --- | --- | --- | --- | --- |
| AC-05.1 | An expired deal is never shown | 0 deals returned whose end timestamp is at or before the current instant | invariant | Pending M5 |
| AC-05.2 | Discounts stay inside a credible range | discount percentage between 1 and 70 inclusive, values outside rejected at write time | provisional | Pending M5 |
| AC-05.3 | The deals list is ordered predictably | 0 pairs out of order under discount descending then end timestamp ascending | invariant | Pending M5 |
| AC-05.4 | The deals list is bounded | at most 10 deals returned | provisional | Pending M5 |
| AC-05.5 | Expiry can be tested without waiting for real time to pass | 100% of expiry tests run against an injected clock, 0 calls to the system clock | invariant | Pending M5 |

---

## US-06 Unidentified sixth backlog item

The source documents state that the focus group produced **six** backlog items, but name
only the five above. The sixth is not recoverable from the sprint review, the presentation
or the module six journal.

This is recorded rather than invented. If the original user story sheets surface, the item
belongs here with criteria written to the same standard. Until then the backlog is
knowingly incomplete, and saying so is more useful than quietly shipping five and implying
that was always the number.

Origin: SNHU Travel backlog, which the source documents state held six items. The sixth
is not recoverable from the sprint review, the presentation or the module six journal.

Tracking: https://github.com/MalloyTheDev/wayfinder/issues/1

| ID | Acceptance criterion | Measure | Basis | Verified by |
| --- | --- | --- | --- | --- |
| AC-06.1 | The gap is disclosed rather than silently dropped | an issue is tracked against this section and linked above | categorical | T-CRIT-001 |

---

## Non-functional criteria

These apply across every story. They exist because the retrospective's first conclusion
was that criteria need measurable limits, and limits that live only inside a single story
get forgotten by the next one.

Origin: sprint retrospective conclusion that acceptance criteria require measurable
limits, generalized beyond any single story.

| ID | Acceptance criterion | Measure | Basis | Verified by |
| --- | --- | --- | --- | --- |
| AC-00.1 | The API answers quickly under ordinary load | p95 latency 300 ms or less across all read endpoints at 50 concurrent requests | provisional | Pending M6 |
| AC-00.2 | Ranked output never varies between identical runs | 1 distinct result per 100 identical calls, on every ranked endpoint | invariant | Pending M6 |
| AC-00.3 | Automated tests cover the logic that decides what a user sees | 85% or higher line coverage of the ranking and preference packages | provisional | Pending M6 |
| AC-00.4 | Every criterion above declares how it could be falsified | 0 criteria lacking a basis, a measure consistent with that basis, or a verification reference | invariant | T-CRIT-001 |
| AC-00.5 | A failed criterion stops a release | 0 deploys from a pipeline with 1 or more failing criterion tests | invariant | Pending M6 |
| AC-00.6 | The domain stays independent of the framework and the database | 0 dependencies from the domain package onto Spring, JPA, Jackson, Flyway or SQL | invariant | T-ARCH-001 |
| AC-00.7 | The service starts fast enough that a blue-green cutover is not a visible outage | median startup 4.22 s in a 512 MB single-CPU container, down from 9.10 s without the AOT cache, a 53.6% reduction, method and figures in [deployment.md](deployment.md) | measured | Pending M6 |
| AC-00.8 | A bad migration never reaches a running instance | migrations run in 1 release step that exits non-zero on failure, verified locally to exit 1 against an unreachable database, see [deployment.md](deployment.md) | measured | Pending M6 |
