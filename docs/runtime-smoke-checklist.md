# Runtime Smoke Checklist

## Environment

- PaperMC 26.1.2
- Java 25
- Fresh or disposable test server
- ReputationBan config with Discord webhook disabled by default

## Install

1. Run `./gradlew clean test build --warning-mode all`.
2. Copy `build/libs/ReputationBan-0.9.0.jar` to the Paper `plugins` directory.
3. Start Paper with Java 25.

## Startup

- Verify `/plugins` lists ReputationBan.
- Verify `plugins/ReputationBan/config.yml` exists.
- Verify `plugins/ReputationBan/reputationban.db` exists.
- Confirm startup logs do not print a Discord webhook URL.

## Commands

- `/rep help`
- `/reports help`
- `/reportbad <TAB>` and category TAB completion
- `/rep audit recent`
- `/rep audit export recent`
- `/rep maintenance preview`
- `/rep maintenance run`
- `/rep maintenance run confirm`

## Safety Checks

- Confirm `/rep maintenance preview` only displays counts.
- Confirm `/rep maintenance run` does not delete data and asks for `run confirm`.
- Confirm `/rep maintenance run confirm` creates `plugins/ReputationBan/backups/reputationban-before-maintenance-*.db`.
- Confirm Discord webhook is disabled by default.
- Confirm webhook URLs are not printed in logs, audit output, CSV output, or review archive files.
- Run BAN and pardon commands only against disposable test users.

## Rollback

- Stop the server.
- Move the current `plugins/ReputationBan/reputationban.db` aside.
- Restore the latest `plugins/ReputationBan/backups/reputationban-before-maintenance-*.db`.
- Start the server and re-check `/rep audit recent`.
