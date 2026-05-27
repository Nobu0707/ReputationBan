# Installation

ReputationBan 0.11.0 targets PaperMC 26.1.2 and Java 25.

## Requirements

- PaperMC 26.1.2 test or production server
- Java 25 runtime
- `build/libs/ReputationBan-0.11.0.jar`
- SQLite JDBC is loaded through `plugin.yml`

## First Install

1. Stop the Paper server.
2. Copy `ReputationBan-0.11.0.jar` to the server `plugins/` directory.
3. Start the server once to generate `plugins/ReputationBan/config.yml`.
4. Confirm `plugins/ReputationBan/reputationban.db` is created.
5. Stop and restart the server after reviewing configuration.
6. Run `/rep version`.
7. Run `/rep doctor`.

Discord webhook notifications are disabled by default. If enabled later, treat the webhook URL as a secret and do not paste it into tickets, logs, review archives, or screenshots.

## Smoke Commands

Use these on a test server before production rollout:

- `/plugins`
- `/rep version`
- `/rep help`
- `/rep doctor`
- `/reports list all 10`
- `/rep audit recent 10`
- `/rep maintenance preview`

Use BAN-related commands only with test users during smoke checks.
