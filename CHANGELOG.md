# Changelog

## 1.0.0

- Phase 36 で `docs/MAINTENANCE_BASELINE.md`、`docs/ISSUE_TRIAGE_GUIDE.md`、`docs/phase-36.md`、`scripts/check-maintenance-baseline.sh` を追加し、v1.0.0 公開後の maintenance baseline と issue/PR intake dry-run を記録しました。
- Phase 36 baseline では open issues は none、open PRs は none、confirmed bug candidates は none、v1.0.1 candidates は none selected、DiscordSRV configured smoke は `NOT_RUN` / `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE` の operational verification candidate です。`v1.0.0` tag、GitHub Release、Release assets、version は変更していません。
- Phase 35 で GitHub issue templates、PR template、`SECURITY.md`、`SUPPORT.md`、`CONTRIBUTING.md`、`docs/phase-35.md` を追加し、v1.0.0 公開後の support / bug report / integration issue / v1.0.1 intake の導線を整えました。
- v1.0.1 candidates を整理し、confirmed bug candidates は none、DiscordSRV token-configured runtime smoke は `NOT_RUN` / `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE` の運用確認候補、Phase 35 docs/support improvements は completed、feature requests は v1.1.0 以降候補として記録しました。`v1.0.0` tag、GitHub Release、Release assets、version は変更していません。
- Phase 34 で DiscordSRV token-configured runtime smoke の実施可否を再判断し、token-configured environment または production-use decision が提供されていないため `NOT_RUN` / `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE` として記録しました。未実施は PASS ではなく、v1.0.1 bugfix ではなく運用確認候補です。
- `docs/phase-34.md` を追加し、post-release monitoring、bugfix intake、v1.0.1 candidates、release readiness、README に Phase 34 の判断を反映しました。`v1.0.0` tag、GitHub Release、Release assets、version は変更していません。
- Phase 33 で DiscordSRV token-configured runtime smoke checklist、結果記録スクリプト、readiness check、review archive 収集を追加しました。未設定環境は `NOT_RUN` / `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE` として扱い、PASS にはしません。
- `scripts/check-v1-release-gates.sh` が `discordSrvConfiguredSmoke` を表示し、未実施時は `RELEASED_WITH_DISCORDSRV_WARNING`、PASS 時は `RELEASED` として post-release 状態を示すようにしました。`v1.0.0` tag と GitHub Release assets は変更していません。
- Post-release documentation updates after v1.0.0 publication.
- Phase 32 で `docs/POST_RELEASE_MONITORING.md`、`docs/BUGFIX_INTAKE.md`、`docs/V1_0_1_CANDIDATES.md`、`docs/phase-32.md` を追加し、公開後監視、bugfix intake、v1.0.1 candidates の扱いを記録しました。
- Phase 31a で公開済み GitHub Release 本文と生成レポートの整合性を修正しました。`GitHub Release status: PUBLISHED`、Release URL、`Tag status: CREATED`、`Next action: Post-release monitoring / bugfix intake` を release notes / Go-No-Go report に反映しています。
- Go/No-Go report の公開後判定は `RELEASED_WITH_DISCORDSRV_WARNING` です。DiscordSRV は引き続き WARN 扱いで、tag と Release assets は変更していません。
- Phase 31 で GitHub Release `v1.0.0` を公開しました。Release URL は <https://github.com/Nobu0707/ReputationBan/releases/tag/v1.0.0> です。
- 公開後の GitHub Release は `isDraft=false`、`isPrerelease=false` で、asset は `ReputationBan-1.0.0.jar`、`ReputationBan-1.0.0.jar.sha256`、`ReputationBan-1.0.0-release.zip`、`ReputationBan-1.0.0-release.zip.sha256` の4件です。
- `v1.0.0` annotated tag は Phase 30 commit `b422e72ec5a917cdc04dee902e96a0cef190026c` を指しています。Phase 31 の docs-only commit により main は tag より進みますが、tag は移動していません。
- `docs/phase-31.md` を追加し、公開前後の release 状態、asset、SHA256、runtime gate、DiscordSRV WARN、公開日時を記録しました。
- Phase 30 は v1.0.0 tag / GitHub Release draft preparation フェーズです。`v1.0.0` annotated tag を Phase 30 commit に作成し、GitHub Release は draft として作成します。
- GitHub Release draft には `ReputationBan-1.0.0.jar`、JAR sha256、`ReputationBan-1.0.0-release.zip`、release zip sha256 を添付します。
- Phase 30 時点では GitHub Release 公開、`draft=false` への変更、v1.0.1 以降への version bump は行いませんでした。
- `docs/phase-30.md` を追加し、tag/draft 作成方針、添付 artifact、runtime gate、DiscordSRV WARN、次Phaseの公開手順を記録しました。
- `scripts/make-review-archive.sh` が tag 状態、GitHub Release draft 状態、draft asset 一覧を review archive に含めるようにしました。
- Phase 29 は first stable release candidate final artifact preparation フェーズです。version を 1.0.0 に更新し、`ReputationBan-1.0.0.jar` と `ReputationBan-1.0.0-release.zip` を配布候補として準備します。
- Paper runtime smoke、Integration runtime smoke、Player report/evidence runtime smoke、Runtime smoke consistency の release gates は PASS として再確認します。
- LuckPerms、CoreProtect、WorldGuard、GriefPrevention、PlaceholderAPI、DiscordSRV の任意連携を v1.0.0 の外部連携対象として記録します。
- DiscordSRV は bot token 未設定時 WARN 扱いです。DiscordSRV 通知はデフォルト無効であり、ReputationBan 本体や他連携の release gate は止めません。
- `scripts/generate-v1-release-notes.sh` と `docs/V1_RELEASE_EXECUTION_PLAN.md` を追加し、Go/No-Go report と release notes final candidate をレビューアーカイブに含められるようにしました。
- Phase 29 では `v1.0.0` tag 作成と GitHub Release 公開は行っていません。

