# v0.28.0 リリース候補チェックリスト

v0.28.0 の v1.0.0 release candidate final checklist として次を確認します。

## Build / Test / Review

- `./gradlew clean test build --warning-mode all` が成功します。
- `./scripts/review_code.sh` が成功します。
- `./scripts/check-optional-dependency-safety.sh` が成功します。
- `./scripts/run-paper-runtime-smoke.sh` が成功します。環境がない場合は `NOT_RUN` で記録されます。
- `./scripts/check-paper-runtime-readiness.sh` が成功し、未実施なら HOLD/NOT_RUN を表示します。
- `./scripts/run-integration-runtime-smoke.sh` が成功します。環境がない場合は `NOT_RUN` で記録されます。
- `./scripts/check-integration-runtime-readiness.sh` が成功し、未実施なら HOLD/NOT_RUN を表示します。
- `./scripts/check-player-report-runtime-readiness.sh` が成功し、未実施なら HOLD/NOT_RUN を表示します。
- `./scripts/check-runtime-smoke-consistency.sh` が成功します。
- `./scripts/check-v1-release-gates.sh` が成功します。
- `./scripts/generate-v1-go-no-go-report.sh` が成功します。
- `./scripts/generate-v1-release-notes-draft.sh` が成功します。
- `./scripts/run-local-smoke-check.sh` が成功します。
- JAR 名が `ReputationBan-0.28.0.jar` です。
- Paper runtime smoke、Integration runtime smoke、Player report/evidence runtime smoke は PASS 済みです。
- Player report/evidence runtime smoke は `manualConfirmed=true` で Phase 26 のユーザー手動確認結果を記録済みです。
- v1 release gates の既定 judgment は `READY_FOR_V1_RELEASE_REVIEW_WITH_DISCORDSRV_WARNING` です。

## Docs Localization

- `./scripts/check-docs-localization.sh` が成功します。
- README.md と主要 docs に日本語説明があります。
- `/rep version`、`/reportbad`、`reputationban.admin`、`initial-score`、`notify.discord-webhook`、`retention` などの識別子が翻訳で壊れていません。

## Release Artifact Verification

- `./scripts/create-release-artifact.sh` が成功します。
- `./scripts/verify-release-artifact.sh` が成功します。
- release ZIP に README.md、CHANGELOG.md、主要 docs、JAR、checksum が含まれます。
- release ZIP に `docs/INTEGRATIONS.md` が含まれます。
- release ZIP に `docs/INTEGRATION_RUNTIME_SMOKE_CHECKLIST.md` が含まれます。
- release ZIP に `docs/PLAYER_REPORT_RUNTIME_SMOKE_CHECKLIST.md` が含まれます。
- release ZIP に `docs/V1_RELEASE_PLAN.md` が含まれます。
- Go/No-Go report と release notes draft は review archive の `release-prep/` に収集します。
- release ZIP に live `config.yml`、`reputationban.db`、WAL/SHM、logs は含まれません。

## Secret Scan

- docs と release ZIP に実 Discord Webhook URL 風の文字列がありません。
- Webhook URL values は logs、doctor output、audit metadata、CSV、support bundle、review archive に出ません。

## Support Bundle Safety

- `/rep support bundle` は `config-redacted.yml` を含みます。
- support bundle に DB files、server logs、live `config.yml`、Webhook URL が含まれません。
- 共有前に `<redacted>` の状態を確認します。

## Paper Runtime Smoke

- 可能な限り実 Paper 26.1.2 サーバーと Java 25 で確認します。
- 既定の Paper server directory は `~/servers/paper-26.1.2/`、既定の start script は `~/servers/paper-26.1.2/start.sh` です。
- `start.sh` は `screen` で Paper server を起動する前提です。
- `REPUTATIONBAN_PAPER_DIR`、`REPUTATIONBAN_PAPER_START_SCRIPT`、`REPUTATIONBAN_SCREEN_NAME`、`REPUTATIONBAN_SMOKE_STOP_SERVER` を必要に応じて指定します。
- 未実施は PASS ではありません。`./scripts/check-paper-runtime-readiness.sh --strict` は PASS 以外で non-zero になります。
- Paper runtime smoke readiness と latest summary に矛盾がないことを確認します。
- `/rep version`、`/rep doctor`、`/rep integrations`、`/rep integrations test`、`/rep placeholders`、`/rep support bundle`、`/rep backup`、`/reportbad` TAB 補完、`/reports evidence <id>` を確認します。

## Integrations

