# Release Readiness

0.15.0 を v1.0.0 candidate へ進める前に、次の項目を確認してください。

- `./gradlew clean test build --warning-mode all` が成功します。
- `./scripts/review_code.sh` が成功します。
- `./scripts/check-docs-localization.sh` が成功します。
- `./scripts/run-local-smoke-check.sh` が成功します。
- `./scripts/create-release-artifact.sh` が成功します。
- `./scripts/verify-release-artifact.sh` が成功します。
- `./scripts/make-review-archive.sh "Phase 15"` が archive を作成し、`checks/docs-localization.txt` と `checks/latest-paper-runtime-smoke-summary.txt` を含みます。
- `bash -n scripts/run-paper-runtime-smoke-helper.sh` が成功します。
- `bash -n scripts/create-release-artifact.sh` が成功します。
- `bash -n scripts/verify-release-artifact.sh` が成功します。
- `bash -n scripts/record-paper-runtime-smoke-result.sh` が成功します。
- Paper runtime smoke を PaperMC 26.1.2 server と Java 25 で実施します。
- v1.0.0に進む前に、可能な限り実Paperサーバーで /rep version、/rep doctor、/rep support bundle、/rep backup、/reportbad TAB補完を確認してください。
- `config.yml` が生成され、内容を確認済みです。
- `/rep version` が 0.15.0 を表示します。
- `/rep doctor` が database、tables、config、audit export、Discord、backup status を期待通りに表示します。
- `/rep backup before-release` が `backups/reputationban-manual-backup-*.db` を作成します。
- `/rep support bundle` が `support/reputationban-support-*.zip` を作成します。
- support bundle に `meta.txt`、`doctor.txt`、`counts.txt`、`config-redacted.yml`、`README-SHARING.txt` が含まれます。
- support bundle に SQLite DB files、server logs、Webhook URLs、共有不要な absolute paths が含まれません。
- `/rep maintenance preview` は data を削除しません。
- `/rep audit export recent 10` は安全な export directory 配下に CSV を作成します。
- `build/release/ReputationBan-0.15.0.jar`、`.jar.sha256`、`ReputationBan-0.15.0-release.zip`、`ReputationBan-0.15.0-release.zip.sha256` が存在します。
- release ZIP には JAR、checksum、README、CHANGELOG、docs が含まれます。
- release ZIP 内の README.md と docs/INSTALLATION.md が日本語ドキュメントとして読めることを確認します。
- release ZIP に live `config.yml`、SQLite DB files、logs は含まれません。
- Paper runtime smoke を手動実施した場合は `scripts/record-paper-runtime-smoke-result.sh` で結果を記録します。未実施の場合は PASS 扱いにせず、review archive に `status=NOT_RUN` として残します。
- Discord Webhook はデフォルトで無効です。
- Webhook URL values は logs、command output、audit metadata、CSV files、review archives に出ません。
- BAN 関連確認は test users のみに行います。
- helper scripts は既存 SQLite DB や server directories を削除しません。
- secret scan は確認用です。説明文に `token` や `secret` が含まれるだけでは即失敗にしません。
