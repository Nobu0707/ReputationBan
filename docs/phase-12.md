# Phase 12 / v0.12.0

Phase 12 prepares ReputationBan for safer pre-v1.0.0 investigation and release packaging.

## Implemented

- Version updated to `0.12.0`.
- Added `/rep backup [reason]` for manual SQLite backups under `plugins/ReputationBan/backups/`.
- Added `DB_BACKUP_CREATED` audit events with relative file names and redacted reason metadata.
- Added `/rep support bundle` for support ZIPs under `plugins/ReputationBan/support/`.
- Added `SUPPORT_BUNDLE_CREATED` audit events with file-name-only metadata.
- Added config redaction for keys containing `url`, `webhook`, `password`, `token`, `secret`, `session`, or `cookie`.
- Added `scripts/create-release-artifact.sh` for JAR, checksum, and release ZIP creation under `build/release/`.
- Updated review archive and smoke scripts to collect release artifact checks.

## Safety

- Support bundles exclude SQLite DB files, WAL/SHM files, and server logs.
- Support bundles include `config-redacted.yml`, not the live config.
- Release ZIPs include the JAR, checksum, and docs only.
- Discord webhook URLs remain excluded from logs, command output, audit metadata, CSV files, support bundles, release artifacts, and review archives.

## Verification Targets

- `./gradlew clean test build --warning-mode all`
- `./scripts/review_code.sh`
- `./scripts/run-local-smoke-check.sh`
- `bash -n scripts/run-paper-runtime-smoke-helper.sh`
- `bash -n scripts/create-release-artifact.sh`
- `./scripts/create-release-artifact.sh`
- `./scripts/make-review-archive.sh "Phase 12"`
