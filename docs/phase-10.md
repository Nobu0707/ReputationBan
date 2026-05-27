# Phase 10 / v0.10.0

Phase 10 では、v1.0.0 line に向けて operational diagnostics と review artifact hardening を整えました。

## 主な変更

- `bans.unbanned_by_name` を guarded SQLite migration で追加しました。
- `bans.unbanned_by` は durable actor ID として player UUID string または `CONSOLE` を保持します。
- 新しい `/rep unban` と `/rep pardon` updates では `bans.unbanned_by_name` に actor display name を保存します。
- `/rep banhistory` に `解除者ID`、`解除者名`、`解除理由` を表示します。
- `/rep doctor` と `/rep diagnostics` を追加しました。
- `reputationban.admin.diagnostics` と `DIAGNOSTICS_RUN` を追加しました。
- version、server、Java、DB connectivity、required tables、config validation、Discord enabled/urlConfigured booleans、audit export directory safety、retention settings、pending report counts、threshold pending report counts、active ReputationBan DB bans を診断します。
- review archive に `checks/local-smoke-check.txt` と `./scripts/run-local-smoke-check.sh=<exit code>` を追加しました。

## Secret Handling

Doctor output と diagnostics audit metadata は Discord webhook state を booleans のみで扱います。Webhook URL 自体は表示、logging、audit CSV、diagnostics reports、review archives に含めません。

## Release Readiness

- Version: `0.10.0`
- JAR: `build/libs/ReputationBan-0.10.0.jar`
- runtime smoke checklist に `/rep doctor` を含めました。
- `review_code.sh`、`run-local-smoke-check.sh`、`make-review-archive.sh` は v0.10.0 artifacts を確認します。