## 0.28.0

- Phase 28 は v1.0.0 release candidate readiness review フェーズです。新機能追加ではなく、v1.0.0 公開前の最終 Go/No-Go 判定、release gates、release notes draft を整備しました。
- `scripts/check-v1-release-gates.sh` を追加し、Paper runtime smoke、Integration runtime smoke、Player report/evidence runtime smoke、runtime smoke consistency、optional dependency safety、docs localization、release artifact verification、secret scan、破壊的連携操作なしをまとめて判定できるようにしました。
- `scripts/generate-v1-go-no-go-report.sh` と `scripts/generate-v1-release-notes-draft.sh` を追加し、`build/release/` に v1.0.0 Go/No-Go report と GitHub Release向け下書きを生成できるようにしました。
- `docs/phase-28.md` と `docs/V1_RELEASE_PLAN.md` を追加し、v1.0.0 へ進む条件、タグ作成方針、GitHub Release公開方針、Rollback方針、DiscordSRV WARN扱いを明文化しました。
- release artifact と review archive が v1準備資料を収集できるようにし、`make-review-archive.sh` に v1 release gates、Go/No-Go report、release notes draft の出力を追加しました。
- DiscordSRV は bot token 未設定時 WARN として扱います。DiscordSRV通知はデフォルト無効であり、ReputationBan 本体や他連携の release gate は止めません。本番で DiscordSRV 通知や account link を使う場合は、bot token 設定済み環境で追加確認が必要です。
- v1.0.0 への version bump、v1.0.0 tag 作成、GitHub Release 公開は行っていません。

## 0.27.0

