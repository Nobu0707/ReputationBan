# Release Readiness

Use this checklist before treating 0.13.0 as ready for a v1.0.0 candidate.

- `./gradlew clean test build --warning-mode all` succeeds.
- `./scripts/review_code.sh` succeeds.
- `./scripts/run-local-smoke-check.sh` succeeds.
- `./scripts/create-release-artifact.sh` succeeds.
- `./scripts/verify-release-artifact.sh` succeeds.
- `./scripts/make-review-archive.sh "Phase 13"` creates an archive.
- `scripts/run-paper-runtime-smoke-helper.sh` passes `bash -n`.
- `scripts/create-release-artifact.sh` passes `bash -n`.
- `scripts/verify-release-artifact.sh` passes `bash -n`.
- `scripts/record-paper-runtime-smoke-result.sh` passes `bash -n`.
- Paper runtime smoke is performed on a PaperMC 26.1.2 server with Java 25.
- `config.yml` is generated and reviewed.
- `/rep version` reports 0.13.0.
- `/rep doctor` reports expected database, table, config, audit export, Discord, and backup status.
- `/rep backup before-release` creates `backups/reputationban-manual-backup-*.db`.
- `/rep support bundle` creates `support/reputationban-support-*.zip`.
- The support bundle contains `meta.txt`, `doctor.txt`, `counts.txt`, `config-redacted.yml`, and `README-SHARING.txt`.
- The support bundle does not contain SQLite DB files, server logs, webhook URLs, or shared-unnecessary absolute paths.
- `/rep maintenance preview` does not delete data.
- `/rep audit export recent 10` writes a CSV under the configured safe export directory.
- `build/release/ReputationBan-0.13.0.jar`, `.jar.sha256`, `ReputationBan-0.13.0-release.zip`, and `ReputationBan-0.13.0-release.zip.sha256` exist.
- The release ZIP contains the JAR, checksum, README, CHANGELOG, and install/config/migration/readiness docs.
- The release ZIP does not contain live `config.yml`, SQLite DB files, or logs.
- Paper runtime smoke results are recorded with `scripts/record-paper-runtime-smoke-result.sh` when a manual smoke run is performed.
- Discord webhook is disabled by default.
- Webhook URL values do not appear in logs, command output, audit metadata, CSV files, or review archives.
- BAN-related checks use test users only.
- Existing SQLite DB and server directories are not deleted by helper scripts.
