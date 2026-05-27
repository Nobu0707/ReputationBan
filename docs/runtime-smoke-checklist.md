# Runtime Smoke Checklist

## Environment

- PaperMC 26.1.2
- Java 25
- Fresh または disposable test server
- Discord webhook がデフォルト無効の ReputationBan config

## Install

1. `./gradlew clean test build --warning-mode all` を実行します。
2. `build/libs/ReputationBan-0.20.0.jar` を Paper `plugins` directory へコピーします。
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
- `/rep integrations test` が外部連携だけの詳細診断を表示し、CoreProtect 実 lookup をデフォルトでは実行しないことを確認します。
- WorldGuard + WorldEdit 導入時は `/rep integrations test` の player 実行で `currentRegions` と `regionCount` が表示されることを確認します。
- GriefPrevention 導入時は `/rep integrations test` の player 実行で `currentClaimPresent`、`adminClaim`、`claimId` が表示されることを確認します。
- `/reports evidence <id>` が保存済み `report_context` または「この通報に保存された連携情報はありません。」を表示することを確認します。
- WorldGuard region 内の `/reportbad griefing` 後、`/reports evidence <id>` に WorldGuard context が出ることを確認します。region/flag は変更しません。
- GriefPrevention claim 内と claim 外の `/reportbad griefing` 後、`/reports evidence <id>` に GriefPrevention context が出ることを確認します。claim/trust は変更しません。
- PlaceholderAPI 導入時は `/rep placeholders` と `/papi parse <player> %reputationban_score%` を確認し、score 変更後に `cache-refresh-seconds` 以内で反映されることを確認します。
- Discord webhook がデフォルトで無効であることを確認します。
- Webhook URLs が logs、`/rep doctor`、audit output、CSV output、review archive files に出ていないことを確認します。
- BAN と pardon commands は disposable test users のみに実行します。
- 実行後の結果は次のように記録します。

```bash
./scripts/record-paper-runtime-smoke-result.sh --result PASS --note "Paper runtime smoke passed"
```

- 失敗した場合は `--result FAIL` と原因の note を記録します。
- 未実施を PASS 扱いにしないでください。未実施の場合、review archive では `status=NOT_RUN` として扱います。

## Rollback

- server を停止します。
- 現在の `plugins/ReputationBan/reputationban.db` を退避します。
- 最新の `plugins/ReputationBan/backups/reputationban-before-maintenance-*.db` を復元します。
- server を起動し、`/rep audit recent` を再確認します。
