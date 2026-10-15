# CLAUDE.md

This file provides guidance to Claude Code when working with code in this repository.

## Project Overview

**QBit DB Performance**: Database performance for QQQ apps: slow and expensive queries from pg_stat_statements with delta snapshots, table scan and index analysis, live activity and blocking chains, connection capacity, maintenance, QQQ query attribution with N+1 detection, and ranked suggestions, all as standard QQQ tables. Design: docs/SPEC.md in this repository. Implementation in progress.

## Build Commands

```bash
./mvnw compile          # Compile (always use the wrapper: Maven 3.10 cannot read the parent POM)
./mvnw test             # Unit tests
./mvnw verify           # Unit tests + JaCoCo gate (70% instructions, 90% classes)
```

## Architecture

**Entry point:** `DbPerfQBitProducer implements QBitMetaDataProducer<DbPerfQBitConfig>` in `com.kingsrook.qbits.dbperformance`. Component producers in this package (and below) are found by package scanning.

## Naming (owner rule)

- Tables are lowercase snake_case prefixed with the full qbit name: `db_perf_<name>`. The QQQ table name and the backend (SQL) name are identical; possible-value sources take the table name.
- Permissions use the full qbit name: `db_perf.<action>` (for example `db_perf.view`, `db_perf.admin`).
- Qbit data lives in standard, visible QQQ tables, secured by permissions and record security locks, never hidden.
- Processes run as table or record actions, never in the nav.

## Code Style

- 3-space indentation, opening braces on the next line, `return (x);`.
- Javadoc flower-box comments (`/*****` with `**` lines) on every class and method.
- Fluent setters (`withX()`).
- Wrapper types (`Integer`, `Boolean`) in config and entities.

## Dependencies

- Java 21.
- Parent `com.kingsrook:qbit-build-parent:2.0.0` (qqq 4.0.0). The default-active `qqq-snapshot` profile imports `qqq-bom-pom:4.1.0-RC.10` ahead of it, as in qbit-quick-search (ADR-0007). Remove that activation when the parent moves to 4.1.
- Tests: JUnit 5, AssertJ.

## Release

- CircleCI qqq-orb 0.6.9.
- `develop` publishes SNAPSHOTs.
- A `release/*` branch publishes RCs (the orb bumps the version).
- A `v*` tag publishes the release.
