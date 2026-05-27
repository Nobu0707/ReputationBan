# Phase 13 / v0.13.0

Phase 13 is the final hardening pass before moving toward a v1.0.0 release candidate.

## Implemented

- Updated project and plugin metadata to `0.13.0`.
- Strengthened free-text redaction for webhook, URL, password, token, secret, session/sessionId, and cookie-like values.
- Reduced support bundle absolute path exposure by using `<plugin-data-folder>` in shared diagnostics.
- Added support bundle safety checks for forbidden entry names, webhook URLs, URLs, and absolute paths.
- Added `scripts/verify-release-artifact.sh`.
- Updated `scripts/create-release-artifact.sh` to write SHA256 files for both the JAR and release ZIP.
- Added `docs/SECURITY_REDACTION.md`.
- Added `docs/PAPER_RUNTIME_SMOKE_REPORT_TEMPLATE.md`.
- Added `scripts/record-paper-runtime-smoke-result.sh`.
- Updated review archive generation to include release verification output and the latest Paper runtime smoke summary.

## Deferred

GUI, LuckPerms integration, CoreProtect integration, WorldGuard or other protection integrations, trust scoring, contact detection, full appeal workflows, and Folia support remain deferred.

## Validation

- `./gradlew clean test build --warning-mode all`
- `./scripts/review_code.sh`
- `./scripts/run-local-smoke-check.sh`
- `./scripts/create-release-artifact.sh`
- `./scripts/verify-release-artifact.sh`
- `./scripts/make-review-archive.sh "Phase 13"`
