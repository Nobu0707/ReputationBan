# Phase 20 / v0.20.0

Phase 20 では PlaceholderAPI を任意連携として追加しました。目的は scoreboard、TAB、chat などの外部表示から ReputationBan の読み取り専用状態を参照できるようにすることです。

## 任意依存

- `plugin.yml`: `softdepend` に `PlaceholderAPI` を追加
- Gradle: `compileOnly("me.clip:placeholderapi:2.12.2")`
- Repository: `https://repo.helpch.at/releases/`

PlaceholderAPI が未導入でも ReputationBan 本体は起動します。

## Class Loading 方針

`PlaceholderExpansion` 継承が必要なため、PAPI API の直接 import は `ReputationBanPlaceholderExpansion` に限定しています。`PlaceholderApiIntegration` は PlaceholderAPI の plugin presence を確認してから reflection で expansion をロード、登録、解除します。

## 提供 Placeholder

- `%reputationban_score%`
- `%reputationban_max_score%`
- `%reputationban_score_percent%`
- `%reputationban_status%`
- `%reputationban_ban_count%`
- `%reputationban_false_report_count%`
- `%reputationban_report_banned%`
- `%reputationban_report_banned_until%`
- `%reputationban_last_seen%`
- `%reputationban_version%`

`status` は `normal`、`warning`、`watch`、`restricted`、`final-warning`、`banned-threshold` の英字 stable value です。既定 identifier は `reputationban` です。

## Cache 方針

placeholder の `onPlaceholderRequest` では SQLite へ同期問い合わせを行いません。値は `PlaceholderCacheService` の online player summary cache から返します。cache は player join、定期 refresh、主要な score 変更後に更新します。

Phase 20 では最大 `cache-refresh-seconds` 程度の表示遅延があり得ます。cache 未取得、offline、不明値は `show-unknown-as` を返します。`version` のみ player なしでも返します。

## 検証

v0.20.0 の検証対象は以下です。

- `./gradlew clean test build --warning-mode all`
- `scripts/check-docs-localization.sh`
- `scripts/check-optional-dependency-safety.sh`
- `scripts/review_code.sh`
- `scripts/run-local-smoke-check.sh`
- `scripts/create-release-artifact.sh`
- `scripts/verify-release-artifact.sh`
- `scripts/make-review-archive.sh "Phase 20"`

実行結果は review archive に保存します。
