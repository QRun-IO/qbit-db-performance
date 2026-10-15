# QBit DB Performance: specification

Status: **approved for build** (owner decisions 2026-10-15, below). Author: James Maes (drafted with Claude).
Repository: `QRun-IO/qbit-db-performance` (Apache-2.0), package `com.kingsrook.qbits.dbperformance`,
Maven `com.kingsrook.qbits:qbit-db-performance`. First adopter: kof22 (`KofTwentyTwo/Website-Backend`).

## Owner decisions (2026-10-15)

| # | Decision |
|---|---|
| D1 | A **separate qbit**, not part of SQL Workbench. The two link both ways: a statement opens "Explain in SQL Workbench", and SQL Workbench can open a statement's history here. |
| D2 | **Retention:** 30 days of detailed snapshots plus 1 year of daily rollups, both configurable per monitored database. |
| D3 | **v1 scope is everything in this document.** PostgreSQL first, behind a provider interface so MySQL can follow. |

Standing owner standards apply (all recommendations below accepted as written): data in standard
visible QQQ tables, `db_perf_` snake_case table prefix, permissions under the full name
`db_performance.*`, processes as table or record actions with standard Review/Result screens and
plain labels, UI only through `/qqq/v1`, durable jobs for heavy work, no filesystem (database only),
central health, alerts through core `QAlerts`, data-driven help, readable at every width.

## 1. Summary

A system-administration tool that answers "why is the database slow, and what should we change?"
for a QQQ application. It records what the database itself knows (statement statistics, table and
index usage, sessions, locks, connections, maintenance), what QQQ knows about who issued each
statement (route, process, scheduled job, table), and turns both into ranked, deduplicated
findings with evidence and a ready-to-commit fix. It never changes the live database: fixes are
Liquibase snippets for a developer to commit.

It lives under **Admin → Operations → Database Performance** (an admin tool, not a business app).

## 2. Architecture

```
            ┌──────────── per pod ────────────┐        ┌──── one pod (scheduler) ────┐
QQQ RDBMS ──► StatementExecutionListener (core) │        │ Collect snapshot   (5 min)  │
actions      └► Attribution aggregator (memory) │        │ Analyze → findings (hourly) │
                └► flush every N s, 1 batch     │        │ Roll up + purge    (daily)  │
                   insert per window ───────────┼──► DB ◄┤ (durable-eligible processes)│
            └──────────────────────────────────┘        └─────────────┬───────────────┘
                                                                     │ DbStatsProvider
                                                         PostgresStatsProvider (pg_stat_*)
```

- **`DbStatsProvider`** (SPI): `capabilities()`, `readDatabase`, `readStatements`, `readTables`,
  `readIndexes`, `readActivity` (sessions plus lock graph), `readConnections`, `readSettings`,
  `explain(...)`. `PostgresStatsProvider` is the v1 implementation (PostgreSQL 13+, features that
  need 16+/17 such as `EXPLAIN (GENERIC_PLAN)` and per-entry `stats_since` are detected).
  The provider is chosen per monitored backend from the JDBC vendor; an unsupported vendor is
  reported in the Setup card, never a crash.
- **Connections** are the backend's own (QQQ `ConnectionManager`), so they run as the app's
  database user. That user needs the built-in `pg_read_all_stats` role to see other sessions'
  query text and all statements; without it the qbit runs in a reduced mode and the Setup card and
  health check say exactly what to grant.
- **Core hook** (QQQ, next 4.1 RC): `StatementExecutionListener` SPI in `qqq-backend-core`, fired by
  every `qqq-backend-module-rdbms` action (query, count, aggregate, insert, update, delete) with
  backend, table, operation, parameterized SQL, elapsed nanoseconds, row count and success, plus
  `UnitOfWork` scopes opened by core: HTTP request (route template), process run, scheduled job.
  Listeners must be cheap and never throw; core catches and rate-limits their errors. With no
  listener registered the hook costs one volatile read.

## 3. Tables

All tables are standard QQQ tables on the host's backend, created by the qbit's Liquibase
changelog (`db/liquibase/qbit-db-performance/db_performance.changelog.sql`, PostgreSQL and H2),
with foreign keys between the qbit's own tables (never into WORM tables), indexes on every foreign
key and on every time column that retention or a widget filters on.