- Phase 27 は player report/evidence runtime smoke result recording / release gate close フェーズです。新機能追加ではなく、Phase 26 でユーザーが手動確認済みと報告した player report/evidence runtime smoke PASS を正式な記録として保存しました。
- `scripts/record-player-report-runtime-smoke-result.sh` に `--manual-confirmed` を追加し、公開用レビューに reporter、target、report id を残さず `reporter=<manual-confirmed>`、`target=<manual-confirmed>`、`reportId=<manual-confirmed>`、`manualConfirmed=true` を記録できるようにしました。
- manual confirmed PASS では `manual-checklist.txt` を `build/manual-smoke/player-report-runtime-*` に保存し、`latest-player-report-runtime-smoke-summary.txt`、`player-report-runtime-readiness.txt`、`runtime-smoke-consistency.txt` が PASS/READY/PASS で揃うようにしました。
- Release readiness と release candidate checklist に Paper runtime smoke PASS、Integration runtime smoke PASS、Player report/evidence runtime smoke PASS 済みを記録しました。DiscordSRV は bot token 未設定時 WARN のため、本番で通知や account link を使う場合は追加確認が必要です。
- 新しい外部連携、Discord からの Minecraft コマンド実行、Discord role 変更、GUI、Folia 対応、v1.0.0 リリース化は追加していません。

## 0.26.0

- Phase 26 は player report/evidence runtime smoke gate フェーズです。新しい外部連携ではなく、実プレイヤー2名以上が必要な `/reportbad`、`/reports view`、`/reports evidence`、`report_context` 表示確認を記録しやすくしました。
- `docs/PLAYER_REPORT_RUNTIME_SMOKE_CHECKLIST.md` と `docs/phase-26.md` を追加し、未実施を PASS 扱いにしない方針、PASS/FAIL 条件、cleanup 手順を明記しました。
- `scripts/record-player-report-runtime-smoke-result.sh` を追加し、PASS/FAIL/NOT_RUN summary を `build/manual-smoke/player-report-runtime-*` に保存できるようにしました。PASS は reporter、target、report id を必須にしています。
- `scripts/check-player-report-runtime-readiness.sh` を追加し、通常モードでは NOT_RUN/HOLD を exit 0、`--strict` では PASS 以外を non-zero とする gate を用意しました。
- `scripts/check-runtime-smoke-consistency.sh` と `scripts/make-review-archive.sh` が Player report runtime smoke の readiness、latest summary、review archive 収集を扱うようになりました。
- Release readiness と release candidate checklist に、v1.0.0 前の推奨ゲートとして Paper runtime smoke PASS、Integration runtime smoke PASS、Player report/evidence runtime smoke PASS を明記しました。
- 手動プレイヤー操作なしに `/reportbad` を自動実行する処理、Discord から Minecraft コマンドを実行する機能、Discord role 変更、GUI、Folia 対応、v1.0.0 リリース化は追加していません。

## 0.25.0

- Phase 25 は runtime smoke readiness consistency / release gate 整合フェーズです。新機能追加ではなく、Paper runtime smoke / integration runtime smoke と readiness 判定の矛盾を防ぐための release automation を更新しました。
- `scripts/make-review-archive.sh` の実行順を修正し、`run-paper-runtime-smoke.sh` の後に `check-paper-runtime-readiness.sh`、`run-integration-runtime-smoke.sh` の後に `check-integration-runtime-readiness.sh` が走るようにしました。
- `scripts/check-runtime-smoke-consistency.sh` を追加し、latest summary が PASS なのに readiness が HOLD/NOT_RUN になる不整合、または NOT_RUN なのに readiness が READY になる不整合を non-zero で検出します。
- `scripts/run-integration-runtime-smoke.sh` が `/rep integrations` のログから `integration-status.txt` を生成し、summary に `activeIntegrations` / `unavailableIntegrations` / `discordSrvUnavailableReason` を残すようにしました。
- DiscordSRV が bot token 未設定などで `apiAvailable=false` になるケースは、ReputationBan 本体と他連携の runtime smoke PASS を妨げない WARN として記録します。本番で DiscordSRV 通知や account link を使う場合は、bot token 設定済み環境で追加確認が必要です。
- review archive に `checks/runtime-smoke-consistency.txt` と `runtime-smoke/integration-runtime-latest/integration-status.txt` を収集するようにしました。
- Phase 25 docs、runtime smoke checklist、release readiness、release artifact checks を v0.25.0 向けに更新しました。
- 新しい外部連携、Discord からの Minecraft コマンド実行、Discord role 変更、GUI、Folia 対応、v1.0.0 リリース化は追加していません。

## 0.24.0

