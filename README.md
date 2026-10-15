# QBit DB Performance

Database performance for QQQ apps: PostgreSQL statement deltas, table and index statistics, live sessions and blocking, connection capacity, audited query plans, and typed home dashboards in standard secured QQQ tables. Approved design: [docs/SPEC.md](docs/SPEC.md). This branch implements collection and the presentation/plan-viewer phase; attribution listeners, findings evaluation, alerts and health are developed in a separate phase. No release has been published yet.

## Requirements

- Java 21 and QQQ **4.1.0-RC.13+** (the statement execution listener SPI used for QQQ attribution).
- PostgreSQL **13+**; **16+** for parameterized generic plans, with the pgJDBC driver (built against 42.7.14).
- `pg_stat_statements` preloaded, created in the monitored database, and readable by the application role.
- `pg_read_all_stats` granted to that role for complete statistics and other sessions' visibility.
- A host scheduler for automatic collection and rollups. Docker is required for the PostgreSQL acceptance tests.

## Install

Add the dependency when the first release is available (use your locally installed snapshot during development):

```xml
<dependency>
   <groupId>com.kingsrook.qbits</groupId>
   <artifactId>qbit-db-performance</artifactId>
   <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Register a QQQ `PostgreSQLBackendMetaData` backend and the scheduler first, then produce the qbit:

```java
new DbPerfQBitProducer()
   .withQBitConfig(new DbPerfQBitConfig()
      .withBackendName("rdbms")
      .withSchedulerName("scheduler")
      .withParentAppName("operations"))
   .produce(qInstance);
