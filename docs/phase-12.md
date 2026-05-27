# Phase 12 / v0.12.0

Phase 12 では、v1.0.0 前の安全な investigation と release packaging を整えました。

## 主な変更

- version を `0.12.0` に更新しました。
- `/rep backup [reason]` を追加し、`plugins/ReputationBan/backups/` に manual SQLite backups を作成します。
- `DB_BACKUP_CREATED` audit events は relative file names と redacted reason metadata を記録します。
- `/rep support bundle` を追加し、`plugins/ReputationBan/support/` に support ZIPs を作成します。
- `SUPPORT_BUNDLE_CREATED` audit events は file-name-only metadata を記録します。
- `url`、`webhook`、`password`、`token`、`secret`、`session`、`cookie` を含む keys の config redaction を追加しました。
- `scripts/create-release-artifact.sh` を追加し、`build/release/` に JAR、checksum、release ZIP を作成します。
- review archive と smoke scripts が release artifact checks を収集するよう更新しました。

## Safety

- support bundles は SQLite DB files、WAL/SHM files、server logs を除外します。
- support bundles は live config ではなく `config-redacted.yml` を含めます。
- release ZIPs は JAR、checksum、docs のみを含めます。
- Discord webhook URLs は logs、command output、audit metadata、CSV files、support bundles、release artifacts、review archives から除外し続けます。

## Verification Targets

- `./gradlew clean test build --warning-mode all`
- `./scripts/review_code.sh`
- `./scripts/run-local-smoke-check.sh`
- `bash -n scripts/run-paper-runtime-smoke-helper.sh`
- `bash -n scripts/create-release-artifact.sh`
- `./scripts/create-release-artifact.sh`
- `./scripts/make-review-archive.sh "Phase 12"`
