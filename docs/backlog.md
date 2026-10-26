# Product backlog

Every acceptance criterion here carries a measure that can be checked by a machine, and
names the test that checks it. This is deliberate. The backlog it descends from used
criteria such as "filtered out or ranked low" with no definition of either, and set no
limit on page load time or caption length, which is the specific failure the sprint
retrospective called out. See [ADR-0002](adr/0002-measurable-acceptance-criteria.md).

`Measure` must contain a concrete threshold. `Verified by` is either a test identifier or
`Pending M<n>` naming the milestone that will supply it. `tools/check-criteria.mjs`
enforces both on every push, so a criterion cannot be added here without a way to falsify
it.

Scoring, thresholds and latency budgets are stated against the seeded reference dataset
defined in M1, so the numbers mean the same thing in every run.

---

## US-01 Ranked destination list

**As a** visitor, **I want** to reach a ranked list of the top five destinations from the
landing page, **so that** I can see what the site recommends before configuring anything.

Origin: SNHU Travel backlog item, Top Five Destinations list. The original criterion
required that the user reach the list by clicking a link, which is what exposed that a
bare list was not a finished story.

| ID | Acceptance criterion | Measure | Verified by |
| --- | --- | --- | --- |
| AC-01.1 | The landing page exposes a link to the destination list | 1 link present within the page's primary navigation landmark, resolving to HTTP 200 | Pending M2 |
| AC-01.2 | The list returns a fixed-size ranked set to an anonymous visitor | exactly 5 destinations returned against the seeded dataset | Pending M2 |
| AC-01.3 | Each entry carries enough detail to choose between options | 4 required fields present and non-empty: name, vacation type, region, summary | Pending M2 |
| AC-01.4 | Summary text is bounded so layout cannot be broken by content | summary length between 1 and 140 characters inclusive | Pending M2 |
| AC-01.5 | The list is fast enough to feel immediate | p95 latency 300 ms or less at 50 concurrent requests | Pending M6 |
| AC-01.6 | Ranking is reproducible | 100 consecutive calls with identical inputs produce 1 distinct ordering | Pending M2 |

---

## US-02 Save travel preferences

**As a** returning traveler, **I want** the preferences I set to be saved, **so that** the
site reflects the kind of trips I actually take.

Origin: SNHU Travel backlog item, Save Travel Preferences. The original criteria covered
labels, an unset default state, and a save confirmation. The value statement implied
persistence across visits, which was never written down as a criterion.

| ID | Acceptance criterion | Measure | Verified by |
| --- | --- | --- | --- |
| AC-02.1 | Every preference control is labelled for assistive technology | 100% of controls expose a non-empty accessible name, 0 violations | Pending M3 |
| AC-02.2 | No preference is chosen on the user's behalf | 0 controls in a selected state on first visit | Pending M3 |
| AC-02.3 | Saving is acknowledged, and the acknowledgement says what was saved | confirmation visible within 1 second, naming at least 1 changed field | Pending M3 |
| AC-02.4 | Preferences survive the end of a session | values returned after sign out and sign in match the saved values, 0 fields lost | Pending M3 |
| AC-02.5 | Invalid input is rejected without partial writes | HTTP 422 with at least 1 field-level message, and 0 rows changed | Pending M3 |
| AC-02.6 | A user can only read and write their own preferences | 100% of cross-account attempts return HTTP 404, leaking 0 bytes of another user's data | Pending M3 |

---

## US-03 Personalized destination ranking

**As a** traveler with saved preferences, **I want** destinations ordered by how well they
suit me, **so that** the ones I would never book stop occupying the top of the list.

Origin: SNHU Travel backlog item, Personalized Travel Destination List. The original said
unlikely destinations should be "filtered out or ranked low" without defining either term,
which left it impossible to test. AC-03.1 supplies the missing definition.

