# Phase 4 / v0.4.0

Phase 4 strengthens ban-sensitive report review and adds staff ban management commands.

## Changes

- Bumped plugin version to `0.4.0`.
- Requires `reputationban.admin.ban` when `/reports approve` would cross the ban threshold.
- Re-checks bypass/OP protection before report approval.
- Adds `/rep banhistory <player> [limit]`.
- Adds `/rep baninfo <player>`.
- Adds `/rep unban <player> [reason]`.
- Adds `/rep pardon <player> [reason]`.
- Uses Profile BAN pardon through Paper's profile ban list, not name bans.
- Marks active DB bans with `unbanned_at` and `unbanned_by`.
- Records pardon score restoration in `score_history` with `source_type = pardon`.
- Updates review scripts and review archive command status capture for Phase 4.

## Validation

- `./gradlew clean test build --warning-mode all`
- `./scripts/review_code.sh`
- `./scripts/make-review-archive.sh "Phase 4"` after the Phase 4 commit
