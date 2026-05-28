# Runtime Smoke Checklist

## Environment

- PaperMC 26.1.2
- Java 25
- Fresh または disposable test server
- Discord webhook がデフォルト無効の ReputationBan config
- 既定の Paper server directory は `~/servers/paper-26.1.2/` です。
- 既定の start script は `~/servers/paper-26.1.2/start.sh` です。
- `start.sh` は `screen` で Paper server を起動する前提です。

## Automated Paper Smoke

Phase 23 以降は、次の Paper 単体自動化スクリプトを使えます。

```bash
./scripts/run-paper-runtime-smoke.sh
./scripts/check-paper-runtime-readiness.sh
./scripts/check-paper-runtime-readiness.sh --strict
./scripts/check-runtime-smoke-consistency.sh
```

`scripts/run-paper-runtime-smoke.sh` は `build/libs/ReputationBan-0.27.0.jar` を `plugins/` に配置し、起動前後の `screen -ls` を記録し、特定した screen session に console command を投入します。環境が見つからない場合は `build/manual-smoke/paper-runtime-*` に `status=NOT_RUN` / `result=NOT_RUN` を記録し、PASS にはしません。

主な環境変数:

- `REPUTATIONBAN_PAPER_DIR`: Paper server directory。未設定時は `~/servers/paper-26.1.2`
- `REPUTATIONBAN_PAPER_START_SCRIPT`: start script。未設定時は `~/servers/paper-26.1.2/start.sh`
- `REPUTATIONBAN_SCREEN_NAME`: 既存 screen session を明示する場合に指定
- `REPUTATIONBAN_SMOKE_STOP_SERVER`: `1` の場合だけ既存 session も stop 対象にします。未設定時は既存 session を止めません。
- `REPUTATIONBAN_SMOKE_MUTATING`: `1` の場合だけ `rep backup runtime-smoke` と `rep support bundle` も投入します。

`scripts/check-paper-runtime-readiness.sh` の通常モードは `NOT_RUN` でも exit 0 とし、`judgment: HOLD_FOR_PAPER_RUNTIME_SMOKE` を表示します。`--strict` は `result=PASS` または `status=PASS` 以外を non-zero にします。

## Automated Integration Smoke

Phase 25 では、`~/servers/PaperPlugins/` の外部連携プラグイン JAR を使う integration runtime smoke を自動化します。

```bash
./scripts/run-integration-runtime-smoke.sh
./scripts/check-integration-runtime-readiness.sh
./scripts/check-integration-runtime-readiness.sh --strict
./scripts/check-runtime-smoke-consistency.sh
```

`scripts/run-integration-runtime-smoke.sh` は `build/libs/ReputationBan-0.27.0.jar` と `REPUTATIONBAN_INTEGRATION_PLUGIN_DIR` の `*.jar` を Paper `plugins/` に配置し、対象 plugin JAR を backup してから `screen` session に `/rep doctor`、`/rep integrations`、`/rep integrations test`、`/rep placeholders` などを投入します。既定では `REPUTATIONBAN_INTEGRATION_RESTORE_PLUGINS=1` で、外部連携 JAR を削除し、backup した既存 JAR を復元します。`~/servers/PaperPlugins/`、`*.jar`、Paper directory、start script、または `screen` が見つからない場合は `build/manual-smoke/integration-runtime-*` に `status=NOT_RUN` / `result=NOT_RUN` を記録し、PASS にはしません。

`scripts/run-integration-runtime-smoke.sh` は `/rep integrations` のログから `integration-status.txt` を作成し、summary に `activeIntegrations` と `unavailableIntegrations` を書きます。DiscordSRV が bot token 未設定などで `apiAvailable=false` の場合は `DiscordSRV=unavailable` と理由を残し、ReputationBan 本体と他連携の起動確認を妨げない WARN として扱います。

`scripts/check-runtime-smoke-consistency.sh` は latest summary が PASS なら readiness が READY/PASS、latest summary が NOT_RUN なら readiness が HOLD/NOT_RUN であることを確認します。矛盾があれば non-zero です。

主な環境変数:

- `REPUTATIONBAN_INTEGRATION_PLUGIN_DIR`: 連携プラグイン JAR directory。未設定時は `~/servers/PaperPlugins`
- `REPUTATIONBAN_INTEGRATION_RESTORE_PLUGINS`: `1` の場合に staging した外部 JAR を削除し backup を復元します。未設定時は `1`
- `REPUTATIONBAN_PAPER_DIR`: Paper server directory。未設定時は `~/servers/paper-26.1.2`
- `REPUTATIONBAN_PAPER_START_SCRIPT`: start script。未設定時は `~/servers/paper-26.1.2/start.sh`
- `REPUTATIONBAN_SCREEN_NAME`: 既存 screen session を明示する場合に指定

## Player Report/Evidence Smoke

Phase 26 では、実プレイヤー2名以上が必要な player report/evidence runtime smoke を別 gate として記録します。
Phase 27 では、ユーザーが Phase 26 checklist 全項目 OK と報告した手動確認結果を `--manual-confirmed` で正式記録します。

```bash
./scripts/check-player-report-runtime-readiness.sh
./scripts/check-player-report-runtime-readiness.sh --strict
```

`/reportbad` は player sender 前提のため、自動スクリプトや review archive から勝手に実行しません。実施する場合は `docs/PLAYER_REPORT_RUNTIME_SMOKE_CHECKLIST.md` に従い、reporter と target の2名以上で `/reportbad <target> griefing runtime-smoke-test`、`/reports view <id>`、`/reports evidence <id>` を確認します。

実施後の記録:

```bash
./scripts/record-player-report-runtime-smoke-result.sh \
  --result PASS \
  --reporter TestReporter \
  --target TestTarget \
  --report-id 123 \
  --note "reportbad and reports evidence passed"
```

ユーザー手動確認済みで、公開用レビューに reporter、target、report id を残さない場合の記録:

```bash
./scripts/record-player-report-runtime-smoke-result.sh \
  --result PASS \
  --manual-confirmed \
  --note "User manually confirmed all Phase 26 player report runtime smoke checklist items passed."
```

未実施の場合は PASS と扱わず、`player report runtime smoke: NOT_RUN` と `judgment: HOLD_FOR_PLAYER_REPORT_RUNTIME_SMOKE` を表示します。`--strict` は PASS 以外を non-zero にします。

## Install

1. `./gradlew clean test build --warning-mode all` を実行します。
2. `build/libs/ReputationBan-0.27.0.jar` を Paper `plugins` directory へコピーします。
3. Java 25 で Paper を起動します。

## Startup

- `/plugins` に ReputationBan が表示されることを確認します。
- `plugins/ReputationBan/config.yml` が存在することを確認します。
- `plugins/ReputationBan/reputationban.db` が存在することを確認します。
- startup logs に Discord Webhook URL が出ていないことを確認します。

## Commands

- `/rep help`
- `/rep version`
- `/reports help`
- `/reportbad <TAB>` と category TAB completion
- `/rep audit recent`
- `/rep audit export recent`
- `/rep doctor`
- `/rep diagnostics`
- `/rep integrations`
- `/rep integrations test`
- `/rep placeholders`
- `/reports evidence <id>`
- `/rep backup before-runtime-smoke`
- `/rep support bundle`
- `/rep maintenance preview`
- `/rep maintenance run`
- `/rep maintenance run confirm`

## Safety Checks

