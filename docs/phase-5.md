# Phase 5 / v0.5.0

Phase 5 では、command usability と BAN 解除監査情報を改善しました。

## 主な変更

- plugin version を `0.5.0` に更新しました。
- `/reportbad`、`/rep`、`/reports` の TAB completion を追加しました。
- `/rep help` と `/reports help` を追加しました。
- `bans.unban_reason` migration を追加しました。
- `unbanned_by` と `unban_reason` を分離しました。
- `/rep unban` と `/rep pardon` の message を改善しました。
- `/rep banhistory` に解除者と解除理由を表示します。
- invalid limit と report ID の error を明確化しました。
- review scripts と review archive を Phase 5 用に更新しました。

## 検証

- `./gradlew clean test build --warning-mode all`
- `./scripts/review_code.sh`
- `./scripts/make-review-archive.sh "Phase 5"`