| Table | Label | Written by | Notes |
|---|---|---|---|
| `db_perf_settings` | Settings | seeded per backend at first run (insert-only), admins | one row per monitored backend: enabled, intervals, retention (30 days detailed, 365 days daily), top-N, attribution on/sample rate/flush seconds/N+1 threshold, EXPLAIN timeout, alert thresholds. Field-audited. Hidden from the assistant. |
| `db_perf_rule` | Suggestion Rules | Liquibase seed (insert-only), admins | one row per rule: code, label, enabled, severity, thresholds. Every rule reads its row. Field-audited. Hidden from the assistant. |
| `db_perf_snapshot` | Snapshots | collector | one row per collection pass: status, duration, reset detection, database-level deltas (commits, rollbacks, blocks hit/read, cache hit ratio, temp files/bytes, deadlocks), connection counts vs `max_connections`, database size, total statement time. Doubles as the collection run log. |
| `db_perf_statement` | Queries | collector | one row per distinct statement (backend + `queryid` + user + top-level): normalized text, fingerprint, operation, main table, first/last seen, the last cumulative counters used for deltas, and 24-hour totals for sorting. **Main nav table.** |
| `db_perf_query_snapshot` | Query Snapshots | collector | per snapshot per statement **delta**: calls, total/mean time, max time (since reset), rows, shared hit/read, hit ratio, temp blocks, % of database time. Top N per pass (default 200) by delta time. Child list on Queries. |
| `db_perf_query_daily` | Query Daily | rollup | per statement per day: calls, total/mean/max time, rows, hit ratio, % of database time. |
| `db_perf_table_stat` | Tables | collector | one row per user table: latest scans, tuples, live/dead, sizes, maintenance times, growth. Nav. |
| `db_perf_table_snapshot` | Table Snapshots | collector (hourly) | per table **delta**: seq/idx scans, tuples read/fetched, inserts/updates/deletes/HOT, live/dead tuples, sizes, autovacuum/analyze counts, freeze age. |
| `db_perf_table_daily` | Table Daily | rollup | per table per day: size, live/dead, scans, writes. |
| `db_perf_index_stat` | Indexes | collector (hourly) | one row per index: definition, columns, unique/primary/valid, scans, size, write cost (table writes since stats reset). Nav. |
| `db_perf_activity_sample` | Activity Samples | collector | notable sessions only (long-running, idle in transaction, blocked or blocking): state, wait event, durations, blocking pids, application, user, `queryid` and **fingerprinted text, never raw text**. |
| `db_perf_connection_sample` | Connection Samples | collector | per snapshot per (database user, application, state): count. |
| `db_perf_attribution` | QQQ Attribution | per-pod flush | per window per (pod, unit kind, unit name, table, operation, statement fingerprint): units, statements, total/max time, rows, errors, sampled flag. |
| `db_perf_attribution_daily` | QQQ Attribution Daily | rollup | the same per day, pods merged. |
| `db_perf_repeat_pattern` | Repeated Statements (N+1) | per-pod flush | per window per (unit kind, unit name, fingerprint): units affected, max and total repeats in one unit, table. |
| `db_perf_finding` | Findings | analyzer, people (status) | rule, severity, score (rank), title, summary, evidence, recommendation, Liquibase snippet, subject table/statement/index, dedupe key (unique), status OPEN/ACKNOWLEDGED/RESOLVED/IGNORED, first/last seen, occurrences, note. Field-audited. **Nav.** |
| `db_perf_explain` | Query Plans | plan viewer | audit of every plan request: who, when, statement, mode (PLAN, GENERIC, ANALYZE), duration, outcome. Plain/generic plans of literal-free statements are kept as plan history; ANALYZE plans are shown, never stored. |

Retention (`db_perf_settings`): `detail_retention_days` (30) purges snapshots, query/table
snapshots, activity and connection samples, attribution, repeat patterns and query plans;
`rollup_retention_days` (365) purges the `*_daily` tables. Statements, tables and indexes not seen
within the rollup retention are deleted with their history. Findings are kept (resolved findings
older than the rollup retention are purged).

Seeded defaults (insert-only, so admin edits survive): every `db_perf_rule` row. `db_perf_settings`
is seeded by the runtime the first time it sees a backend (also insert-only). Tables that start
empty carry help saying what fills them and when; the app home's Setup card shows each step.

## 4. Collection (delta snapshots)

