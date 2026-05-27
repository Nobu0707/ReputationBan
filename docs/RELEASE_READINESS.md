# Release Readiness

Use this checklist before treating 0.11.0 as ready for a v1.0.0 candidate.

- `./gradlew clean test build --warning-mode all` succeeds.
- `./scripts/review_code.sh` succeeds.
- `./scripts/run-local-smoke-check.sh` succeeds.
- `./scripts/make-review-archive.sh "Phase 11"` creates an archive.
- `scripts/run-paper-runtime-smoke-helper.sh` passes `bash -n`.
- Paper runtime smoke is performed on a PaperMC 26.1.2 server with Java 25.
- `config.yml` is generated and reviewed.
- `/rep version` reports 0.11.0.
- `/rep doctor` reports expected database, table, config, audit export, Discord, and backup status.
- `/rep maintenance preview` does not delete data.
- `/rep audit export recent 10` writes a CSV under the configured safe export directory.
- Discord webhook is disabled by default.
- Webhook URL values do not appear in logs, command output, audit metadata, CSV files, or review archives.
- BAN-related checks use test users only.
- Existing SQLite DB and server directories are not deleted by helper scripts.
