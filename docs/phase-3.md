# Phase 3 / v0.3.0

Phase 3 builds on the staff review workflow with reporter accountability, score recovery, and stricter manual score safety.

## Changes

- Bumped plugin version to `0.3.0`.
- Requires `reputationban.admin.ban` when a manual score change crosses from above the ban threshold to the threshold or below.
- Increments `players.false_report_count` when a pending report is rejected.
- Applies temporary report bans through `players.report_banned_until` when false report counts reach the configured threshold.
- Rejects `/reportbad` while the reporter is report-banned.
- Shows false report count and report-ban status in `/rep check <player>`.
- Adds scheduled score recovery with `score_history.source_type = recovery`.
- Adds `players.last_recovery_at` migration for recovery duplicate prevention.
- Supports `/reports list <status> [limit]`, including `cancelled` and `all`.
- Updates review scripts and review archive contents for Phase 3 checks.

## Validation

- `./gradlew clean test build`
- `./scripts/review_code.sh`
- `./scripts/make-review-archive.sh "Phase 3"` after the Phase 3 commit