`pg_stat_statements` counters are cumulative. Each pass:

1. Read `pg_stat_statements_info` (`stats_reset`, `dealloc`) and every `pg_stat_statements` row of
   this database (`toplevel` rows; `pg_stat_statements.track = top` on kof22).
2. For each entry, compare with the statement's stored last counters. **Delta = current − last.**
   A reset is detected when `calls` went down, the entry's `stats_since` changed (PostgreSQL 17),
   or `pg_stat_statements_info.stats_reset` moved; then the delta is the current value (activity
   since the reset) and the snapshot records `stats_reset_detected`. New entries count from zero
   only when their `stats_since` falls inside the interval; otherwise they are baselined
   (no delta) so a restart of the qbit never books months of history into one interval.
3. Keep the top N by delta time (default 200); the remainder is summed into the snapshot's totals,
   so % of database time stays exact.
4. Database counters (`pg_stat_database`) use the same delta and reset rules (`stats_reset`).
5. Tables and indexes (`pg_stat_user_tables`, `pg_statio_user_tables`, `pg_stat_user_indexes`,
   `pg_index`, sizes) are collected every `table_snapshot_minutes` (60).
6. Sessions (`pg_stat_activity` with `pg_blocking_pids`) are sampled each pass; only notable
   sessions are stored.

**Text and PHI.** `pg_stat_statements` already replaces constants with `$n`. The qbit
re-normalizes every stored statement text (string and numeric literals become placeholders,
`IN` lists collapse) and stores a SHA-256 fingerprint of the normalized text. Raw text from
`pg_stat_activity` is never persisted: samples keep the `queryid` and the normalized text of the
matching statement, or a fingerprint of the normalized live text. Live text is shown only to users
with `db_performance.view_live_text`, and only in the live activity response.

## 5. QQQ attribution and N+1

The qbit registers a `StatementExecutionListener` at startup (when `attribution_enabled`).

- On each statement: normalize the SQL (QQQ binds parameters, so text is literal-free; `?` lists
  collapse), fingerprint it (cached), and add to an in-memory `LongAdder` aggregate keyed by
  (unit kind, unit name, table, operation, fingerprint). Sampling (`attribution_sample_rate`,
  default 1.0) applies per unit of work. The qbit's own tables are ignored, and the flush thread
  suppresses itself, so attribution never measures itself.
- Per unit of work, a small per-thread map counts each fingerprint; when the unit ends, any
  fingerprint repeated at least `n_plus_one_threshold` times (rule `N_PLUS_ONE`, default 10) is
  added to the repeat-pattern aggregate.
- Every `attribution_flush_seconds` (60) each pod swaps its aggregate and writes it in one batch
  insert per table. This flush is an in-JVM timer, not a durable job: the counts exist only in that
  pod's memory, so no other pod could resume them. A crash loses at most one window of counts,
  which is acceptable for statistics. Memory is bounded (`attribution_max_keys`, 10,000; overflow is
  counted in an `(other)` key).

## 6. Suggestions engine

Rules read their `db_perf_rule` row (enabled, severity, thresholds) and produce findings with
evidence. Each finding has a stable dedupe key (rule + subject), so a rerun updates `last_seen`
and `occurrences` instead of adding rows. A finding that is no longer produced is marked RESOLVED
automatically unless a person IGNORED it; an IGNORED finding stays ignored.

