# Changelog

## 0.12.0

- Added `/rep backup [reason]` for manual SQLite backups with `DB_BACKUP_CREATED` audit events.
- Added `/rep support bundle` for secret-redacted diagnostic ZIPs that exclude DB files and server logs.
- Added config redaction utilities for webhook URLs, URL-like values, passwords, tokens, secrets, sessions, and cookies.
- Added `scripts/create-release-artifact.sh` to create `build/release` JAR, SHA256, and release ZIP artifacts.
- Updated review archive, local smoke, runtime smoke, release readiness, and support bundle documentation for Phase 12.

## 0.11.0

- Added release preparation and operation documents for installation, configuration, migration, and v1.0.0 readiness.
- Added `/rep version` and TAB completion.
- Added safe Paper runtime smoke helper script.
- Extended `/rep doctor` with plugin data folder, database file, audit export, Discord webhook state, and backup directory checks without exposing webhook URLs.
- Reduced duplicated build work between review archive generation and local smoke checks.

## 0.10.0

- Added `/rep doctor` and `/rep diagnostics`.
- Added `DIAGNOSTICS_RUN` audit metadata with safe booleans and counts.
- Split `bans.unbanned_by` durable actor ID from `bans.unbanned_by_name` display name.
- Collected local smoke output in review archives.

## 0.9.0

- Added config validation and safe audit export path handling.
- Added maintenance preview and confirmed cleanup flow.
- Added SQLite backup before retention cleanup.
- Added runtime smoke checklist and review archive secret scan.

## 0.8.0

- Added `audit_events`, audit commands, CSV export, and retention policy.
- Recorded moderation, score, ban, recovery, reload, and maintenance audit events.
- Kept webhook URLs and other secrets out of audit metadata and CSV output.

## 0.7.0

- Added reporter playtime and server account age gates.
- Added multi-report threshold flow with `threshold_pending`.
- Added score threshold notifications.

## 0.6.0

- Added optional Discord webhook notifications.
- Added event-level notification toggles, JSON escaping, content truncation, and failure log rate limiting.

## 0.5.0

- Added TAB completion and help commands.
- Added `bans.unban_reason` migration and improved ban history output.
- Improved command input validation.

## 0.4.0

- Added safer ban review gates and bypass checks.
- Added ban history, ban info, unban, and pardon commands.

## 0.3.0

- Added false-report penalties and report suspension.
- Added score recovery and richer report listing.

## 0.2.0

- Added staff report review and score administration commands.
- Added score history and manual recovery workflows.

## 0.1.0

- Initial PaperMC 26.1.2 / Java 25 plugin.
- Added SQLite storage, player reputation scores, `/reportbad`, `/rep`, and basic automatic profile bans.
