# Upgrading QBit DB Performance

## First release

There is no previous published release. See [CHANGELOG.md](CHANGELOG.md) for additions and [README.md](README.md) for complete installation.

Use QQQ 4.1.0-RC.11 or newer, Java 21, and PostgreSQL 13 or newer (16+ for generic plans). Include `db/liquibase/qbit-db-performance/db_performance.changelog.sql` once in your application's Liquibase changelog. Run migrations through the host deployment before collecting statistics; the qbit does not apply them automatically.

Preload and create `pg_stat_statements`, then grant `pg_read_all_stats` to the application's database role through your normal provisioning. Without the role the collector reports reduced visibility. Preserve existing rule and settings edits; seeding is insert-only.

Register the producer and a host scheduler. Grant `db_performance.view` and the narrower operator permissions appropriate to each role; ANALYZE and raw live text require separate grants. Existing deployments using an early scaffold's `db_perf.*` grants must use the actual `db_performance.*` names (or the configured permission base).

Collect twice to establish useful deltas. Inspect Setup for access and schedule status. UI hosts need the optional `qqq-middleware-javalin` dependency so the four endpoints register in the standard UI API. The help bundle version is filtered from the Maven project version during the build.
