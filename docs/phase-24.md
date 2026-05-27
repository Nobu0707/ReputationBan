# Phase 24 / v0.24.0

Phase 24 は、新しい外部連携機能を追加せず、`~/servers/PaperPlugins/` の外部連携プラグイン JAR を使った Integration runtime smoke を自動化し、記録しやすくするフェーズです。

## 目的

- version を 0.24.0 に更新します。
- `scripts/run-integration-runtime-smoke.sh` で `~/servers/PaperPlugins/*.jar` と `build/libs/ReputationBan-0.24.0.jar` を Paper test server の `plugins/` に staging します。
- `~/servers/paper-26.1.2/start.sh` が `screen` で起動する PaperMC 26.1.2 / Java 25 server に対して、`/rep doctor`、`/rep integrations`、`/rep integrations test`、`/rep placeholders` などを console 実行します。
- `build/manual-smoke/integration-runtime-YYYYMMDD-HHMMSS/` に summary、commands、environment、screen snapshots、server log、staged plugin list、restore log を残します。

## Backup / Restore 方針

- 既存の `ReputationBan*.jar`、`LuckPerms*.jar`、`CoreProtect*.jar`、`WorldEdit*.jar`、`WorldGuard*.jar`、`GriefPrevention*.jar`、`PlaceholderAPI*.jar`、`DiscordSRV*.jar` は `plugins/backups/reputationban-integration-smoke-YYYYMMDD-HHMMSS/` に退避します。
- 既定では `REPUTATIONBAN_INTEGRATION_RESTORE_PLUGINS=1` です。smoke 後に今回 staging した外部連携 JAR を削除し、backup 済み JAR を復元します。
- ReputationBan の v0.24.0 JAR は test server に残す方針です。
- `plugins/` 全体、`config.yml`、SQLite DB、logs、Paper server directory は削除しません。

## Runtime 前提

- 連携プラグイン JAR 置き場: `~/servers/PaperPlugins/`
- Paper server: `~/servers/paper-26.1.2/`
- start script: `~/servers/paper-26.1.2/start.sh`
- start script は `screen` で Paper server を起動する前提です。
- `REPUTATIONBAN_SCREEN_NAME` が指定されていればその session を使い、未指定なら start 前後の `screen -ls` 差分と paper/minecraft/mc/server 候補から session を特定します。

## 自動判定

- PASS は Paper server 起動、ReputationBan load、外部連携 JAR staging、`/rep doctor`、`/rep integrations`、`/rep integrations test` 実行、主要 class loading failure 不在を確認できた場合だけです。
- `~/servers/PaperPlugins/`、`*.jar`、Paper directory、start script、または `screen` がない場合は `NOT_RUN` で exit 0 します。
- ReputationBan の起動失敗、`NoClassDefFoundError`、`ClassNotFoundException`、screen session 特定失敗は FAIL です。

## 手動確認が残る内容

`/reportbad` と `/reports evidence` による `report_context` の実生成確認は、console だけでは完結しません。実プレイヤー2名以上で、LuckPerms / CoreProtect / WorldGuard / GriefPrevention / DiscordSRV の文脈が保存・表示されることを手動確認してください。

## v0.24.0 検証結果

- build / JUnit / review / release artifact verification は Phase 24 実装後に実行します。
- integration runtime smoke は実環境が使える場合のみ PASS を記録します。環境が足りない場合は `NOT_RUN` として summary と review archive に残します。