- `/rep maintenance preview` が件数表示のみで data を削除しないことを確認します。
- `/rep maintenance run` が data を削除せず、`run confirm` を案内することを確認します。
- `/rep maintenance run confirm` が `plugins/ReputationBan/backups/reputationban-before-maintenance-*.db` を作成することを確認します。
- `/rep backup before-runtime-smoke` が `plugins/ReputationBan/backups/reputationban-manual-backup-*.db` を作成することを確認します。
- `/rep support bundle` が `plugins/ReputationBan/support/reputationban-support-*.zip` を作成することを確認します。
- support ZIP に `config-redacted.yml` と `README-SHARING.txt` が含まれ、DB files、server logs、Webhook URLs、共有不要な absolute paths が含まれないことを確認します。
- `/rep doctor` が database/table/config/Discord status と lightweight counts を表示することを確認します。
- `/rep integrations` が LuckPerms と CoreProtect の連携状態を表示することを確認します。未導入なら unavailable で問題ありません。
- `/rep integrations` が WorldGuard / WorldEdit の連携状態を表示することを確認します。未導入なら unavailable で問題ありません。
- `/rep integrations` が GriefPrevention の連携状態を表示することを確認します。未導入なら unavailable で問題ありません。
- `/rep integrations` が PlaceholderAPI の連携状態を表示することを確認します。未導入なら unavailable で問題ありません。
- `/rep integrations` が DiscordSRV の連携状態を表示することを確認します。未導入なら unavailable で問題ありません。
- `/rep integrations test` が外部連携だけの詳細診断を表示し、CoreProtect 実 lookup をデフォルトでは実行しないことを確認します。
- WorldGuard + WorldEdit 導入時は `/rep integrations test` の player 実行で `currentRegions` と `regionCount` が表示されることを確認します。
- GriefPrevention 導入時は `/rep integrations test` の player 実行で `currentClaimPresent`、`adminClaim`、`claimId` が表示されることを確認します。
- `/reports evidence <id>` が保存済み `report_context` または「この通報に保存された連携情報はありません。」を表示することを確認します。
- WorldGuard region 内の `/reportbad griefing` 後、`/reports evidence <id>` に WorldGuard context が出ることを確認します。region/flag は変更しません。
- GriefPrevention claim 内と claim 外の `/reportbad griefing` 後、`/reports evidence <id>` に GriefPrevention context が出ることを確認します。claim/trust は変更しません。
- PlaceholderAPI 導入時は `/rep placeholders` と `/papi parse <player> %reputationban_score%` を確認し、score 変更後に `cache-refresh-seconds` 以内で反映されることを確認します。
- DiscordSRV 導入時は account link 済み/未リンクの player で `/reports evidence <id>` に context が表示されることを確認します。DiscordSRV 通知はデフォルト無効で、Discord から Minecraft コマンドを実行しません。
- DiscordSRV 通知や account link を実利用する場合は、bot token 設定済み環境で `apiAvailable=true` と `active=true` を追加確認します。
- optional plugin 組み合わせの integration smoke は `./scripts/run-integration-runtime-smoke-helper.sh` と `docs/INTEGRATION_RUNTIME_SMOKE_CHECKLIST.md` に沿って確認します。
- Discord webhook がデフォルトで無効であることを確認します。
- Webhook URLs が logs、`/rep doctor`、audit output、CSV output、review archive files に出ていないことを確認します。
- BAN と pardon commands は disposable test users のみに実行します。
- 実行後の結果は次のように記録します。

```bash
./scripts/record-paper-runtime-smoke-result.sh --result PASS --note "Paper runtime smoke passed"
./scripts/record-integration-runtime-smoke-result.sh --result PASS --scenario "All integrations" --note "manual smoke passed"
./scripts/record-player-report-runtime-smoke-result.sh --result PASS --reporter TestReporter --target TestTarget --report-id 123 --note "reportbad and reports evidence passed"
```

- 失敗した場合は `--result FAIL` と原因の note を記録します。
- 未実施を PASS 扱いにしないでください。Paper runtime smoke 未実施の場合、`./scripts/check-paper-runtime-readiness.sh` は `HOLD_FOR_PAPER_RUNTIME_SMOKE` を表示し、review archive では `status=NOT_RUN` として扱います。Integration runtime smoke 未実施の場合、`./scripts/check-integration-runtime-readiness.sh` は `HOLD_FOR_INTEGRATION_RUNTIME_SMOKE` を表示します。Player report runtime smoke 未実施の場合、`./scripts/check-player-report-runtime-readiness.sh` は `HOLD_FOR_PLAYER_REPORT_RUNTIME_SMOKE` を表示します。
- review archive では runtime smoke 実行後に readiness を判定し、`checks/runtime-smoke-consistency.txt` で summary/readiness の整合性を確認します。

## Rollback

- server を停止します。
- 現在の `plugins/ReputationBan/reputationban.db` を退避します。
- 最新の `plugins/ReputationBan/backups/reputationban-before-maintenance-*.db` を復元します。
- server を起動し、`/rep audit recent` を再確認します。
