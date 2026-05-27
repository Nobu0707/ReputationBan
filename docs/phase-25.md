# Phase 25 / v0.25.0

Phase 25 は runtime smoke readiness consistency / release gate 整合フェーズです。新しい外部連携や GUI は追加せず、Paper runtime smoke / Integration runtime smoke の実行結果と readiness 判定が review archive 内で矛盾しないようにしました。

## 目的

- version を 0.25.0 に更新します。
- `make-review-archive.sh` の runtime smoke と readiness の実行順を修正します。
- latest summary が PASS なら readiness が READY/PASS、latest summary が NOT_RUN なら readiness が HOLD/NOT_RUN になることを確認します。
- integration runtime smoke で読み取れた外部プラグイン状態を `integration-status.txt` と summary に残します。

## 実装

- `scripts/make-review-archive.sh` は `run-paper-runtime-smoke.sh` の後に `check-paper-runtime-readiness.sh` を実行します。
- `scripts/make-review-archive.sh` は `run-integration-runtime-smoke.sh` の後に `check-integration-runtime-readiness.sh` を実行します。
- `scripts/check-runtime-smoke-consistency.sh` を追加しました。
- review archive に `checks/runtime-smoke-consistency.txt` を追加しました。
- `scripts/run-integration-runtime-smoke.sh` は `/rep integrations` のログから `integration-status.txt` を作成します。
- integration summary に `activeIntegrations`、`unavailableIntegrations`、必要に応じて `discordSrvUnavailableReason` を書きます。

## DiscordSRV

DiscordSRV は bot token 未設定の場合に `pluginPresent=true` でも `apiAvailable=false` になることがあります。ReputationBan の DiscordSRV 通知はデフォルト無効であり、この状態だけでは ReputationBan 本体や LuckPerms / CoreProtect / WorldGuard / GriefPrevention / PlaceholderAPI の runtime smoke PASS を取り消しません。

本番で DiscordSRV 通知や account link を使う場合は、bot token 設定済み環境で `apiAvailable=true` と `active=true` を別途確認してください。

## v0.25.0 検証結果

- `./gradlew clean test build --warning-mode all`
- `./scripts/check-docs-localization.sh`
- `./scripts/check-optional-dependency-safety.sh`
- `./scripts/run-paper-runtime-smoke.sh`
- `./scripts/check-paper-runtime-readiness.sh`
- `./scripts/run-integration-runtime-smoke.sh`
- `./scripts/check-integration-runtime-readiness.sh`
- `./scripts/check-runtime-smoke-consistency.sh`
- `./scripts/review_code.sh`
- `./scripts/run-local-smoke-check.sh`
- `./scripts/create-release-artifact.sh`
- `./scripts/verify-release-artifact.sh`
- `./scripts/make-review-archive.sh "Phase 25"`

実行結果と生成物の SHA256 は release review archive と最終報告に記録します。
