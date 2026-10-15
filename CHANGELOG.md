# Changelog

All notable changes are documented here. This project follows Keep a Changelog and semantic versioning.

## [Unreleased]

### Added
- QQQ attribution and N+1 detection: a core statement execution listener (QQQ 4.1.0-RC.13) aggregates database time per route, scheduled job, process, table and statement shape in bounded memory, records repeated statement shapes per unit of work, and each pod flushes one batched insert per table every `attribution_flush_seconds`. Hosts without QQQ's application launcher call `DbPerfRuntime.of(instance).startAttribution()` after startup.

- Secured database-performance tables, PostgreSQL statistics snapshots, daily rollups and configurable retention.
- Audited PLAN/GENERIC and read-only ANALYZE query viewer with SQL validation, timeout, rollback and transient parameter handling.
- Query plan statement action with readable plan text, cost, SQL Workbench link and a sanitized assistant prompt.
- UI API extension 1.0.0 with overview, live activity, explain and statement lookup, plus its committed OpenAPI contract.
- Eight full-width typed home panels with pagination, record links, guided empty states and permission-gated live activity.
- Maven-versioned QBIT help bundle, complete metadata coverage checks, rule threshold guidance and adoption documentation.
- PostgreSQL security acceptance tests and H2 API/widget fixtures.
