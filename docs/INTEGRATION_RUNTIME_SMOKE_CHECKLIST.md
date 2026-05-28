# Integration Runtime Smoke Checklist

Phase 29 では LuckPerms / CoreProtect / WorldEdit / WorldGuard / GriefPrevention / PlaceholderAPI / DiscordSRV の JAR を `~/servers/PaperPlugins/` から Paper test server へ staging し、ReputationBan 1.0.0 が連携込みで起動・診断できるかを自動確認します。未実施でも local release checks は失敗扱いにしませんが、未実施は PASS ではありません。v1.0.0 final artifact 前には実施し、`run-integration-runtime-smoke.sh` または `record-integration-runtime-smoke-result.sh` で結果を記録してください。

## Runtime Gate

- summary がない場合は `NOT_RUN` です。
- `NOT_RUN` は `HOLD_FOR_INTEGRATION_RUNTIME_SMOKE` として扱い、PASS とは扱いません。
- `./scripts/check-integration-runtime-readiness.sh` は通常モードで HOLD を表示し exit 0 にします。
- `./scripts/check-integration-runtime-readiness.sh --strict` は `result=PASS` 以外を non-zero にします。
- PASS を記録できるのは、実 Paper server で optional plugin なし/ありの確認を行った場合だけです。

## 自動 smoke

- 連携プラグイン JAR 置き場: `~/servers/PaperPlugins/`
- Paper server: `~/servers/paper-26.1.2/`
- start script: `~/servers/paper-26.1.2/start.sh`
- 自動実行: `./scripts/run-integration-runtime-smoke.sh`
- plugin dir override: `REPUTATIONBAN_INTEGRATION_PLUGIN_DIR`
- restore override: `REPUTATIONBAN_INTEGRATION_RESTORE_PLUGINS`
- `REPUTATIONBAN_INTEGRATION_RESTORE_PLUGINS=1` が既定です。既存対象 JAR を backup し、smoke 後に外部連携 JAR を削除して既存 JAR を復元します。ReputationBan JAR は test server に残す方針です。
- `~/servers/PaperPlugins/`、`*.jar`、Paper directory、start script、または `screen` がない場合は `NOT_RUN` summary を作成して exit 0 します。
- summary、server log、commands、environment、screen snapshots、staged plugins、restore log、`integration-status.txt` は `build/manual-smoke/integration-runtime-YYYYMMDD-HHMMSS/` に保存されます。
- summary には `activeIntegrations` と `unavailableIntegrations` が記録されます。

## 共通確認

- PaperMC 26.1.2 と Java 25 で起動します。
- `build/libs/ReputationBan-1.0.0.jar` を配置します。
- 自動 smoke は `version`、`plugins`、`rep version`、`rep doctor`、`rep integrations`、`rep integrations test`、`rep placeholders`、`reports help`、`rep audit recent 5`、`rep maintenance preview` を console へ投入します。
- `/reportbad` と `/reports evidence` による report_context の実生成確認は、実プレイヤー2名以上で `docs/PLAYER_REPORT_RUNTIME_SMOKE_CHECKLIST.md` に沿って手動確認が必要です。未実施なら `check-player-report-runtime-readiness.sh` は `HOLD_FOR_PLAYER_REPORT_RUNTIME_SMOKE` を表示します。
- CoreProtect rollback、restore、purge は使いません。
- LuckPerms の group 変更、権限付与、権限剥奪、`saveUser` などの書き込みは行いません。
- WorldGuard region / flag の作成、変更、削除は行いません。
- GriefPrevention claim / trust の作成、変更、削除は行いません。
- Discord から Minecraft コマンドを実行する機能、Discord role 変更、Discord button 承認は行いません。
- DiscordSRV 通知はデフォルト無効です。明示的に有効化した smoke 以外では送信されないことを確認します。
- DiscordSRV は bot token 未設定だと `apiAvailable=false` になる場合があります。ReputationBan の起動・他連携確認とは別に、DiscordSRV 通知や account link 確認を行うには bot token 設定済み環境で追加テストしてください。
- DiscordSRV unavailable だけで integration runtime smoke の PASS は取り消しません。`check-integration-runtime-readiness.sh` は WARN として理由を表示します。

## LuckPerms 未導入

- `/rep integrations` で LuckPerms の `pluginPresent=false` を確認します。
- `/rep integrations test` で LuckPerms API unavailable が安全に表示されることを確認します。
- `/rep doctor` が失敗しないことを確認します。

## CoreProtect 未導入

- `/rep integrations` で CoreProtect の `pluginPresent=false` を確認します。
- `/rep integrations test` で CoreProtect API unavailable が安全に表示されることを確認します。
- `/reportbad griefing` 相当の通報が CoreProtect なしでも成功することを確認します。

## WorldGuard 未導入

- `/rep integrations` で WorldGuard の `worldGuardPresent=false` を確認します。
- `/rep integrations test` で WorldGuard API unavailable が安全に表示されることを確認します。
- `/reportbad griefing` 相当の通報が WorldGuard なしでも成功することを確認します。

## WorldEdit のみ導入

- `/rep integrations` で `worldEditPresent=true`、`worldGuardPresent=false` を確認します。
- `/rep integrations test` が失敗せず、WorldGuard region test が unavailable として扱われることを確認します。

## WorldGuard + WorldEdit 導入

