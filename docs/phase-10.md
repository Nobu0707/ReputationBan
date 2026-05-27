# Phase 10 / v0.10.0

Phase 10 prepares ReputationBan for the v1.0.0 release line with operational diagnostics and review artifact hardening.

## Implemented

- Added `bans.unbanned_by_name` with a guarded SQLite migration.
- Kept `bans.unbanned_by` as the durable actor ID: player UUID string or `CONSOLE`.
- Stored `bans.unbanned_by_name` as the actor display name for new `/rep unban` and `/rep pardon` updates.
- Updated `/rep banhistory` to show `解除者ID`, `解除者名`, and `解除理由`.
- Added `/rep doctor` and `/rep diagnostics`.
- Added `reputationban.admin.diagnostics` and `DIAGNOSTICS_RUN`.
- Added diagnostics for version, server, Java, DB connectivity, required tables, config validation, Discord enabled/urlConfigured booleans, audit export directory safety, retention settings, pending report counts, threshold pending report counts, and active ReputationBan DB bans.
- Updated review archive generation to collect `checks/local-smoke-check.txt` and `./scripts/run-local-smoke-check.sh=<exit code>` in `checks/command-status.txt`.

## Secret Handling

Doctor output and diagnostics audit metadata only expose Discord webhook state as booleans. The webhook URL itself is not printed, logged, exported to audit CSV, stored in diagnostics reports, or included in review archives.

## Release Readiness

- Version: `0.10.0`
- JAR: `build/libs/ReputationBan-0.10.0.jar`
- Runtime smoke checklist includes `/rep doctor`.
- `review_code.sh`, `run-local-smoke-check.sh`, and `make-review-archive.sh` check v0.10.0 artifacts.
