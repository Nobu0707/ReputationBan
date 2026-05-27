# Phase 3 / v0.3.0

Phase 3 では、reporter accountability、score recovery、manual score safety を強化しました。

## 主な変更

- plugin version を `0.3.0` に更新しました。
- manual score change が ban threshold を跨ぐ場合に `reputationban.admin.ban` を要求します。
- `/reports reject` 時に `players.false_report_count` を加算します。
- false report count がしきい値に達した場合、`players.report_banned_until` による一時 report ban を適用します。
- report-banned reporter の `/reportbad` を拒否します。
- `/rep check <player>` に false report count と report-ban status を表示します。
- `score-recovery` による scheduled recovery と `score_history.source_type = recovery` を追加しました。
- `players.last_recovery_at` migration を追加しました。
- `/reports list <status> [limit]` に `cancelled` と `all` を含めました。
- review scripts と review archive を Phase 3 用に更新しました。

## 検証

- `./gradlew clean test build`
- `./scripts/review_code.sh`
- `./scripts/make-review-archive.sh "Phase 3"`
