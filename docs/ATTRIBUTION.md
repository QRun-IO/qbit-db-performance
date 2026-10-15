# Attribution runtime (phase B2)

The producer registers `AttributionRuntimeService` with the QQQ application
launcher. It starts one daemon per application instance after initialization,
independently of scheduled-job leadership. Hosts without `QApplicationLauncher`
should call `DbPerfRuntime.of(instance).startAttribution()` after migrations and
instance validation, and `stopAttribution()` before disposing of the instance.
`attributionEnabled=false` prevents listener registration and background work.
Metadata production itself starts no threads, so existing metadata tests remain
isolated.

`AttributionFlusher` can also be constructed directly for deterministic tests:
`refreshSettings()`, register `getListener()` with `StatementExecutionListeners`,
and call `flushNow()`. Always close it to unregister. Production `start()` loads
settings, registers the listener, schedules flushing, and installs a JVM shutdown
hook. Closing stops scheduling, unregisters, and flushes once. Shutdown only
persists completed units; hosts should drain application requests first.

Statements never read settings from the database. A successful settings refresh
publishes an immutable cached copy; a failed refresh keeps the previous copy.
The initial copy disables recording until settings can be read. The configured
flush interval takes effect when the next pass is scheduled. Disabled attribution
still refreshes settings so administrators can re-enable it.

Units are sampled once and buffered until completion, allowing HTTP route names
to be finalized. A completed unit belongs to the window in which it ends; an
abandoned unit is merged when that worker sees a different unit. No-unit work is
sampled per statement and counted as one unit per statement. Sampling transitions
retain separate aggregates for each effective rate. Values are observed counts,
not extrapolated estimates. Daily rollups sum those counts across windows and
pods and take the maximum duration.

Each global map includes its overflow bucket within `attribution_max_keys`.
One slot is reserved for a universal `(other)` key (all dimensions collapse,
fingerprint is SHA-256 of `(other)`). Per-unit maps use the same bound with a
minimum of two slots. Collapsed per-unit shapes cannot establish N+1 evidence
and are excluded from repeat detection, avoiding false positives. Global repeat
overflow sums qualifying repetitions but counts each affected unit only once.
A mixed-rate overflow row has a null sample rate because there is no single
valid rate for it. The SQL normalization cache holds at most 5,000 raw SQL keys
in memory and clears on overflow; only normalized SQL is persisted, capped at
4,000 characters.

Window swaps and counter merges synchronize only in memory. Database work runs
under the system user on the flusher thread with attribution suppressed. Each
nonempty table receives one `InsertAction`, even above 1,000 rows; QQQ may page
the underlying SQL. Both tables are written in one transaction. Failed windows
are dropped, with rate-limited fixed-text logging, and never queued for retry.
The repeat-pattern migration adds `sample_rate` without changing prior
changesets. Existing retention already includes repeat patterns.

Build using the locally installed statement-listener API:

```powershell
.\mvnw.cmd -o verify '-Dqqq.snapshot.version=4.1.0-dbperf-SNAPSHOT'
```

The normal POM default is `4.1.0-RC.13`.
