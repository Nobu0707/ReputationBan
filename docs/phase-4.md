# Phase 4 / v0.4.0

Phase 4 では、BAN を伴う report review と staff ban management を安全化しました。

## 主な変更

- plugin version を `0.4.0` に更新しました。
- `/reports approve` が ban threshold を跨ぐ場合に `reputationban.admin.ban` を要求します。
- approve 直前に bypass/OP protection を再確認します。
- `/rep banhistory <player> [limit]`、`/rep baninfo <player>`、`/rep unban <player> [reason]`、`/rep pardon <player> [reason]` を追加しました。
- Paper Profile BAN list による pardon を使い、name bans は使いません。
- active DB bans に `unbanned_at` と `unbanned_by` を記録します。
- pardon の score restoration を `score_history` に `source_type = pardon` で記録します。
- review scripts と review archive を Phase 4 用に更新しました。

## 検証

- `./gradlew clean test build --warning-mode all`
- `./scripts/review_code.sh`
- `./scripts/make-review-archive.sh "Phase 4"`
