# ReputationBan

ReputationBan is a PaperMC moderation plugin that tracks player reputation scores from reports and staff actions. It stores data in SQLite, supports pending report review, and can trigger profile-based temporary or permanent bans when scores cross the configured threshold.

Current version: `0.13.0`

## Requirements

- Minecraft/PaperMC 26.1.2
- Java 25
- Gradle wrapper included
- SQLite JDBC loaded through `plugin.yml` libraries
- Optional Discord webhook for moderation notifications

## Documentation

- [Installation](docs/INSTALLATION.md)
- [Configuration](docs/CONFIGURATION.md)
- [Migration](docs/MIGRATION.md)
- [Release readiness](docs/RELEASE_READINESS.md)
- [Support bundle](docs/SUPPORT_BUNDLE.md)
- [Security and redaction](docs/SECURITY_REDACTION.md)
- [Changelog](CHANGELOG.md)

## Main Features

- Reputation score storage in SQLite.
- Player reports with categories, cooldowns, eligibility gates, and optional staff review.
- Staff score management, report review, ban history, unban, and pardon commands.
- Profile-based BAN handling without name BAN APIs.
- Audit log, CSV export, retention preview, and confirmed maintenance cleanup with backup.
- Manual SQLite backup and secret-redacted support bundle generation.
- Optional Discord webhook notifications with secret-safe diagnostics and audit metadata.

## Commands

- `/reportbad <player> <category> <reason>`: report a player.
- `/rep`: show your own score.
- `/rep version`: show the installed ReputationBan version and target runtime.
- `/rep help`: show commands available to you.
- `/rep check <player>`: show another player's score.
- `/rep history <player> [limit]`: show recent score history.
- `/rep banhistory <player> [limit]`: show ReputationBan ban history.
- `/rep baninfo <player>`: show current Paper/Profile and ReputationBan ban state.
- `/rep unban <player> [reason...]`: remove the player's Profile BAN and mark active DB bans unbanned.
- `/rep pardon <player> [reason...]`: unban, clear report suspension, and restore score to max.
- `/rep audit recent [limit]`: show recent audit events.
- `/rep audit <player> [limit]`: show audit events for a player target.
- `/rep audit type <eventType> [limit]`: show audit events by type.
- `/rep audit export recent [limit]`: export recent audit events as CSV.
- `/rep audit export <player> [limit]`: export player audit events as CSV.
- `/rep maintenance preview`: show retention cleanup counts without deleting data.
- `/rep maintenance run`: show the confirm guidance without deleting data.
- `/rep maintenance run confirm`: back up the SQLite database and run retention cleanup.
- `/rep backup [reason...]`: create a manual SQLite database backup.
- `/rep doctor`: show operational diagnostics without exposing Discord webhook URLs.
- `/rep diagnostics`: alias for `/rep doctor`.
- `/rep support bundle`: create a support diagnostic ZIP without DB files or server logs.
- `/rep add <player> <points> [reason...]`: add score points.
- `/rep remove <player> <points> [reason...]`: remove score points.
- `/rep set <player> <score> [reason...]`: set an exact score.
- `/rep reload`: reload configuration.
- `/reports`: list pending reports.
- `/reports help`: show report review commands.
- `/reports list [pending|threshold_pending|approved|rejected|auto_accepted|cancelled|all] [limit]`: list reports.
- `/reports view <id>`: show report details.
- `/reports approve <id> [note...]`: approve a pending report and apply score deduction.
- `/reports reject <id> [note...]`: reject a pending report.

## Phase 7 Report Safety

- `/reportbad` can require reporter playtime with `report-requirements.min-playtime-minutes`.
- `/reportbad` can require server account age with `report-requirements.min-account-age-days`. This is based on ReputationBan `players.first_seen`, not Mojang account creation date.
- `rating.min-unique-reports-before-deduction` controls automatic deduction. `1` means immediate auto acceptance; `2` or more stores non-review reports as `threshold_pending` until enough unique reporters submit the same target/category within `rating.report-window-days`.
- `threshold_pending` means waiting for the multi-report threshold, not staff review.
- Score drops crossing `warning`, `watch`, `restricted`, or `final-warning` in `score-thresholds` notify staff and Discord. The `ban` threshold remains handled by the auto-ban notification path.

## Phase 8 Audit And Retention

- `audit_events` records cross-cutting moderation events while `reports`, `score_history`, and `bans` remain the canonical data tables.
- Audit CSV exports are written under `plugins/ReputationBan/exports/` by default.
- Audit metadata intentionally omits secret-like keys such as webhook URLs, tokens, passwords, cookies, and session IDs.
- `retention.audit-events-days`, `retention.rejected-reports-days`, and `retention.cancelled-reports-days` control default cleanup. `retention.score-history-days` and `retention.bans-days` default to `0`, meaning no deletion.

