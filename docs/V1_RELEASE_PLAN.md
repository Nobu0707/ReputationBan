# v1.0.0 Release Plan

この文書は ReputationBan v1.0.0 へ進むための最終確認方針です。Phase 28 では公開せず、レビュー後にユーザーが明示的に承認した次Phaseで tag 作成と GitHub Release 公開を行います。

## v1.0.0 へ進む条件

- `./gradlew clean test build --warning-mode all` が成功します。
- `./scripts/review_code.sh` が成功します。
- `./scripts/check-v1-release-gates.sh` が `READY_FOR_V1_RELEASE_REVIEW_WITH_DISCORDSRV_WARNING` または `READY_FOR_V1_RELEASE_REVIEW` を出します。
- Paper runtime smoke が PASS です。
- Integration runtime smoke が PASS です。
- Player report/evidence runtime smoke が PASS です。
- Runtime smoke consistency が PASS です。
- Optional dependency safety と docs localization が PASS です。
- Release artifact verification が PASS です。
- Secret scan に実 Discord Webhook URL がありません。
- CoreProtect rollback/restore/purge、LuckPerms 書き込み、WorldGuard 変更、GriefPrevention claim/trust 変更がありません。

## 公開前チェック

1. `scripts/generate-v1-go-no-go-report.sh` で Go/No-Go report を生成します。
2. `scripts/generate-v1-release-notes-draft.sh` で release notes draft を生成します。
3. `scripts/create-release-artifact.sh` と `scripts/verify-release-artifact.sh` を再実行します。
4. `scripts/make-review-archive.sh "Phase 28"` で review archive を作成します。
5. 生成された JAR、release ZIP、SHA256、Go/No-Go report、release notes draft を確認します。

## タグ作成方針

Phase 28 では `v1.0.0` tag を作成しません。レビュー承認後の次Phaseで、version を `1.0.0` に更新し、build/test/release artifact verification を再実行してから tag を作成します。

想定 tag:

```text
v1.0.0
```

## GitHub Release 公開方針

Phase 28 では GitHub Release を作成しません。次Phaseでユーザー承認後に release notes draft を確認し、必要な文言調整を行ってから GitHub Release を公開します。

公開前に添付予定の artifact:

- `ReputationBan-1.0.0.jar`
- `ReputationBan-1.0.0.jar.sha256`
- `ReputationBan-1.0.0-release.zip`
- `ReputationBan-1.0.0-release.zip.sha256`

## Rollback 方針

- サーバーを停止します。
- 現在の `plugins/ReputationBan/reputationban.db` を退避します。
- 直近の `plugins/ReputationBan/backups/reputationban-before-maintenance-*.db` または手動 backup を復元します。
- 直前に利用していた ReputationBan JAR へ戻します。
- 起動後に `/rep doctor`、`/rep audit recent`、主要 commands を確認します。

## DiscordSRV WARN の扱い

DiscordSRV が bot token 未設定で unavailable の場合、v1 release gate では WARN として扱います。DiscordSRV 通知はデフォルト無効であり、ReputationBan 本体や他の外部連携の release gate は止めません。

本番で DiscordSRV 通知や account link を使う場合は、bot token 設定済み環境で `apiAvailable=true`、`active=true`、通知 channel 解決、account link context 表示を追加確認します。`./scripts/check-v1-release-gates.sh --strict --require-discordsrv` はこの確認を要求する運用向けです。

## 次Phaseで行うこと

- ユーザー承認後に version を `1.0.0` へ更新します。
- `v1.0.0` tag を作成します。
- GitHub Release を作成し、release notes を確定します。
- Phase 28 の Go/No-Go report と release notes draft をもとに公開内容を最終確認します。