- `/rep integrations` で WorldGuard の `worldEditPresent=true`、`worldGuardPresent=true`、`apiAvailable=true`、`active=true` を確認します。
- player 実行の `/rep integrations test` で `currentRegions` と `regionCount` が表示されることを確認します。
- console 実行の `/rep integrations test` で `WorldGuard region test: console sender, skipped` が表示されることを確認します。
- WorldGuard region 内で `/reportbad <player> griefing <reason>` を実行し、`/reports evidence <id>` に WorldGuard region context が表示されることを確認します。
- WorldGuard region / flag が変更されていないことを確認します。

## GriefPrevention 未導入

- `/rep integrations` で GriefPrevention の `pluginPresent=false` を確認します。
- `/rep integrations test` で GriefPrevention API unavailable が安全に表示されることを確認します。
- `/reportbad griefing` 相当の通報が GriefPrevention なしでも成功することを確認します。

## GriefPrevention 導入

- `/rep integrations` で GriefPrevention の `pluginPresent=true`、`apiAvailable=true`、`active=true` を確認します。
- player 実行の `/rep integrations test` で `currentClaimPresent`、`adminClaim`、`claimId` が表示されることを確認します。
- console 実行の `/rep integrations test` で `GriefPrevention claim test: console sender, skipped` が表示されることを確認します。
- claim 内と claim 外で `/reportbad <player> griefing <reason>` を実行し、`/reports evidence <id>` に claim 有無が表示されることを確認します。
- GriefPrevention claim / trust が変更されていないことを確認します。

## PlaceholderAPI 未導入

- `/rep integrations` で PlaceholderAPI の `pluginPresent=false` を確認します。
- `/rep integrations test` で PlaceholderAPI が unavailable として安全に表示されることを確認します。
- `/rep placeholders` で一覧が表示され、未導入のため外部プラグインからは利用できない旨が表示されることを確認します。

## PlaceholderAPI 導入

- `/rep integrations` で PlaceholderAPI の `pluginPresent=true`、`apiAvailable=true`、`active=true`、`identifier=reputationban` を確認します。
- `/rep integrations test` で `registered=true` と sample placeholder が表示されることを確認します。
- `/rep placeholders` で `%reputationban_score%` などの一覧が表示されることを確認します。
- PlaceholderAPI 対応プラグイン、または `/papi parse <player> %reputationban_score%` で値が返ることを確認します。
- score 変更後、`cache-refresh-seconds` 以内に placeholder 値へ反映されることを確認します。

## DiscordSRV 未導入

- `/rep integrations` で DiscordSRV の `pluginPresent=false` を確認します。
- `/rep integrations test` で DiscordSRV API unavailable が安全に表示されることを確認します。
- `/rep doctor` が失敗しないことを確認します。
- `/reportbad <player> griefing <reason>` が DiscordSRV なしでも成功することを確認します。

## DiscordSRV 導入

- `/rep integrations` で DiscordSRV の `pluginPresent=true`、`apiAvailable=true`、`active=true` を確認します。
- player 実行の `/rep integrations test` で `accountLinkAvailable`、`senderLinked`、`discordId=hidden` が表示されることを確認します。
- console 実行の `/rep integrations test` で `DiscordSRV account link test: console sender, skipped` が表示されることを確認します。
- account link 済み player と未リンク player で `/reportbad <player> griefing <reason>` を実行し、`/reports evidence <id>` に reporter/target の linked/unlinked が表示されることを確認します。
- `include-discord-ids: false` の既定で Discord ID が表示されないことを確認します。
- `integrations.discordsrv.notifications.enabled: true` にした場合だけ、`report-created` 通知が DiscordSRV 側に送信されることを確認します。
- Discord から Minecraft コマンドを実行していないことを確認します。

## LuckPerms のみ導入

- `/rep integrations` で LuckPerms の `pluginPresent=true` と `apiAvailable=true` を確認します。
- player 実行の `/rep integrations test` で primary group、reporterWeight、bypassGroup が表示されることを確認します。
- console 実行の `/rep integrations test` で `LuckPerms user test: console sender, skipped` が表示されることを確認します。

## CoreProtect のみ導入

- `/rep integrations` で CoreProtect の API version、minimumApiVersion、lookupSeconds、radius、maxResults を確認します。
- `/rep integrations test` で実 lookup が skipped と表示されることを確認します。
- griefing report 後、`/reports evidence <id>` で CoreProtect summary または「保存された連携情報なし」が安全に表示されることを確認します。

## LuckPerms + CoreProtect + WorldGuard + GriefPrevention + PlaceholderAPI + DiscordSRV 導入

- `/rep integrations`
- `/rep integrations test`
- `/rep doctor`
- `/rep placeholders`
- `/reportbad <player> griefing <reason>`
- `/reports evidence <id>`
- `/papi parse <player> %reputationban_score%`
- claim 内 / claim 外の表示確認
- account link 済み / 未リンクの DiscordSRV 表示確認

上記を順番に確認し、通報処理が外部連携の有無に依存して失敗しないことを確認します。

## 結果記録

```bash
./scripts/record-integration-runtime-smoke-result.sh --result PASS --scenario "WorldGuard" --note "manual smoke passed"
./scripts/record-integration-runtime-smoke-result.sh --result PASS --scenario "GriefPrevention" --note "manual smoke passed"
./scripts/record-integration-runtime-smoke-result.sh --result PASS --scenario "PlaceholderAPI" --note "manual smoke passed"
./scripts/record-integration-runtime-smoke-result.sh --result PASS --scenario "DiscordSRV" --note "manual smoke passed"
./scripts/record-integration-runtime-smoke-result.sh --result PASS --scenario "All integrations" --note "manual smoke passed"
```

結果は `build/manual-smoke/integration-runtime-YYYYMMDD-HHMMSS/summary.txt` に保存され、review archive が最新 summary を収集します。