```

The optional parent must already exist. Include the migration in the host's Liquibase master:

```xml
<include file="db/liquibase/qbit-db-performance/db_performance.changelog.sql"/>
```

The changelog creates 17 tables and seeds 14 rules without overwriting administrator edits. The qbit never runs migrations itself. Declare the database role grant through your normal database provisioning:

```sql
GRANT pg_read_all_stats TO your_application_role;
```

Configure `shared_preload_libraries` with `pg_stat_statements` and create the extension in the monitored database through your normal deployment process. Missing access produces reduced-capability status and setup guidance.

With `schedulerName`, `dbPerfCollectSnapshot` runs every five minutes and `dbPerfRollupAndPurge` daily at 03:15 (UTC by default). Omitting it registers no schedules. Use **Collect now** on Snapshots for the first baseline and a second collection for deltas. Table and index statistics normally update hourly. The scheduled processes are durable-eligible when the host has durable jobs enabled.

UI hosts must include `qqq-middleware-javalin` (an optional dependency of this qbit). The producer registers UI API extension `db-performance` version `1.0.0` under `/qqq/v1/qbit/db-performance`. Core-only hosts can use tables and collection without middleware.

## Tables

All table names and SQL names are identical; all data is visible through standard QQQ tables.

| Tables | Purpose | Retention |
|---|---|---|
| `db_perf_settings`, `db_perf_rule` | Per-backend settings and rule thresholds | Indefinite |
| `db_perf_snapshot` | Collection status, interval totals and capacity | 30 days |
| `db_perf_statement` | Normalized query inventory and retained 24-hour totals | Stale after 365 days |
| `db_perf_query_snapshot`, `db_perf_query_daily` | Statement interval details and daily aggregates | 30 / 365 days |
| `db_perf_table_stat`, `db_perf_index_stat` | Current table/index inventory and maintenance | Stale after 365 days |
| `db_perf_table_snapshot`, `db_perf_table_daily` | Table history and growth | 30 / 365 days |
| `db_perf_activity_sample`, `db_perf_connection_sample` | Notable activity and connection counts | 30 days |
| `db_perf_attribution`, `db_perf_attribution_daily`, `db_perf_repeat_pattern` | QQQ units of work and repeated statements | 30 / 365 / 30 days |
| `db_perf_finding` | Ranked suggestions and evidence | Open retained; stale closed after 365 days |
| `db_perf_explain` | Every plan request and eligible saved plans | 30 days |

The home page stacks Setup, Database time, Top queries, Findings, Live activity, Connections, QQQ attribution and Tables at full width. Tables paginate; widgets refresh every 30–60 seconds. Statement records include Query Plans and snapshot/daily/finding child lists. Attribution and findings panels remain guided empty states until their producing runtime is installed and has collected data.

Snapshot totals include all observed statement deltas. Only the top configured details are retained, so statement totals and rollups can undercount queries outside that selection. The database-time share denominator includes all observed statement deltas. Reset counters start new baselines rather than creating spikes.

## Permissions

| Permission | Access |
|---|---|
| `db_performance.view` | App, read-only tables, widgets, overview, lookup and normalized activity |
| `db_performance.view_live_text` | Raw SQL in the transient activity API only; also requires view |
| `db_performance.explain` | PLAN/GENERIC requests and Query plan record action |
| `db_performance.explain_analyze` | Execute the guarded ANALYZE service; the record action also needs explain |
| `db_performance.manage_settings` | Settings/rule edits and Collect now |
| `db_performance.manage_findings` | Finding status actions supplied by the findings phase |

Hosts can change `permissionBaseName`; grant the corresponding complete names. Persistent collector and explain-audit writes run as the system user, retaining the requesting session's user ID for plans. Dashboard and API reads retain the viewer's session.

## Configuration

| Host option | Default / meaning |
|---|---|
| `backendName` | Required; monitored database and qbit storage backend |
| `schedulerName` | Optional; host-registered scheduler |
| `appName`, `parentAppName` | `dbPerformance`; optional existing parent app |
| `permissionBaseName` | `db_performance` |
| `attributionEnabled` | `true`; attribution listener integration is a separate phase |
| `sqlWorkbenchRunnerPath` | `/app/sqlWorkbenchRunner` |
| `podName` | `HOSTNAME`, or `local` |
| `snapshotCron`, `analyzeCron`, `rollupCron` | `0 */5 * * * ?`, `0 7 * * * ?`, `0 15 3 * * ?`; analysis schedule is owned by the findings phase |
| `timeZone` | `UTC` |

Runtime values live in Settings: 5-minute collection, hourly tables, 200 top statement details, 30-day detail retention, 365-day rollups, 10-second explain timeout, 30-second long-query and 60-second idle-transaction thresholds. Attribution and alert settings are documented in the shipped help bundle, as are every rule's threshold units and seeded values. Existing administrator settings are never overwritten on startup.

## Query plans and UI API

**Query plan** on one statement presents PLAN/ANALYZE and optional parameter values (one per line). The Result screen shows selectable plan text, cost, a SQL Workbench link and an assistant prompt. There is no action on the Query Plans audit table.

- `GET /overview?backend=`: setup status, latest snapshot, top ten queries and open findings.
- `GET /activity?backend=`: current provider sessions and transitive blocking chains; raw text is permission-gated.
- `POST /explain`: `{ "backend": "rdbms", "sql": "select * from orders where id = $1", "mode": "PLAN" }`. Supply either `statementId` or `sql`. For ANALYZE, add `"parameters": ["123"]` and obtain its separate permission.
- `GET /statements/lookup?backend=&sql=`: normalized fingerprint matches with `/app/<appName>/db_perf_statement/<id>` history paths.

Paths above are relative to `/qqq/v1/qbit/db-performance`. The [committed OpenAPI fragment](src/main/resources/openapi/qqq-v1/db-performance.yaml) is generated from endpoint specs and verified by a contract test. The server never calls an AI model.

## Security and PHI

Stored SQL is normalized and fingerprinted. Raw `pg_stat_activity` text is never persisted and appears only in the activity API with `view_live_text`; home widgets always use normalized text. SQL literals and parameter values must not be used as identifiers: normalization preserves quoted identifiers. Settings and rules are excluded from AI exposure.

PLAN executes EXPLAIN in a read-only transaction with a local timeout and rollback. Parameterized PLAN uses PostgreSQL 16+ GENERIC_PLAN. Only eligible literal-free PLAN/GENERIC plans are stored. A user's SQL must equal its own normalized form to qualify; SQL loaded from the normalized statement table also qualifies.

ANALYZE accepts one SELECT or a non-modifying WITH, rejects INTO and row locks, binds supplied strings as unknown JDBC parameters, and executes inside a read-only transaction. Functions are still constrained by PostgreSQL READ ONLY. The EXPLAIN command uses pgJDBC simple-query execution with JDBC-managed parameter quoting, so PostgreSQL can coerce unknown-typed EXECUTE arguments. The transaction always rolls back and the prepared statement is explicitly deallocated before the connection returns to its pool. SQLSTATE 57014 records TIMEOUT. The service stores only a sanitized audit, never ANALYZE plans or parameter values. Returned ANALYZE plans are transient and can contain values; the assistant prompt redacts literals in plan expressions. Review transient plans before copying them elsewhere.

## Adopting in another app

1. Add compatible QQQ, PostgreSQL backend and qbit dependencies; include the changelog once.
2. Preload/create pg_stat_statements and grant the database statistics role through your provisioning.
3. Register the backend, optional parent app and scheduler, then produce the qbit.
4. Grant the exact permissions to intended operators; reserve ANALYZE and raw live text for appropriate roles.
5. Run Collect now twice, check Setup, and confirm the API extension in `/qqq/v1/openapi.yaml`.
6. Optionally configure SQL Workbench links; install attribution/findings runtime integrations when available. Review and commit proposed database changes through the host's migration workflow.

## Build

```bash
./mvnw -o verify
```

On Windows use `./mvnw.cmd -o verify`. Always use the wrapper; Maven 3.10 cannot read the parent POM. Offline mode requires dependencies already cached. Verification runs main/test Checkstyle, JUnit including PostgreSQL 17 Testcontainers, and JaCoCo gates of 70% instructions and 90% classes. Docker must be accessible to the build process. Regenerate the API fragment with `./mvnw -o -Dtest=DbPerfApiContractTest -Ddbperf.openapi.update=true test`.

## License

[Apache License 2.0](LICENSE).
