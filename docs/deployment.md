# Deployment

**Status: prepared, not performed.** Everything below is written and the parts that can be
verified locally have been. Nothing has been deployed, because deploying needs a Fly
account, a Neon database and a payment method. This document says exactly which parts are
proven and which are not, because a deployment guide that quietly blends the two is worse
than none.

## What is verified

Locally, against Docker:

- The image builds, generating the AOT cache inside the build against the exact jar and
  JVM that will run it.
- The container starts under a 512 MB, single-CPU limit, applies both migrations, serves
  all 30 destinations, returns `application/problem+json` on an unknown slug, and runs as
  an unprivileged user (uid 10001).
- Memory settles at about 206 MB of the 512 MB limit.
- The migration command applies both migrations and exits 0, and exits 1 against an
  unreachable database. That exit code is what stops a Fly deploy.

Measured, median of three runs each way:

| Environment | Without AOT cache | With AOT cache | Reduction |
| --- | --- | --- | --- |
| 512 MB / 1 CPU container | 9.10 s | 4.22 s | 53.6% |
| Unconstrained host | 4.17 s | 2.34 s | 43.8% |

The container figure is the one that matters, and it is the larger because a constrained
CPU makes class loading a bigger share of startup. The cost is 106 MB of image.

## What is not verified

Everything in `api/fly.toml`, and every step in `.github/workflows/deploy.yml` after the
configuration check. No deploy has run. In particular these are reasoned but untested:

- Whether `release_command` arguments reach the application. The Dockerfile forwards
  arguments specifically because Fly does not override `ENTRYPOINT`, and the forwarding
  itself is verified locally, but the Fly end of it is not.
- Blue-green cutover behaviour and whether the readiness grace period is long enough.
- Whether the health check path and interval suit Fly's proxy.

## The intended stack, and why

| Component | Choice | Cost |
| --- | --- | --- |
| Application | Fly.io `shared-cpu-1x`, 1 GB, always warm | about $6.70/mo |
| Database | Neon, managed Postgres | $0 on Free, more on Launch |
| Edge | Cloudflare Free | $0 |
| CI/CD and registry | GitHub Actions and GHCR | $0 on a public repo |

**Why not scale to zero.** A cold JVM start is seconds and a cold platform start is worse.
The entire point of a live URL is that someone clicks it and it works, so
`min_machines_running = 1` and `auto_stop_machines = "off"` are the configuration, and the
AOT cache is what makes the remaining startup cost tolerable on deploys and restarts.

**Why the database is not on Fly.** Fly's managed Postgres starts at about $38/mo, which
is five times the application. Decoupling it is what makes the whole thing cost roughly
eight dollars.

## Two traps worth knowing before the first deploy

**The Neon compute-hour trap.** Neon's free tier allows 100 compute-hours per project per
month and suspends compute when they run out. An always-on application holding a
connection pool open never lets Neon idle-suspend, so a 0.25 CU compute runs for the whole
month: 0.25 x 730 = roughly 182 compute-hours, well past the limit, and the database goes
dark mid-month. Either set `minimum-idle` to 0 with a short idle timeout so the pool fully
drains, accepting a wake delay on the next request, or pay for Launch. **Measure Neon's
resume latency against the 300 ms budget in AC-01.5 before relying on the free path.**

**Rollback is redeploying an image, not reverting a schema.** Flyway Community has no
`undo`; it is a paid feature. So the honest statement of the rollback story is:

> Rollback means redeploying the previous image digest. That is only valid because every
> migration is backward-compatible with the previous release. There is no schema rollback.

Which means destructive schema changes take three releases, not one: **expand** (add the
new column, nullable), **migrate** (deploy code that writes both shapes and backfills),
and **contract** (a later release drops the old column). Because migrations run before the
new code goes live, rolling code back lands on an already-migrated schema, and
expand/contract is exactly what makes that safe.

## First deploy

Requires a Fly account and a Neon database, and will incur cost.

1. Create the Neon project and copy its connection string.
2. `fly launch --no-deploy` from `api/`, which sets `app` and `primary_region` in
   `fly.toml`. Keep the rest of the file.
3. Set the database credentials as secrets, never as `[env]`:
   `fly secrets set WAYFINDER_DB_URL=... WAYFINDER_DB_USER=... WAYFINDER_DB_PASSWORD=...`
4. `fly deploy` once by hand, so the first run's output is read by a person rather than
   scrolling past in CI.
5. Add `FLY_API_TOKEN` as a repository secret and `WAYFINDER_BASE_URL` as a repository
   variable. The deploy workflow starts working at that point with no edit.
6. Run a rollback once, deliberately, and record how long it took. An untested rollback is
   not a rollback.