| Rule | Finds | Evidence | Recommendation |
|---|---|---|---|
| `SLOW_QUERY` | statements with high total or mean time | calls, mean, total, % of DB time (24 h) | plan link, index candidates |
| `MISSING_INDEX` | tables with heavy sequential scans and statements filtering on unindexed columns | seq vs idx scans, tuples read, statement predicates | **ready-to-commit Liquibase changeset** (`CREATE INDEX CONCURRENTLY IF NOT EXISTS ...`, `runInTransaction:false`) |
| `UNUSED_INDEX` | non-unique, non-primary indexes with zero scans since stats reset (minimum age) | size, write cost | Liquibase `DROP INDEX CONCURRENTLY IF EXISTS` snippet |
| `DUPLICATE_INDEX` | identical or prefix-redundant indexes on the same table | both definitions, sizes | drop snippet for the redundant one |
| `N_PLUS_ONE` | repeated statement shapes in one unit of work | unit, statement, max/total repeats | batch the lookups (one query with `IN`) |
| `HOT_JOB` | scheduled jobs and processes with the most database time | attribution totals | review schedule or batching |
| `OVER_AUDITED` | QQQ audit tables with high write volume relative to their source tables | inserts per day, size | lower audit level on noisy tables |
| `BLOAT` | tables with high dead-tuple ratio | n_dead/n_live, size | autovacuum settings per table |
| `VACUUM_STALE` | tables not vacuumed or analyzed for long, or near freeze age | last (auto)vacuum/analyze, age | maintenance settings |
| `POOL_SIZING` | connection usage near `max_connections` or many idle connections | peaks by app/user | pool size changes |
| `IDLE_IN_TRANSACTION` | sessions idle in transaction too long | samples | fix the transaction scope; `idle_in_transaction_session_timeout` |
| `CACHE_HIT_LOW` | database cache hit ratio below threshold | hit ratio trend | memory or query fixes |
| `TEMP_SPILL` | statements writing temp blocks | temp blocks, statements | `work_mem` or query fixes |
| `DEADLOCKS` | deadlocks in the interval | count | lock ordering |

Score = severity weight × normalized impact (share of database time, size or connections), so the
Findings table and the home list sort by real impact. Findings never offer a button that changes
the live database.

## 7. Plan viewer

- **Plain EXPLAIN** by default: `EXPLAIN (FORMAT JSON)` for SQL with no parameters, or
  `EXPLAIN (GENERIC_PLAN, FORMAT JSON)` (PostgreSQL 16+) for a normalized statement with `$n`.
- **EXPLAIN (ANALYZE, BUFFERS)** only for a single `SELECT` (or `WITH` without data-modifying
  CTEs), with parameter values the user supplies, in a `READ ONLY` transaction with
  `SET LOCAL statement_timeout` (`explain_timeout_ms`, 10 s) that is always **rolled back**.
  It needs the separate `db_performance.explain_analyze` permission.
- Every request writes a `db_perf_explain` audit row (who, statement, mode, duration, outcome).
  Parameter values are never stored.
- Entry points: the **Query plan** record action on a statement (standard process screens), and
  `POST /qqq/v1/qbit/db-performance/explain` for any client.
- **Explain in SQL Workbench**: each statement links to the Workbench runner with `EXPLAIN` and its
  text (`sqlWorkbenchRunnerPath`, configurable). The reverse link, Workbench → this qbit's query
  history, uses `GET /qqq/v1/qbit/db-performance/statements/lookup?sql=...`, which normalizes the
  SQL the same way and returns the statement's history URL.