## Phase 9 Operational Hardening

- Audit actor fields now keep player UUID and display name separate; console actions use `actor_uuid=NULL` and `actor_name=CONSOLE`.
- Audit CSV export paths are constrained to the plugin data folder even if `audit.export-directory` is misconfigured.
- Maintenance now supports `/rep maintenance preview` and requires `/rep maintenance run confirm` for deletion. Confirmed runs create `plugins/ReputationBan/backups/reputationban-before-maintenance-*.db`.
- Startup and `/rep reload` validate obvious unsafe or invalid config values and report summaries.
- Runtime smoke support lives in `scripts/run-local-smoke-check.sh` and `docs/runtime-smoke-checklist.md`.

## Phase 10 Release Readiness

- `bans.unbanned_by` stores a durable actor ID (`UUID` string or `CONSOLE`), while `bans.unbanned_by_name` stores the display name used for history output.
- `/rep doctor` and `/rep diagnostics` report version, server, Java, database/table health, config warnings/errors, Discord enabled/urlConfigured booleans, safe audit export status, retention settings, and lightweight report/BAN counts.
- Doctor audit metadata records only booleans and counts. Discord webhook URLs are not printed, exported, stored in doctor reports, or included in diagnostics audit metadata.
- Review archives now include `checks/local-smoke-check.txt` and command status for `./scripts/run-local-smoke-check.sh`.
- v0.10.0 is the pre-v1.0.0 operational diagnostics and release readiness phase.

## Phase 11 Release Preparation

- `/rep version` reports the installed plugin version and target runtime.
- `/rep doctor` includes plugin data folder, database file existence, audit export safety, Discord enabled/urlConfigured booleans, and backup directory writability without printing webhook URLs.
- Release documents cover installation, configuration, migration, and v1.0.0 readiness.
- `scripts/run-paper-runtime-smoke-helper.sh` safely prepares a Paper test server by backing up existing plugin JARs and copying the built ReputationBan JAR.
- Review archive and local smoke scripts avoid unnecessary duplicate builds where possible.

## Phase 12 Support And Release Artifacts

- `/rep backup [reason]` creates `plugins/ReputationBan/backups/reputationban-manual-backup-*.db` and records `DB_BACKUP_CREATED` without absolute paths or webhook-like reason values.
- `/rep support bundle` creates `plugins/ReputationBan/support/reputationban-support-*.zip` with diagnostics, counts, sharing guidance, and `config-redacted.yml`.
- Support bundles exclude `reputationban.db`, WAL/SHM files, server logs, and webhook URLs.
- `scripts/create-release-artifact.sh` copies the JAR, writes a checksum, and creates a release ZIP with docs only.

## Phase 13 Release Candidate Hardening

- Free-text redaction now covers simple `token abc`, `password is hunter2`, `sessionId abc`, webhook, and URL patterns.
- Support bundle metadata uses shared-safe path placeholders and continues to exclude DB files, WAL/SHM files, logs, and webhook URLs.
- `scripts/create-release-artifact.sh` now writes SHA256 files for both the JAR and release ZIP.
- `scripts/verify-release-artifact.sh` validates release contents, checksums, and the JAR `plugin.yml` version.
- Paper runtime smoke results can be recorded with `scripts/record-paper-runtime-smoke-result.sh`.

## Permissions

- `reputationban.report`: use `/reportbad`.
- `reputationban.score.self`: view your own score.
- `reputationban.score.others`: view other players' scores and histories.
- `reputationban.admin.score`: use score administration commands.
- `reputationban.admin.reports`: review reports.
- `reputationban.admin.ban`: allow ban-sensitive approvals and ban management commands.
- `reputationban.admin.audit`: view and export audit logs.
- `reputationban.admin.maintenance`: preview and run confirmed retention cleanup, and create manual DB backups.
- `reputationban.admin.diagnostics`: view operational diagnostics and create support bundles.
- `reputationban.notify`: receive staff notifications.
- `reputationban.bypass`: bypass reports, deductions, and automatic bans while online.
- `reputationban.admin`: grants the main admin permissions.

## Build And Test

```bash
./gradlew clean test build
./scripts/review_code.sh
```

The plugin jar is written to `build/libs/ReputationBan-0.13.0.jar`.

## Current Limitations

- GUI menus, permissions plugin integration, and protection plugin integrations are not implemented.
- Offline bypass detection is limited to OP status; online players with `reputationban.bypass` are protected.
- Appeal and automatic unban workflows are deferred.
- Folia support is not included.