- `scripts/run-integration-runtime-smoke.sh` を追加し、`~/servers/PaperPlugins/*.jar` と `build/libs/ReputationBan-0.24.0.jar` を Paper test server に staging して、`/rep integrations`、`/rep integrations test`、`/rep doctor` を自動実行できるようにしました。
- 既存の ReputationBan / LuckPerms / CoreProtect / WorldEdit / WorldGuard / GriefPrevention / PlaceholderAPI / DiscordSRV JAR を `plugins/backups/reputationban-integration-smoke-*` に退避し、既定では外部連携 JAR を削除して既存 JAR を復元する方針を追加しました。
- Integration smoke 環境が見つからない場合は `build/manual-smoke/integration-runtime-*` に `status=NOT_RUN` / `result=NOT_RUN` を記録し、PASS 扱いにしません。
- `scripts/check-integration-runtime-readiness.sh` を v0.24.0 向けに更新し、通常モードでは HOLD、`--strict` では PASS 以外を non-zero とします。
- review archive に `checks/integration-runtime-smoke-auto.txt` と `runtime-smoke/integration-runtime-latest/`、`runtime-smoke/paper-runtime-latest/` を収集するようにしました。
- Phase 24 docs、runtime smoke checklist、release readiness、release artifact checks を v0.24.0 向けに更新しました。
- 新しい外部連携、Discord からの Minecraft コマンド実行、Discord role 変更、GUI、Folia 対応、v1.0.0 リリース化は追加していません。

## 0.23.0

- `scripts/run-paper-runtime-smoke.sh` を追加し、`~/servers/paper-26.1.2/start.sh` が `screen` で起動する Paper test server に対して JAR 配置、screen session 特定、console command 投入、ログ検査、必要時の stop まで自動化しました。
- Paper 実機環境が見つからない場合は `build/manual-smoke/paper-runtime-*` に `status=NOT_RUN` / `result=NOT_RUN` を記録し、PASS 扱いにしない方針を明確化しました。
- `scripts/check-paper-runtime-readiness.sh` を追加し、通常モードでは HOLD、`--strict` では PASS 以外を non-zero とする Paper runtime gate を用意しました。
- review archive に `checks/paper-runtime-smoke-auto.txt` と `checks/paper-runtime-readiness.txt` を収集し、最新 Paper runtime smoke summary を `latest-paper-runtime-smoke-summary.txt` として残します。
- Phase 23 docs、runtime smoke checklist、release readiness、release artifact checks を v0.23.0 向けに更新しました。
- 新しい外部連携、Discord からの Minecraft コマンド実行、Discord role 変更、GUI、Folia 対応、v1.0.0 リリース化は追加していません。

## 0.22.0

- DiscordSRV reflection adapter を Bukkit PluginManager 由来の plugin instance route 優先にし、`Class.forName` route は fallback として残しました。
- DiscordSRV 通知の `integration.detail(config)`、message sanitize、sendMessage を main thread task 内で実行するようにしました。
- `scripts/check-integration-runtime-readiness.sh` を追加し、未実施の integration runtime smoke を `HOLD_FOR_INTEGRATION_RUNTIME_SMOKE` として明示します。
- `scripts/run-integration-runtime-smoke-helper.sh` を追加し、optional plugin 構成ごとの実機確認手順を出せるようにしました。
- Phase 22 docs、runtime smoke checklist、review archive、release artifact checks を v0.22.0 向けに更新しました。
- 新しい外部連携、Discord からの Minecraft コマンド実行、Discord role 変更、LuckPerms/CoreProtect/WorldGuard/GriefPrevention の書き込み操作は追加していません。

## 0.21.0

- DiscordSRV を `softdepend` の任意連携として追加しました。
- DiscordSRV/JDA API 型の直接 import を避け、`DiscordSrvReflectionAdapter` の reflection に閉じ込めました。
- `/rep integrations`、`/rep integrations test`、`/rep doctor` に DiscordSRV 状態を追加しました。
- `/reportbad` 後に対象カテゴリの DiscordSRV account link context を `report_context` provider `discordsrv` として保存するようにしました。
- `/reports view <id>` と `/reports evidence <id>` で DiscordSRV 文脈を表示できるようにしました。
- `DISCORDSRV_CONTEXT_CAPTURED` audit event を追加しました。
- DiscordSRV 通知を追加しました。既存 Webhook との二重通知を避けるためデフォルトは無効です。
- Discord から Minecraft コマンドを実行する機能、Discord role 変更、自動BANの唯一根拠化は行いません。

