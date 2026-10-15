# QBit DB Performance

[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/java-21+-blue.svg)](https://adoptium.net/)

> Database performance for QQQ apps: slow and expensive queries from pg_stat_statements with delta snapshots, table scan and index analysis, live activity and blocking chains, connection capacity, maintenance, QQQ query attribution with N+1 detection, and ranked suggestions, all as standard QQQ tables.

**Status:** Phase A is implemented: schema and secured metadata, PostgreSQL statistics collection, daily rollups and retention. Attribution listeners, suggestions, explain actions, dashboard/API integration and help are later phases. Nothing is published yet. Approved design: [docs/SPEC.md](docs/SPEC.md).

## Requirements

- Java 21.
- QQQ 4.1 (built against `4.1.0-RC.11`).
- PostgreSQL 13+ for collection; PostgreSQL 17 and H2 2.2 are migration test targets.

## Install

Not yet published. Once released, add `com.kingsrook.qbits:qbit-db-performance` and produce the qbit into your `QInstance`:

```java
new DbPerfQBitProducer()
   .withQBitConfig(new DbPerfQBitConfig()
      .withBackendName("rdbms")
      .withSchedulerName("scheduler")) // omit to register no schedules
   .produce(qInstance);
```

The backend must already exist. For PostgreSQL, use QQQ's
`PostgreSQLBackendMetaData` from `qqq-backend-module-postgres` so table actions
use the PostgreSQL SQL dialect. The qbit reads statistics through the backend's
connection manager and writes its records through QQQ actions as the system user.

Include `db/liquibase/qbit-db-performance/db_performance.changelog.sql` in the
host's Liquibase changelog. It creates 17 tables and seeds 14 suggestion rules
without overwriting administrator edits. The runtime creates its Settings row
on first use. It does not run migrations itself.

Enable `pg_stat_statements` in PostgreSQL's `shared_preload_libraries`, create
the extension in the monitored database, and grant `pg_read_all_stats` to the
application database role. Missing statistics access produces a PARTIAL snapshot
and a Settings message; unsupported database vendors remain usable for metadata.

## Naming

Tables are `db_perf_<name>` (lowercase snake_case, backend name equal to the table name). Permissions are `db_performance.<action>`; the complete names are in `DbPerfPermissions.ALL`.

The app and tables require `db_performance.view`. Settings updates, rule changes
and Collect now require `db_performance.manage_settings`. Other tables are
read-only to users. Settings and rules have no AI exposure.

Collection defaults to every five minutes, with table/index samples hourly.
New statements establish a baseline instead of booking historical counts into
one interval. Snapshot totals include all statement deltas; only the top 200
details are stored by default. Therefore a statement's 24-hour totals and daily
rollup reflect its retained details, while its share uses the full database total.

The daily process rolls up yesterday and missing days in the preceding week,
then purges details after 30 days and daily rows after 365 days. These thresholds
are editable in Settings. Days use the configured time zone, UTC by default.
Processes are scheduled or exposed as table actions, never navigation children.

## Adopting in another app

Follow the QQQ [adoption checklist](https://github.com/QRun-IO/qqq/blob/develop/docs/migration/adoption-checklist.adoc) once the first release is published.

## Build

```bash
./mvnw -o verify
```

Always use the Maven wrapper (Maven 3.9.11); Maven 3.10 cannot read the parent POM.
On Windows use `./mvnw.cmd -o verify`. Validation includes main/test checkstyle,
JUnit tests and JaCoCo gates of 70% instructions and 90% classes. PostgreSQL
Testcontainers tests (including `PostgresStatsProviderIT`) run with the suite and
skip when Docker is unavailable.

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
