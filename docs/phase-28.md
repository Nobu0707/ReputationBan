# Phase 28 / v0.28.0

Phase 28 は v1.0.0 release candidate readiness review フェーズです。新しい管理コマンド、外部連携、DBスキーマ変更は追加せず、v1.0.0 へ進むための最終確認資料と自動判定を整えます。

## 目的

- v0.28.0 として release candidate review の準備を行います。
- `scripts/check-v1-release-gates.sh` で v1 release gates をまとめて確認します。
- `scripts/generate-v1-go-no-go-report.sh` で Go/No-Go report を生成します。
- `scripts/generate-v1-release-notes-draft.sh` で v1.0.0 release notes draft を生成します。
- release artifact と review archive に v1準備資料を含めます。
- DiscordSRV unavailable WARN の扱いを明文化します。

## v1 Release Gates

- Paper runtime smoke: PASS
- Integration runtime smoke: PASS
- Player report/evidence runtime smoke: PASS
- Runtime smoke consistency: PASS
- Optional dependency safety: PASS
- Docs localization: PASS
- Release artifact verification: PASS
- Secret scan: PASS
- No destructive integration operations: PASS

`./scripts/check-v1-release-gates.sh` の通常モードでは DiscordSRV unavailable を WARN として表示し、他の必須 gate が PASS なら v1 review へ進めます。`--require-discordsrv` を付けた場合は DiscordSRV が unavailable のままなら HOLD とします。

## Go/No-Go Report

`./scripts/generate-v1-go-no-go-report.sh` は `build/release/ReputationBan-v1-go-no-go-report.md` を生成します。対象 commit、現在 version、JAR、JAR SHA256、release ZIP、release ZIP SHA256、gate 結果、外部連携状態、DiscordSRV WARN、最終 judgment を日本語でまとめます。

Phase 28 の既定 judgment は `READY_FOR_V1_RELEASE_REVIEW_WITH_DISCORDSRV_WARNING` です。

## Release Notes Draft

`./scripts/generate-v1-release-notes-draft.sh` は `build/release/ReputationBan-v1.0.0-release-notes-draft.md` を生成します。GitHub Release に貼れる日本語の下書きとして、概要、対応環境、主な機能、重要な注意、検証状況、SHA256 を含みます。

## DiscordSRV WARN

DiscordSRV は bot token 未設定などで `apiAvailable=false` になる場合があります。この状態は `WARNING_UNAVAILABLE` として扱います。

- DiscordSRV 通知はデフォルト無効です。
- ReputationBan 本体、Paper runtime smoke、他の外部連携 runtime smoke の release gate は止めません。
- 本番で DiscordSRV 通知や account link を使う場合は、bot token 設定済み環境で追加確認が必要です。

## v0.28.0 の検証結果

Phase 28 では次を確認対象にします。

- `./gradlew clean test build --warning-mode all`
- `./scripts/check-docs-localization.sh`
- `./scripts/check-optional-dependency-safety.sh`
- `./scripts/check-paper-runtime-readiness.sh`
- `./scripts/check-integration-runtime-readiness.sh`
- `./scripts/check-player-report-runtime-readiness.sh`
- `./scripts/check-runtime-smoke-consistency.sh`
- `./scripts/check-v1-release-gates.sh`
- `./scripts/generate-v1-go-no-go-report.sh`
- `./scripts/generate-v1-release-notes-draft.sh`
- `./scripts/review_code.sh`
- `./scripts/run-local-smoke-check.sh`
- `./scripts/create-release-artifact.sh`
- `./scripts/verify-release-artifact.sh`
- `./scripts/make-review-archive.sh "Phase 28"`

Phase 28 では v1.0.0 tag 作成や GitHub Release 公開は行いません。
