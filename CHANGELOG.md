# Changelog

All notable changes are documented here. This project follows Keep a Changelog and semantic versioning.

## [Unreleased]

### Fixed
- The Attribution home panel no longer loads a day of attribution detail into the heap. It sums statements and time per unit and table in the database (at most 5,000 groups), so a render holds about a thousand aggregate rows instead of the roughly 570,000 detail rows (540 MB in PostgreSQL) that ran the kof22 web pod out of memory on 2026-10-16. The daily attribution rollup reads the day in 1,000-row keyset pages (`DbPerfStore.forEachPage`), so worker memory is bounded by distinct dimension keys, not by the day's row count.
- Collection no longer stops at RUNNING on large production statement sets: list folding in the SQL normalizer is a linear token scan (the nested-repetition regular expressions overflowed the stack on 60 KB statements with long IN/VALUES lists). Any failure, including an `Error`, now closes the run as FAILED with the exception class, logs it at ERROR (class and frame only, never SQL text) and raises the collection alert; runs left RUNNING by a crash are closed as FAILED after three intervals.
- Only one runtime attributes per JVM: starting attribution on a newer instance (a hot swap, or repeated server starts in tests) stops the previous one, so old instances are never pinned by a forgotten listener and flusher.
- Each table has its own icon (the app icon is now `storage`), so nav entries are distinguishable and do not repeat icons used elsewhere.
- Home tables keep at most four columns with plain headers (readable at every width): secondary facts such as calls and mean time, the session's query text, and a unit's kind and top table move into a second line of the first cell.

### Added
- QQQ attribution and N+1 detection: a core statement execution listener (QQQ 4.1.0-RC.13) aggregates database time per route, scheduled job, process, table and statement shape in bounded memory, records repeated statement shapes per unit of work, and each pod flushes one batched insert per table every `attribution_flush_seconds`. Hosts without QQQ's application launcher call `DbPerfRuntime.of(instance).startAttribution()` after startup.

- Secured database-performance tables, PostgreSQL statistics snapshots, daily rollups and configurable retention.
- Audited PLAN/GENERIC and read-only ANALYZE query viewer with SQL validation, timeout, rollback and transient parameter handling.
- Query plan statement action with readable plan text, cost, SQL Workbench link and a sanitized assistant prompt.
- UI API extension 1.0.0 with overview, live activity, explain and statement lookup, plus its committed OpenAPI contract.
- Eight full-width typed home panels with pagination, record links, guided empty states and permission-gated live activity.
- Maven-versioned QBIT help bundle, complete metadata coverage checks, rule threshold guidance and adoption documentation.
- PostgreSQL security acceptance tests and H2 API/widget fixtures.
