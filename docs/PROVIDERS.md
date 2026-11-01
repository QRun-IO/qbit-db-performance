# Statistics providers

The qbit reads database statistics through `DbStatsProvider`. `DbStatsProviders` picks the provider from the JDBC product name (`DatabaseMetaData.getDatabaseProductName()`); anything unregistered gets `UnsupportedStatsProvider`, which shows a setup message instead of failing. Each provider declares the statistics areas (`StatsFeature`) its vendor can supply; `ProviderCapabilities.features()` says which are available on the current connection. Dashboards leave unavailable figures empty and analysis rules that need a missing area (`DbPerfRule.requires()`) are skipped, so a vendor with fewer counters produces fewer findings, never false ones.

| JDBC product name | Provider | Saved name | Status |
|---|---|---|---|
| `PostgreSQL` | `PostgresStatsProvider` | `POSTGRES` | Supported (13+) |
| `H2` | `H2StatsProvider` | `H2` | Supported (2.x) |
| `MySQL`, `MariaDB` | none yet | `MYSQL` (reserved) | Unsupported |
| `SQLite` | none yet | | Unsupported |

## Areas by provider

S = supported, A = approximated, - = not available from the vendor.

| Area (`StatsFeature`) | PostgreSQL | H2 | H2 source and notes |
|---|---|---|---|
| Database counters (commits, rollbacks, temp, deadlocks) | S | - | H2 keeps no transaction or temp-file counters |
| Cache hit ratio | S | - | `info.CACHE_HIT_RATIO` exists but is not a cumulative hit/read pair; shown in settings only |
| Database size | S | S | `SETTINGS` `info.FILE_SIZE` (0 for in-memory databases) |
| Connection limit | S | - | H2 has no connection limit |
| Statement statistics | S | S | `QUERY_STATISTICS` with `QUERY_STATISTICS=TRUE`: calls, total/mean/max ms, rows. No query id or user: the id is a stable SHA-256 hash of the text and the user is empty. The collector's own `INFORMATION_SCHEMA` reads are excluded. H2 keeps `QUERY_STATISTICS_MAX_ENTRIES` statements (default 100, set 1000) and evicts the oldest third past the cap |
| Statement I/O (blocks, temp) | S | - | |
| Statement reset time | S (14+) | - | A restart or eviction shows as falling call counts, which the delta logic treats as a reset |
| Table row estimates | S | A | `TABLES.ROW_COUNT_ESTIMATE` |
| Table sizes | S | A | `DISK_SPACE_USED(table)`: table data only; H2 cannot size indexes, so total = table bytes |
| Table access counters (scans, tuple writes) | S | - | |
| Table maintenance (dead rows, vacuum, freeze) | S | - | MVStore has no vacuum |
| Index definitions | S | S | `INDEXES` + `INDEX_COLUMNS`; constraint-generated indexes are marked `USING CONSTRAINT` and never proposed for removal |
| Index usage and size | S | - | |
| Live sessions | S | S | `SESSIONS`: RUNNING = active, SLEEP with uncommitted changes = idle in transaction. Non-admin users see only their own session |
| Blocking | S | S | `SESSIONS.BLOCKER_ID` (admin users) |
| Wait events | S | A | Blocked sessions report wait type `Lock` |
| Transaction age | S | A | No transaction start in H2: an idle transaction's age is the time since it went idle (a lower bound) |
| Connection counts | S | S | `SESSIONS` grouped by user, client info and state |
| Plans | JSON tree with costs | Text | `EXPLAIN` / `EXPLAIN ANALYZE` (index choice, scan counts); generic plans keep `?n` unbound. H2 has no read-only transaction, so ANALYZE relies on the SELECT guard and rollback |

Access codes saved on settings: PostgreSQL `FULL`, `NO_READ_ALL_STATS`, `NO_PG_STAT_STATEMENTS`, `UNSUPPORTED`; H2 `FULL`, `NOT_ADMIN`, `NO_QUERY_STATISTICS`, `UNSUPPORTED`. Health and the Setup panel ask the provider for the remedy text (`remedy`, `setupChecks`). **Enable statement statistics** on Settings calls `enableStatementStatistics`: H2 runs `SET QUERY_STATISTICS_MAX_ENTRIES 1000` and `SET QUERY_STATISTICS TRUE` (admin, until the database closes); PostgreSQL cannot do it at runtime.