| ID | Acceptance criterion | Measure | Verified by |
| --- | --- | --- | --- |
| AC-03.1 | "Unlikely" has a single definition the whole system shares | match score below 0.25 on a 0.0 to 1.0 scale | Pending M4 |
| AC-03.2 | Unlikely destinations never outrank suitable ones | 0 inversions where a destination below 0.25 ranks above one at or above 0.25 | Pending M4 |
| AC-03.3 | Personalization demonstrably changes the result | at least 2 seeded personas receive orderings differing by 1 or more positions | Pending M4 |
| AC-03.4 | A score can be explained, not just produced | per-factor contributions sum to the total score within 0.001 | Pending M4 |
| AC-03.5 | Scoring is reproducible | 100 consecutive calls for one persona produce 1 distinct ordering | Pending M4 |
| AC-03.6 | Personalization does not cost noticeable speed | p95 latency 400 ms or less at 50 concurrent requests | Pending M6 |

---

## US-04 Vacation type filters

**As a** traveler, **I want** to narrow destinations to the kinds of trip I am considering,
**so that** I am not reading about trips I have already ruled out.

Origin: SNHU Travel backlog item, vacation type filters.

| ID | Acceptance criterion | Measure | Verified by |
| --- | --- | --- | --- |
| AC-04.1 | A filter admits nothing outside the requested types | 0 results outside the requested type set | Pending M4 |
| AC-04.2 | Multiple selected types widen rather than narrow the result | result equals the union of each type's set, 0 missing members | Pending M4 |
| AC-04.3 | Filtering preserves personalized order within what remains | 0 pairs reordered relative to the unfiltered personalized ranking | Pending M4 |
| AC-04.4 | An empty result is a valid answer, not an error | HTTP 200 with 0 items and 1 empty-state message | Pending M4 |
| AC-04.5 | An unrecognized type is rejected helpfully | HTTP 400 listing all 1 or more accepted values | Pending M4 |

---

## US-05 Hot deals

**As a** price-sensitive traveler, **I want** to see current discounts, **so that** I can
act on one before it ends.

Origin: SNHU Travel backlog item, hot deals.

| ID | Acceptance criterion | Measure | Verified by |
| --- | --- | --- | --- |
| AC-05.1 | An expired deal is never shown | 0 deals returned whose end timestamp is at or before the current instant | Pending M5 |
| AC-05.2 | Discounts stay inside a credible range | discount percentage between 1 and 70 inclusive, values outside rejected at write time | Pending M5 |
| AC-05.3 | The deals list is bounded and ordered predictably | at most 10 deals, ordered by discount descending then end timestamp ascending | Pending M5 |
| AC-05.4 | Expiry can be tested without waiting for real time to pass | 100% of expiry tests run against an injected clock, 0 calls to the system clock | Pending M5 |

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

| ID | Acceptance criterion | Measure | Verified by |
| --- | --- | --- | --- |
| AC-06.1 | The gap is disclosed rather than silently dropped | 1 open issue tracking recovery of the sixth item | Pending M1 |

---

## Non-functional criteria

These apply across every story. They exist because the retrospective's first conclusion
was that criteria need measurable limits, and limits that live only inside a single story
get forgotten by the next one.

Origin: sprint retrospective conclusion that acceptance criteria require measurable
limits, generalized beyond any single story.

| ID | Acceptance criterion | Measure | Verified by |
| --- | --- | --- | --- |
| AC-00.1 | The API answers quickly under ordinary load | p95 latency 300 ms or less across all read endpoints at 50 concurrent requests | Pending M6 |
| AC-00.2 | Ranked output never varies between identical runs | 1 distinct result per 100 identical calls, on every ranked endpoint | Pending M6 |
| AC-00.3 | Automated tests cover the logic that decides what a user sees | 85% or higher line coverage of the ranking and preference packages | Pending M6 |
| AC-00.4 | Every acceptance criterion above is machine-checked for a measure | 0 criteria lacking a numeric measure or a verification reference | T-CRIT-001 |
| AC-00.5 | A failed criterion stops a release | 0 deploys from a pipeline with 1 or more failing criterion tests | Pending M6 |