## 0.20.0

- PlaceholderAPI を `softdepend` + `compileOnly` の任意連携として追加しました。
- PAPI API の直接 import は `ReputationBanPlaceholderExpansion` に隔離し、通常ロードされるクラスは reflection で登録します。
- `%reputationban_score%`、`%reputationban_status%`、`%reputationban_version%` などの placeholder を追加しました。
- `/rep placeholders`、`/rep integrations`、`/rep integrations test`、`/rep doctor` に PlaceholderAPI 状態を追加しました。
- placeholder 呼び出し時にDB同期問い合わせを行わないよう、online player summary cache を追加しました。
- Phase 20 の docs、review checks、release artifact scripts を v0.20.0 向けに更新しました。

## 0.19.0

- GriefPrevention を `softdepend` の任意連携として追加しました。
- GriefPrevention 連携は reflection adapter に閉じ込め、外部 API 型の直接 import を避けました。
- `/rep integrations`、`/rep integrations test`、`/rep doctor` に GriefPrevention 状態を追加しました。
- `/reportbad` 後に対象カテゴリの GriefPrevention claim context を `report_context` provider `griefprevention` として保存するようにしました。
- `/reports view <id>` と `/reports evidence <id>` で GriefPrevention 文脈を表示できるようにしました。
- `GRIEFPREVENTION_CONTEXT_CAPTURED` audit event を追加しました。
- GriefPrevention claim/trust の作成、変更、削除、自動処罰の根拠化は行いません。

## 0.18.0

- WorldGuard / WorldEdit を `softdepend` と `compileOnly` の任意連携として追加しました。
- WorldGuard 連携は reflection adapter に閉じ込め、`com.sk89q.*` の直接 import を避けました。
- `/rep integrations`、`/rep integrations test`、`/rep doctor` に WorldGuard 状態を追加しました。
- `/reportbad` 後に対象カテゴリの WorldGuard region context を `report_context` provider `worldguard` として保存するようにしました。
- `/reports view <id>` と `/reports evidence <id>` で WorldGuard 文脈を表示できるようにしました。
- `WORLDGUARD_CONTEXT_CAPTURED` audit event を追加しました。
- WorldGuard region/flag の作成、変更、削除、自動処罰の根拠化は行いません。

## 0.17.0

- `/reports evidence <id>` を追加し、report に保存された LuckPerms / CoreProtect の `report_context` を詳細表示できるようにしました。
- `/rep integrations test` を追加し、外部連携だけに絞った安全な診断を実行できるようにしました。
- `/rep integrations` の表示を、configuredEnabled、pluginPresent、apiAvailable、active、LuckPerms weight 設定、CoreProtect lookup 設定まで確認できる内容に拡張しました。
- LuckPerms metadata に `primaryGroup`、`reporterWeight`、`bypassGroup`、`applyWeightToDeduction` を保存し、説明責任を強化しました。
- CoreProtect metadata に `resultCount`、`lookupSeconds`、`radius`、`category`、`world`、`x`、`y`、`z`、`apiVersion` を保存するようにしました。
- CoreProtect summary を日本語運用向けに改善し、`max-results: 0` では個別行を保存せず resultCount のみ保存する方針を明確化しました。
- `scripts/check-optional-dependency-safety.sh` を追加し、review archive と `review_code.sh` に統合しました。
- `docs/INTEGRATION_RUNTIME_SMOKE_CHECKLIST.md` と `scripts/record-integration-runtime-smoke-result.sh` を追加しました。
- CoreProtect rollback、restore、purge と LuckPerms 書き込み操作は引き続き行いません。

## 0.15.0