## Adding a vendor

1. Implement `DbStatsProvider`: the reads, `capabilities`, `features`, `remedy`, `setupChecks`, and optionally `enableStatementStatistics` and `explainText`. Map session states to `active` / `idle` / `idle in transaction` and leave unavailable counters out of the maps rather than writing zeros.
2. Register it under the JDBC product name: add it to the table in `DbStatsProviders`, or call `DbStatsProviders.register("ProductName", MyProvider::new)` from the host before collection.
3. Add its saved name to `DbPerfPossibleValues.Provider` and any new access codes to `StatsAccess` and the health check.
4. Make sure the qbit's Liquibase changelog runs on the vendor (the qbit stores its tables in the monitored backend).
5. Test every read against a real database (Testcontainers or embedded), with and without the vendor's statistics prerequisites.

## MySQL and MariaDB

What a provider would map (MySQL 8.0+, MariaDB 10.6+ with `performance_schema=ON`, which needs a server restart):

| Area | Source | Notes |
|---|---|---|
| Statements | `performance_schema.events_statements_summary_by_digest` | Digest id and normalized text, calls, timer (picoseconds), rows examined/sent, temp tables, `FIRST_SEEN`; `TRUNCATE` resets. Needs `SELECT` on `performance_schema` |
| Database counters | `SHOW GLOBAL STATUS` (`Com_commit`, `Com_rollback`, `Created_tmp_disk_tables`, `Innodb_deadlocks`) | Server-wide, not per schema |
| Cache hit ratio | `Innodb_buffer_pool_read_requests` / `Innodb_buffer_pool_reads` | |
| Connection limit and counts | `max_connections`, `information_schema.PROCESSLIST` / `performance_schema.threads` | Needs `PROCESS` to see other users |
| Sessions and blocking | `PROCESSLIST`, `sys.innodb_lock_waits` (MySQL) / `information_schema.INNODB_LOCK_WAITS` (MariaDB) | |
| Tables | `information_schema.TABLES` (`TABLE_ROWS` estimate, `DATA_LENGTH`, `INDEX_LENGTH`) | Sizes are estimates |
| Table access | `performance_schema.table_io_waits_summary_by_table` | Read/write counts, no seq-vs-index split |
| Index usage | `performance_schema.table_io_waits_summary_by_index_usage`, `sys.schema_unused_indexes` | |
| Maintenance | - | InnoDB purge is automatic; no vacuum or dead-row counters |
| Plans | `EXPLAIN FORMAT=JSON`, `EXPLAIN ANALYZE` (MySQL 8.0.18+, tree text) | MariaDB uses `ANALYZE FORMAT=JSON`; a JSON renderer would be needed |

Beyond the provider class, MySQL support also needs: the MySQL/MariaDB Connector/J on the host classpath; a provider-neutral `ExplainService` path for JSON plans (today JSON plans are PostgreSQL's format and the renderer is PostgreSQL-specific); MySQL-compatible SQL in the changelog (verify it runs, since the qbit stores its own tables there) and in the migration snippets the findings suggest (`CREATE INDEX CONCURRENTLY` is PostgreSQL syntax; MySQL needs `ALGORITHM=INPLACE, LOCK=NONE`); help text that currently names PostgreSQL views; and Testcontainers tests for both MySQL and MariaDB, since their lock views differ.

## SQLite

SQLite has no server, sessions, statement statistics or users. A provider could supply only: database size (`PRAGMA page_count` x `page_size`), tables and row counts (`sqlite_schema` plus `COUNT(*)` or `sqlite_stat1` after `ANALYZE`), table and index sizes (the `dbstat` virtual table, when compiled in), index definitions (`sqlite_schema.sql`, `PRAGMA index_list` / `index_info`), and text plans (`EXPLAIN QUERY PLAN`). Statement statistics would have to come from QQQ attribution (the qbit's own statement listener), not the database. It is useful only as an inventory and plan viewer.