- 自動 integration runtime smoke は `~/servers/PaperPlugins/*.jar` を Paper `plugins/` に staging し、対象 JAR の backup/restore を記録します。
- 既定の integration plugin directory は `~/servers/PaperPlugins/` です。必要に応じて `REPUTATIONBAN_INTEGRATION_PLUGIN_DIR` を指定します。
- `REPUTATIONBAN_INTEGRATION_RESTORE_PLUGINS=1` が既定で、既存対象 JAR を復元します。
- LuckPerms / CoreProtect 未導入でも plugin が起動します。
- WorldEdit / WorldGuard 未導入、WorldEdit のみ導入、WorldEdit + WorldGuard 導入の各構成で plugin が起動します。
- GriefPrevention 未導入、導入の各構成で plugin が起動します。
- PlaceholderAPI 未導入、導入の各構成で plugin が起動します。
- DiscordSRV 未導入、導入の各構成で plugin が起動します。
- LuckPerms だけ、CoreProtect だけが導入されている場合でも plugin が起動します。
- Java ソース内に `import net.luckperms.`、`import net.coreprotect.`、`import com.sk89q.worldguard.`、`import com.sk89q.worldedit.`、`import me.ryanhamshire.GriefPrevention.`、`import me.ryanhamshire.griefprevention.`、`import com.griefprevention.`、`import github.scarsz.discordsrv.`、`import me.scarsz.discordsrv.`、`import net.dv8tion.jda.` が残っていないことを確認します。`import me.clip.placeholderapi.` は `ReputationBanPlaceholderExpansion.java` のみに限定します。
- Phase 16a 以降の reflection adapter と Phase 17 の safety script により optional dependency class loading が安全化されています。
- LuckPerms のオフラインユーザー情報が未ロードの場合、`default-weight` 扱いになる場合があります。
- LuckPerms 導入時は reporter weight と bypass-groups が期待通りに扱われます。
- CoreProtect 導入時は griefing report の周辺ログサマリーが `report_context` に保存されます。
- WorldGuard 導入時は対象 category の report で region context が `report_context` provider `worldguard` に保存されます。
- GriefPrevention 導入時は対象 category の report で claim context が `report_context` provider `griefprevention` に保存されます。
- PlaceholderAPI 導入時は `%reputationban_score%` などが cache 由来の値を返します。
- DiscordSRV 導入時は account link context が `report_context` provider `discordsrv` に保存され、Discord ID は既定で hidden になります。
- Integration runtime smoke readiness と latest summary に矛盾がないことを確認します。
- `integration-status.txt` に active / unavailable integration が記録されていることを確認します。
- DiscordSRV unavailable が残る場合は、本番で DiscordSRV 連携を使うかどうか判断します。通知や account link を使う場合は bot token 設定済み環境で追加テストします。
- DiscordSRV optional warning は通常の v1 release gate を止めません。DiscordSRV を本番必須にする場合だけ `--require-discordsrv` で HOLD 判定にします。
- `max-results: 0` では個別行を保存せず、件数と lookup 条件だけを保存します。
- CoreProtect rollback、restore、purge と LuckPerms 書き込み操作は実行しません。
- CoreProtect rollback、restore、purge、LuckPerms 書き込み操作、WorldGuard region/flag 変更、GriefPrevention claim/trust 変更は実行しません。
- Integration runtime smoke を実施した場合は `./scripts/run-integration-runtime-smoke.sh` の summary、または `./scripts/record-integration-runtime-smoke-result.sh --result PASS --scenario "All integrations" --note "manual smoke passed"` で記録します。
- `/reportbad` と `/reports evidence` による report_context 実生成確認は実プレイヤー2名以上で手動確認します。
- 実施後は `./scripts/record-paper-runtime-smoke-result.sh --result PASS --note "Paper runtime smoke passed"` で記録します。
- 未実施を PASS 扱いにしません。未実施の場合は readiness check が `HOLD_FOR_INTEGRATION_RUNTIME_SMOKE` を表示し、review archive の summary が `status=NOT_RUN` になります。
- v1.0.0 直前 gate では `./scripts/check-paper-runtime-readiness.sh --strict` と `./scripts/check-integration-runtime-readiness.sh --strict` が成功する状態にしてください。

## Player report/evidence runtime smoke

- 実プレイヤー2名以上を reporter / target として使います。
- `/reportbad <target> griefing runtime-smoke-test` を reporter で実行します。
- `/reports list all 10` で report id を確認します。
- `/reports view <id>` が表示されることを確認します。
- `/reports evidence <id>` が表示されることを確認します。
- `report_context` が少なくとも1 provider分表示される、または「連携情報なし」が明示されることを確認します。
- LuckPerms / CoreProtect / WorldGuard / GriefPrevention / DiscordSRV の未導入や未設定で ReputationBan 本体が落ちないことを確認します。
- 必要に応じて `/rep pardon <target> runtime-smoke-cleanup` で cleanup します。
- support bundle に DB files、server logs、secret が含まれないことを確認します。
- Phase 26 の手動確認結果は PASS 済みとして、`./scripts/record-player-report-runtime-smoke-result.sh --result PASS --manual-confirmed --note "User manually confirmed all Phase 26 player report runtime smoke checklist items passed."` で記録します。
- 公開用レビューには reporter、target、report id を残さず、`manual-checklist.txt` に確認項目の要約を保存します。
- v1.0.0 直前 gate では `./scripts/check-player-report-runtime-readiness.sh --strict` が成功する状態にしてください。

## Backup / Restore 注意

- `/rep maintenance run confirm` 前に backup が作成されることを確認します。
- `/rep backup before-release` の生成物を確認します。
- rollback 手順では server を止め、現在の DB を退避してから backup を復元します。

## v1.0.0 へ進む前の判断

- build/test/review_code/local smoke/release verification がすべて成功しています。
- Paper runtime smoke: PASS です。
- Integration runtime smoke: PASS です。
- Player report/evidence runtime smoke: PASS です。
- Runtime smoke consistency: PASS です。
- Optional dependency safety、docs localization、release artifact verification、secret scan が PASS です。
- Go/No-Go report と release notes draft が生成済みです。
- DiscordSRV unavailable は WARN として明記済みです。
- 既知の未解決事項が v1.0.0 を妨げないことを確認します。