- **Explain this plan (assistant, optional):** the plan response carries an `assistantPrompt`
  (the statement's normalized text and the plan, nothing else). A client sends it to the assistant
  through the assistant's own `/qqq/v1` operations with the user's session; the server never calls
  a model. `db_perf_settings` and `db_perf_rule` are marked `AiExposure.NONE`, so the assistant's
  tools cannot read the qbit's configuration.

## 8. UI

**App home (Database Performance)**, full-width widgets only, top to bottom:
1. **Setup** - monitored backend, provider, `pg_read_all_stats` and `pg_stat_statements` access,
   last snapshot, attribution on/off; each with its state and next step.
2. **Database time** - line chart of statement time per snapshot (24 h) with calls.
3. **Top queries** - by total time over 24 h: statement, calls, mean, total, % of DB time, hit ratio.
4. **Findings** - open findings by score with severity and status.
5. **Live activity** - long-running, blocked/blocking chains, idle in transaction (live, not stored).
6. **Connections** - current by application/user vs `max_connections`, cache hit ratio, temp, deadlocks.
7. **QQQ attribution** - routes, processes and jobs by database time; repeated-statement (N+1) patterns.
8. **Tables** - sequential-scan heavy and fastest-growing tables.

Each table widget is full width, paginates instead of scrolling inside, and stacks into cards when
narrow. Record screens: Queries (snapshots and daily history child lists, findings, plan history),
Tables (snapshots, indexes, findings), Indexes, Findings (evidence, snippet).

**Record actions** (never in the nav, standard Review/Result screens):
- Queries: **Query plan**.
- Findings: **Acknowledge**, **Mark resolved**, **Ignore**, **Reopen** (note required for Ignore).
- Snapshots: **Collect now** (`manage_settings`).
- Settings: **Apply default thresholds** (insert-only reseed of missing rules).

**UI API extension** `db-performance` (`/qqq/v1/qbit/db-performance`, semver 1.0.0, committed
OpenAPI fragment with contract test): `GET overview`, `GET activity`, `POST explain`,
`GET statements/lookup`.

## 9. Durable jobs, alerts, health

**Scheduled processes** (host scheduler, `schedulerName` in the config):
`dbPerfCollectSnapshot` (every `snapshot_interval_minutes`, 5), `dbPerfAnalyze` (hourly),
`dbPerfRollupAndPurge` (daily 03:15). Each declares `DurableProcessMetaData` (eligible, ATOMIC,
idempotent), so a host with scheduled durable jobs runs them as crash-proof jobs with job records,
retries and the Jobs app's failure and stall alerts and email. Every step is idempotent: a
snapshot is keyed by backend and capture time, rollups replace the day's row, purges are keyset
batches, so a rerun after a crash never double counts. Each pass also writes its own
`db_perf_snapshot` row (status, duration, error), which is the report even where the host runs
scheduled processes inline.

**Alerts** (core `QAlerts`; delivered by qbit-alerts when installed, logged otherwise; keyed, and
resolved when clear): `db_perf.collection_failed`, `db_perf.query_spike` (a statement's total time
in an interval above `spike_factor` × its 7-day interval median and above a floor),
`db_perf.connection_saturation` (connections ≥ `connection_alert_percent` of `max_connections`),
`db_perf.blocked_chain` (a session blocked longer than `blocked_alert_seconds`).

**Health** (core health SPI, Admin → Health): `db_performance.collector` (last successful snapshot
age vs interval), `db_performance.stats_access` (pg_stat_statements present, `pg_read_all_stats`
held), `db_performance.connections` (saturation).

## 10. Permissions

| Permission | Grants |
|---|---|
| `db_performance.view` | the app, its tables (read), widgets, overview and activity (fingerprinted text) |
| `db_performance.view_live_text` | live query text in the activity view (never persisted) |
| `db_performance.explain` | plain and generic EXPLAIN |
| `db_performance.explain_analyze` | EXPLAIN (ANALYZE, BUFFERS) on SELECTs |
| `db_performance.manage_findings` | finding status actions |
| `db_performance.manage_settings` | edit settings and rules, Collect now |

## 11. Configuration (`DbPerfQBitConfig`)

`backendName` (required, the monitored backend and the tables' backend), `schedulerName`,
`appName` (`dbPerformance`), `parentAppName`, `permissionBaseName` (`db_performance`),
`attributionEnabled` (true), `sqlWorkbenchRunnerPath` (`/app/sqlWorkbenchRunner`), `podName`
(env `HOSTNAME`). Runtime thresholds live in `db_perf_settings` and `db_perf_rule`.

## 12. Host setup (kof22)

1. Grant the app role `pg_read_all_stats` declaratively: cluster-gitops
   `tenants/kof22/website/pg-cluster.yaml`, CNPG managed role `kof22_website` `inRoles`.
   `pg_stat_statements` is already preloaded and created (cluster-gitops#133, #136).
2. Website-Backend: add the dependency, include the qbit changelog, produce the qbit, register the
   listener, grant the permissions to kof22-admins, place the app under Admin → Operations.
   Migrations run as `kof22_website` (ChangelogDefaultRolePostgresIntegrationTest).

## 13. Acceptance

- Delta math: reset (calls down, `stats_since` change, `stats_reset`), new entries, evictions; unit tested.
- Normalizer: literals, IN lists, comments, whitespace; identical fingerprints for QQQ `?` SQL and
  `pg_stat_statements` `$n` SQL of the same statement.
- Postgres Testcontainers: the changelog applies, collection against a live `pg_stat_statements`,
  plan viewer READ ONLY rollback (a write in ANALYZE fails and nothing persists), statement timeout.
- H2: metadata, changelog, every table read by the runtime with a test proving a row changes behavior.
- Attribution: N+1 detection, flush batching (one insert per table per window), self-suppression.
- Findings dedupe, auto-resolve, IGNORED stays ignored, Liquibase snippet content.
- UI API fragment contract test; help bundle coverage test; FK guardrail test.
- JaCoCo ≥ 70% instructions, checkstyle clean.
