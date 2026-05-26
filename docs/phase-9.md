# Phase 9 / v0.9.0

Phase 9 hardens operational paths before v1.0.0.

## Audit And CSV

- Command actors preserve `actor_uuid` and `actor_name` separately.
- Console actions use `actor_uuid=NULL` and `actor_name=CONSOLE`.
- Audit CSV exports are constrained to `plugins/ReputationBan/`; unsafe relative or absolute export paths fall back to `exports`.

## Maintenance

- `/rep maintenance preview` counts cleanup candidates without deleting data.
- `/rep maintenance run` only prints the confirm guidance.
- `/rep maintenance run confirm` creates `backups/reputationban-before-maintenance-YYYYMMDD-HHMMSS.db` before deleting retained data.
- `MAINTENANCE_PREVIEW` and `MAINTENANCE_RUN` audit events record cleanup counts.

## Config Validation

Startup and `/rep reload` validate score bounds, cooldowns, report requirements, retention days, audit limits, Discord timeout range, and unsafe audit export paths. Errors do not disable the plugin in Phase 9, but they are logged clearly and reload senders receive a summary.

## Runtime Smoke

Use `scripts/run-local-smoke-check.sh` for repository-side checks, then follow `docs/runtime-smoke-checklist.md` on a Paper 26.1.2 server with Java 25.
