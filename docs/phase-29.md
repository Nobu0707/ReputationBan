# Phase 29 / v1.0.0

Phase 29 は v1.0.0 finalization / release artifact preparation フェーズです。新しい外部連携、管理コマンド、DB schema 変更、GUI、Folia 対応は追加せず、version bump、docs、scripts、release artifact、review archive を v1.0.0 向けに整えます。

## 目的

- version を `1.0.0` に更新します。
- `ReputationBan-1.0.0.jar` を生成します。
- `ReputationBan-1.0.0-release.zip` と SHA256 を生成します。
- v1.0.0 final artifact として Go/No-Go report と release notes final candidate を生成します。
- Paper runtime smoke と Integration runtime smoke を 1.0.0 JAR で再実行して PASS を確認します。
- Player report/evidence runtime smoke は Phase 27 の手動 PASS を `carriedForwardFrom=0.27.0` として carry-forward 記録します。
- DiscordSRV は bot token 未設定時 WARN 扱いとし、ReputationBan 本体や他連携の release gate は止めません。

## Runtime gates

- Paper runtime smoke: PASS
- Integration runtime smoke: PASS
- Player report/evidence runtime smoke: PASS, carried forward from `0.27.0`
- Runtime smoke consistency: PASS
- v1 release gates judgment: `READY_FOR_V1_RELEASE_WITH_DISCORDSRV_WARNING`

Player report/evidence runtime smoke の carry-forward 理由は、Phase 29 が version/docs/scripts/release artifacts の更新に限定され、`/reportbad`、`/reports evidence`、`report_context` 生成、DB schema の runtime behavior を変更しないためです。

## Release status

- `v1.0.0` tag: NOT_CREATED
- GitHub Release: NOT_CREATED
- Go/No-Go report: `build/release/ReputationBan-v1-go-no-go-report.md`
- Release notes final candidate: `build/release/ReputationBan-v1.0.0-release-notes.md`
- Release execution plan: `docs/V1_RELEASE_EXECUTION_PLAN.md`

## 次Phaseで行うこと

- ユーザーの明示承認を得ます。
- `git status --short`、`git log --oneline -5`、`git tag --list "v1.0.0"` を確認します。
- v1.0.0 tag を作成し、push します。
- GitHub Release draft を作成し、release notes と artifacts を確認してから公開します。
