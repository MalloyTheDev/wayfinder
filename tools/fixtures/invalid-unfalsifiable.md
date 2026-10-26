# Fixture: a backlog that breaks every rule once

Not a real backlog. `tools/check-criteria.mjs` is run against this file in CI and the
build fails if the checker **passes** it, which is how the gate is shown to be a gate
rather than a decoration. This is the same argument as a linter's invalid-case suite, and
the same argument DO-330 makes for qualifying a verification tool.

AC-01.2 carries the exact wording the originating SNHU Travel backlog used, "ranked low",
which the sprint retrospective identified as untestable. If the checker ever accepts that
string again, this project has lost the thing it was built to do.

## US-01 One violation per rule

Origin: a fixture, so that each rule can be seen to fail independently.

| ID | Acceptance criterion | Measure | Basis | Verified by |
| --- | --- | --- | --- | --- |
| AC-01.1 | Basis is not a recognised value | 5 things | vibes | Pending M2 |
| AC-01.2 | Numeric basis with nothing to falsify | ranked low | provisional | Pending M2 |
| AC-01.3 | Categorical basis smuggling in a number | 0 malformed rows out of 1000 | categorical | Pending M2 |
| AC-01.4 | Claims measurement, cites nothing | p95 under 212 ms | measured | Pending M2 |
| AC-01.5 | Claims an external source, cites nothing | 85% coverage | sourced | Pending M2 |
| AC-01.6 | Verification reference is not a test or a milestone | 3 things | invariant | soon |
| AC-01.7 | Duplicate identifier follows | 1 thing | invariant | Pending M2 |
| AC-01.7 | Duplicate identifier | 1 thing | invariant | Pending M2 |
| AC-09.9 | Identifier does not belong to this story | 1 thing | invariant | Pending M2 |

## US-02 A section that never says where its requirement came from

| ID | Acceptance criterion | Measure | Basis | Verified by |
| --- | --- | --- | --- | --- |
| AC-02.1 | Origin is missing from the section above | 1 thing | invariant | Pending M2 |

## US-03 A section promising an issue it does not link

Origin: a fixture.

| ID | Acceptance criterion | Measure | Basis | Verified by |
| --- | --- | --- | --- | --- |
| AC-03.1 | Promises a tracking issue with no Tracking line | 1 open issue tracking the gap | invariant | Pending M2 |
