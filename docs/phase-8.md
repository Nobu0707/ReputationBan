# Phase 8 / v0.8.0

Phase 8 adds audit logging and retention maintenance.

## Audit Events

`audit_events` is a cross-cutting investigation log. Existing tables remain canonical:

- `reports`: report state
- `score_history`: actual score changes
- `bans`: ban history
- `audit_events`: who did what, when, and why

Audit metadata is deliberately limited. Secret-like keys such as webhook URLs, tokens, cookies, passwords, session IDs, and secrets are omitted.

## Commands

- `/rep audit recent [limit]`
- `/rep audit <player> [limit]`
- `/rep audit type <eventType> [limit]`
- `/rep audit export recent [limit]`
- `/rep audit export <player> [limit]`
- `/rep maintenance run`

CSV exports are written to `plugins/ReputationBan/exports/` by default.

## Retention

`retention.audit-events-days`, `retention.rejected-reports-days`, and `retention.cancelled-reports-days` control cleanup. `0` or less disables cleanup for that data type.

`retention.score-history-days` and `retention.bans-days` default to `0`, so important history is not deleted unless the server operator opts in.