- docs localization check script を追加しました。
- release candidate checklist を追加しました。
- Paper runtime smoke status reporting を改善しました。
- localized docs 向けの release artifact verification を強化しました。

## 0.14.0

- README.md を日本語トップページとして再整備しました。
- docs/*.md を日本語化し、command names、permission nodes、config keys、file names は維持しました。
- `docs/phase-14.md` を追加しました。
- release artifact と review archive の対象を `ReputationBan-0.14.0.jar` に更新しました。
- `scripts/review_code.sh` に日本語ドキュメント確認と v0.14.0 artifact 確認を追加しました。
- Discord Webhook URL を docs、support bundle、review archive に含めない方針を日本語で明確化しました。

## 0.13.0

- token/password/secret/session/cookie/webhook/url-like values の free-text redaction を強化しました。
- support bundle の `meta.txt` と `doctor.txt` で absolute path exposure を減らしました。
- release artifact verification と release ZIP SHA256 generation を追加しました。
- Paper runtime smoke result recording と report template を追加しました。
- security/redaction documentation と review archive collection を拡充しました。

## 0.12.0

- `/rep backup [reason]` を追加し、`DB_BACKUP_CREATED` audit events 付きの manual SQLite backups を作成できるようにしました。
- `/rep support bundle` を追加し、DB files と server logs を除外した secret-redacted diagnostic ZIPs を作成できるようにしました。
- webhook URLs、URL-like values、passwords、tokens、secrets、sessions、cookies 向けの config redaction utilities を追加しました。
- `scripts/create-release-artifact.sh` を追加し、`build/release` JAR、SHA256、release ZIP artifacts を作成できるようにしました。
- review archive、local smoke、runtime smoke、release readiness、support bundle documentation を Phase 12 向けに更新しました。

## 0.11.0

- installation、configuration、migration、v1.0.0 readiness 向けの release preparation と operation documents を追加しました。
- `/rep version` と TAB completion を追加しました。
- safe Paper runtime smoke helper script を追加しました。
- `/rep doctor` に plugin data folder、database file、audit export、Discord webhook state、backup directory checks を追加し、Webhook URL は出さないようにしました。
- review archive generation と local smoke checks の重複 build を減らしました。

## 0.10.0

- `/rep doctor` と `/rep diagnostics` を追加しました。
- safe booleans と counts のみを含む `DIAGNOSTICS_RUN` audit metadata を追加しました。
- `bans.unbanned_by` durable actor ID と `bans.unbanned_by_name` display name を分離しました。
- review archives に local smoke output を収集するようにしました。

## 0.9.0

- config validation と safe audit export path handling を追加しました。
- maintenance preview と confirmed cleanup flow を追加しました。
- retention cleanup 前の SQLite backup を追加しました。
- runtime smoke checklist と review archive secret scan を追加しました。

## 0.8.0

- `audit_events`、audit commands、CSV export、retention policy を追加しました。
- moderation、score、ban、recovery、reload、maintenance の audit events を記録するようにしました。
- webhook URLs とその他 secrets を audit metadata と CSV output から除外しました。

## 0.7.0

- reporter playtime と server account age gates を追加しました。
- `threshold_pending` を使う multi-report threshold flow を追加しました。
- score threshold notifications を追加しました。

## 0.6.0

- optional Discord webhook notifications を追加しました。
- event-level notification toggles、JSON escaping、content truncation、failure log rate limiting を追加しました。

## 0.5.0

- TAB completion と help commands を追加しました。
- `bans.unban_reason` migration と ban history output 改善を追加しました。
- command input validation を改善しました。

## 0.4.0

- ban review gates と bypass checks を安全化しました。
- ban history、ban info、unban、pardon commands を追加しました。

## 0.3.0

- false-report penalties と report suspension を追加しました。
- score recovery と richer report listing を追加しました。

## 0.2.0

- staff report review と score administration commands を追加しました。
- score history と manual recovery workflows を追加しました。

## 0.1.0

- PaperMC 26.1.2 / Java 25 plugin の初期版です。
- SQLite storage、player reputation scores、`/reportbad`、`/rep`、basic automatic profile bans を追加しました。
