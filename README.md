# QBit DB Performance

[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/java-21+-blue.svg)](https://adoptium.net/)

> Database performance for QQQ apps: slow and expensive queries from pg_stat_statements with delta snapshots, table scan and index analysis, live activity and blocking chains, connection capacity, maintenance, QQQ query attribution with N+1 detection, and ranked suggestions, all as standard QQQ tables.

**Status:** design: docs/SPEC.md in this repository; implementation in progress. Nothing is published yet.

## Requirements

- Java 21.
- QQQ 4.1 (built against `4.1.0-RC.10`).

## Install

Not yet published. Once released, add `com.kingsrook.qbits:qbit-db-performance` and produce the qbit into your `QInstance`:

```java
new DbPerfQBitProducer()
   .withQBitConfig(new DbPerfQBitConfig().withBackendName("rdbms"))
   .produce(qInstance);
```

## Naming

Tables are `db_perf_<name>` (lowercase snake_case, backend name equal to the table name). Permissions are `db_perf.<action>`.

## Adopting in another app

Follow the QQQ [adoption checklist](https://github.com/QRun-IO/qqq/blob/develop/docs/migration/adoption-checklist.adoc) once the first release is published.

## Build

```bash
./mvnw verify
```

Always use the Maven wrapper (Maven 3.9.11); Maven 3.10 cannot read the parent POM.

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
