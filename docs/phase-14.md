# Phase 14 / v0.14.0

Phase 14 は機能追加ではなく、配布前ドキュメント整備のフェーズです。README.md と docs/*.md を日本語読者向けに整え、release artifact と review archive の確認対象を v0.14.0 に更新します。

## 目的

- README.md を日本語のトップページにします。
- docs/*.md を日本語化します。
- `CHANGELOG.md` と `reputationban_phase_plan.md` も日本語読者向けに整えます。
- `scripts/review_code.sh`、`scripts/run-local-smoke-check.sh`、`scripts/create-release-artifact.sh`、`scripts/verify-release-artifact.sh`、`scripts/make-review-archive.sh` を v0.14.0 対応にします。

## 翻訳しない識別子

次のような識別子は翻訳しません。

- command names: `/rep`、`/rep doctor`、`/rep support bundle`、`/reportbad`、`/reports`
- permission nodes: `reputationban.admin`、`reputationban.admin.audit`、`reputationban.admin.backup`、`reputationban.admin.diagnostics`、`reputationban.admin.maintenance`、`reputationban.report`、`reputationban.notify`
- file names: `plugin.yml`、`config.yml`、`build.gradle.kts`、`README.md`、`CHANGELOG.md`
- package and paths: `dev.modplugin.reputationban`、`src/main/java`、`src/main/resources`、`scripts/*.sh`

## config key を変更しない方針

`initial-score`、`rating.min-unique-reports-before-deduction`、`notify.discord-webhook.url` などの YAML key は翻訳しません。説明文だけを日本語にします。

## Webhook URL を例示しない方針

Discord Webhook URL はシークレットです。docs には実 URL 風の例を追加しません。必要な場合は `<redacted>` または `<webhook-url>` のような placeholder を使います。

support bundle には DB、logs、Webhook URL を含めません。共有前に `config-redacted.yml` を確認してください。review archive にも実 Webhook URL を含めない方針です。

## v0.14.0 の検証結果

- `./gradlew clean test build --warning-mode all`: PASS
- `./scripts/review_code.sh`: PASS
- `./scripts/run-local-smoke-check.sh`: PASS
- `./scripts/create-release-artifact.sh`: PASS
- `./scripts/verify-release-artifact.sh`: PASS
- `./scripts/make-review-archive.sh "Phase 14"`: commit 後に作成
