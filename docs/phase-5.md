# Phase 5 / v0.5.0

Phase 5 improves command usability and ban audit metadata before adding larger external integrations.

## Changes

- Bumped plugin version to `0.5.0`.
- Adds TAB completion for `/reportbad`, `/rep`, and `/reports`.
- Adds `/rep help` and `/reports help`.
- Adds `bans.unban_reason` with a safe SQLite migration.
- Stores `unbanned_by` as the actor UUID or `CONSOLE`, separate from the unban reason.
- Stores `/rep unban` and `/rep pardon` reasons in `unban_reason`.
- Records pardon score recovery with `source_type = pardon` and the supplied reason.
- Shows unban actor and reason in `/rep banhistory`.
- Distinguishes already-cleared Profile BANs and missing active DB bans in unban messages.
- Returns clear errors for invalid limit and report ID arguments.
- Updates review scripts and review archive outputs for Phase 5.

## Validation

- `./gradlew clean test build --warning-mode all`
- `./scripts/review_code.sh`
- `./scripts/make-review-archive.sh "Phase 5"` after the Phase 5 commit
