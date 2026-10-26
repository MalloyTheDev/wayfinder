# Fixture: a minimal backlog that should pass

The control case. A checker that rejects everything would pass the invalid fixture for the
wrong reason and look like it was working, so CI asserts this file is **accepted**.

One row per basis, each correct, so a rule that becomes accidentally over-strict fails
here rather than silently blocking honest criteria.

## US-01 One correct row per basis

Origin: a fixture, so that each basis can be seen to pass when used correctly.

| ID | Acceptance criterion | Measure | Basis | Verified by |
| --- | --- | --- | --- | --- |
| AC-01.1 | A calibrated threshold, with the measurement cited | p95 180 ms against the seeded dataset, recorded in [run 14](https://example.invalid/run/14) | measured | T-PERF-001 |
| AC-01.2 | A threshold taken from published guidance, cited | availability target of 99.9% per https://sre.google/sre-book/service-level-objectives/ | sourced | Pending M6 |
| AC-01.3 | A threshold chosen by judgment, declared as such | summary length between 1 and 140 characters | provisional | T-DATA-002 |
| AC-01.4 | A property that holds for any parameterization | 0 inversions where an item below the cutoff ranks above one at or above it | invariant | Pending M4 |
| AC-01.5 | A property that is simply true or false | the response names the accepted values | categorical | Pending M4 |
