# ReputationBan

ReputationBan は、通報とスタッフ操作をもとにプレイヤーの評判スコアを管理する PaperMC 向け moderation プラグインです。データは SQLite に保存し、未処理通報の審査、監査ログ、バックアップ、support bundle、設定に基づく Profile BAN を扱います。

現在のバージョン: `1.0.0`

v1.0.0 は GitHub Release 公開済みです。`v1.0.0` annotated tag は Phase 30 commit `b422e72ec5a917cdc04dee902e96a0cef190026c` を指し、GitHub Release は `draft=false`、`prerelease=false` として公開されています。Release assets は `ReputationBan-1.0.0.jar`、JAR sha256、`ReputationBan-1.0.0-release.zip`、release zip sha256 の4件です。DiscordSRV は bot token 未設定時に WARN として扱い、ReputationBan 本体や他連携の release gate は止めません。Phase 34 では DiscordSRV token-configured runtime smoke を実施できる環境情報がないため `NOT_RUN` / `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE` として記録しました。Phase 35 では GitHub issue templates、PR template、`SUPPORT.md`、`SECURITY.md`、`CONTRIBUTING.md` を追加し、公開後サポートと v1.0.1 intake の導線を整えました。Phase 36 では maintenance baseline と issue/PR intake dry-run を追加し、open issues / open PRs は none、confirmed bug candidates は none として記録しました。

## 対象環境

| 項目 | 内容 |
| --- | --- |
| Minecraft / Server | PaperMC 26.1.2 |
| Java | Java 25 |
| Build | Gradle Kotlin DSL |
| Storage | SQLite |
| Main package | `dev.modplugin.reputationban` |

## ドキュメント

- [導入手順](docs/INSTALLATION.md)
- [設定説明](docs/CONFIGURATION.md)
- [移行手順](docs/MIGRATION.md)
- [リリース前確認](docs/RELEASE_READINESS.md)
- [Support bundle](docs/SUPPORT_BUNDLE.md)
- [Security and redaction](docs/SECURITY_REDACTION.md)
- [外部連携](docs/INTEGRATIONS.md)
- [Integration runtime smoke checklist](docs/INTEGRATION_RUNTIME_SMOKE_CHECKLIST.md)
- [DiscordSRV configured runtime smoke checklist](docs/DISCORDSRV_CONFIGURED_RUNTIME_SMOKE_CHECKLIST.md)
- [Player report runtime smoke checklist](docs/PLAYER_REPORT_RUNTIME_SMOKE_CHECKLIST.md)
- [Paper runtime smoke checklist](docs/runtime-smoke-checklist.md)
- [Release candidate checklist](docs/RELEASE_CANDIDATE_CHECKLIST.md)
- [v1.0.0 release plan](docs/V1_RELEASE_PLAN.md)
- [v1.0.0 release execution plan](docs/V1_RELEASE_EXECUTION_PLAN.md)
- [Post-release monitoring](docs/POST_RELEASE_MONITORING.md)
- [Bugfix intake](docs/BUGFIX_INTAKE.md)
- [Maintenance baseline](docs/MAINTENANCE_BASELINE.md)
- [Issue triage guide](docs/ISSUE_TRIAGE_GUIDE.md)
- [v1.0.1 candidates](docs/V1_0_1_CANDIDATES.md)
- [Support policy](SUPPORT.md)
- [Security policy](SECURITY.md)
- [Contribution guide](CONTRIBUTING.md)
- [Phase 36 notes](docs/phase-36.md)
- [Phase 35 notes](docs/phase-35.md)
- [Phase 34 notes](docs/phase-34.md)
- [Phase 33 notes](docs/phase-33.md)
- [Phase 32 notes](docs/phase-32.md)
- [Phase 31a notes](docs/phase-31a.md)
- [Phase 31 notes](docs/phase-31.md)
- [Phase 30 notes](docs/phase-30.md)
- [Phase 29 notes](docs/phase-29.md)
- [Phase 28 notes](docs/phase-28.md)
- [Phase 27 notes](docs/phase-27.md)
- [Phase 26 notes](docs/phase-26.md)
- [Phase 25 notes](docs/phase-25.md)
- [Phase 23 notes](docs/phase-23.md)
- [Phase 22 notes](docs/phase-22.md)
- [Phase 21 notes](docs/phase-21.md)
- [Phase 20 notes](docs/phase-20.md)
- [Phase 19 notes](docs/phase-19.md)
- [Changelog](CHANGELOG.md)

## 主な機能

- SQLite による評判スコア、通報、score history、BAN履歴、audit events の保存。
- `/reportbad` による通報、カテゴリ、クールダウン、通報条件、複数通報しきい値、スタッフ審査。
- `/reports` による pending report の一覧、詳細表示、承認、却下。
- `/rep` によるスコア確認、履歴、手動変更、BAN状態確認、unban、pardon。
- Profile BAN API を使った BAN 処理。`BanList.Type.NAME` は使いません。
- audit log、CSV export、retention preview、confirm付き maintenance cleanup。
- `/rep backup` による SQLite backup。
- `/rep support bundle` によるシークレットを伏せた診断 ZIP 作成。
- LuckPerms、CoreProtect、WorldGuard、GriefPrevention、PlaceholderAPI、DiscordSRV の任意連携。未導入でも単体動作します。
- PlaceholderAPI 対応プラグインから `%reputationban_score%` などで評判状態を参照できます。
- 任意の Discord Webhook 通知。URL は表示、ログ、監査、CSV、support bundle、review archive に出さない方針です。

## コマンド

- `/reportbad <player> <category> <reason>`: プレイヤーを通報します。
- `/rep`: 自分のスコアを表示します。
- `/rep version`: インストール済み ReputationBan のバージョンと対象ランタイムを表示します。
- `/rep help`: 利用可能なコマンドを表示します。
- `/rep check <player>`: 他プレイヤーのスコアを表示します。
- `/rep history <player> [limit]`: score history を表示します。
- `/rep banhistory <player> [limit]`: ReputationBan の BAN 履歴を表示します。
- `/rep baninfo <player>`: Paper/Profile BAN と ReputationBan DB の BAN 状態を表示します。
- `/rep unban <player> [reason...]`: Profile BAN を解除し、active DB ban を解除済みにします。
- `/rep pardon <player> [reason...]`: unban、通報停止解除、スコア回復をまとめて行います。
- `/rep audit recent [limit]`: 最近の audit events を表示します。
- `/rep audit <player> [limit]`: 対象プレイヤーの audit events を表示します。
- `/rep audit type <eventType> [limit]`: event type で audit events を表示します。
- `/rep audit export recent [limit]`: 最近の audit events を CSV export します。
- `/rep audit export <player> [limit]`: 対象プレイヤーの audit events を CSV export します。
- `/rep maintenance preview`: 削除せずに retention cleanup 対象件数を表示します。
- `/rep maintenance run`: 削除せず、confirm 手順を表示します。
- `/rep maintenance run confirm`: SQLite backup を作成してから retention cleanup を実行します。
- `/rep backup [reason...]`: 手動 SQLite backup を作成します。
- `/rep doctor`: Discord Webhook URL を出さずに運用診断を表示します。
- `/rep placeholders`: PlaceholderAPI placeholders の一覧を表示します。
- `/rep integrations`: LuckPerms / CoreProtect / WorldGuard / GriefPrevention / PlaceholderAPI / DiscordSRV の連携状態を表示します。
- `/rep integrations test`: LuckPerms / CoreProtect / WorldGuard / GriefPrevention / PlaceholderAPI / DiscordSRV 連携だけに絞った詳細診断を安全に実行します。DiscordSRV のテスト送信は行いません。
- `/rep diagnostics`: `/rep doctor` の alias です。
- `/rep support bundle`: DB files や server logs を含めない診断 ZIP を作成します。
- `/rep add <player> <points> [reason...]`: スコアを加算します。
- `/rep remove <player> <points> [reason...]`: スコアを減算します。
- `/rep set <player> <score> [reason...]`: スコアを指定値にします。
- `/rep reload`: 設定を再読み込みします。
- `/reports`: pending reports を表示します。
- `/reports help`: report review commands を表示します。
- `/reports list [pending|threshold_pending|approved|rejected|auto_accepted|cancelled|all] [limit]`: report を一覧表示します。
- `/reports view <id>`: report 詳細を表示します。
- `/reports evidence <id>`: report に保存された LuckPerms / CoreProtect / WorldGuard / GriefPrevention / DiscordSRV 証拠情報を表示します。
- `/reports approve <id> [note...]`: pending report を承認し、減点を適用します。
- `/reports reject <id> [note...]`: pending report を却下します。

## 権限

- `reputationban.report`: `/reportbad` を使えます。
- `reputationban.score.self`: 自分のスコアを見られます。
- `reputationban.score.others`: 他プレイヤーのスコアと履歴を見られます。
- `reputationban.admin.score`: スコア管理コマンドを使えます。
- `reputationban.admin.reports`: reports を審査できます。
- `reputationban.admin.ban`: BAN に関係する承認や BAN 管理コマンドを使えます。
- `reputationban.admin.audit`: audit log の表示と export ができます。
- `reputationban.admin.maintenance`: retention cleanup preview/run と manual DB backup ができます。
- `reputationban.admin.diagnostics`: `/rep doctor` と support bundle を使えます。
- `reputationban.admin.integrations`: `/rep integrations` と `/rep integrations test` を使えます。
- `reputationban.notify`: staff notification を受け取れます。
- `reputationban.bypass`: online 中の通報、減点、自動 BAN 対象から除外されます。
- `reputationban.admin`: 主な admin 権限をまとめて付与します。

## 安全上の注意

- Discord Webhook URL はシークレットです。実 URL を config 以外へ貼らないでください。
- `support bundle` には `reputationban.db`、WAL/SHM、server logs、Webhook URL を含めない設計です。
- 共有前に `config-redacted.yml` を開き、`<redacted>` になっていることを確認してください。
- review archive に実 Webhook URL を含めないでください。
- secret scan は検出結果の確認用です。説明文に `token` や `secret` という単語があるだけで直ちに失敗とは扱いません。

## Build And Test

```bash
./gradlew clean test build
./scripts/review_code.sh
```

JAR は `build/libs/ReputationBan-1.0.0.jar` に生成されます。

## Support And Contribution

不具合報告や設定相談は GitHub issue templates に沿って、Paper version、Java version、ReputationBan version、optional integrations、`/rep doctor`、`/rep integrations`、support bundle、runtime smoke 結果を添えてください。Discord bot token、Discord Webhook URL、password、session、cookie、secret は貼らないでください。

v1.0.x は原則 bugfix と安全な docs/support 改善を中心に扱います。新機能は v1.1.0 以降候補として整理します。

## 現在の位置づけ

v1.0.0 は first stable release として公開済みです。Release URL は <https://github.com/Nobu0707/ReputationBan/releases/tag/v1.0.0> です。Paper runtime smoke、integration runtime smoke、player report runtime smoke の主要 gate は PASS/READY で揃い、`scripts/check-runtime-smoke-consistency.sh` で整合確認済みです。

Phase 31 では GitHub Release draft を確認し、`draft=false` へ変更して公開しました。Phase 31a では公開済み GitHub Release 本文と生成済み release notes / Go-No-Go report を `PUBLISHED` 状態へ揃えました。Phase 32 では v1.0.0 公開後監視、bugfix intake、v1.0.1 candidates docs を追加しました。Phase 33 では DiscordSRV token-configured runtime smoke の intake を追加しました。Phase 34 では token-configured smoke を実施できる環境情報がないため `NOT_RUN` として判断記録しました。Phase 35 では issue templates と support/security/contribution docs を追加しました。Phase 36 では v1.0.0 maintenance baseline、issue triage guide、issue/PR intake dry-run を追加し、open issues / open PRs / confirmed bug candidates が none であることを記録しました。DiscordSRV は bot token 未設定時 WARN 扱いのため、本番で DiscordSRV 通知や account link を使う場合は token 設定済み環境で追加確認してください。

## 現在の制限

- GUI menus、Discord ボタン承認、Discord role 変更、Discord から Minecraft コマンドを実行する機能は未実装です。
- WorldGuard region/flag の作成、変更、削除は行いません。
- GriefPrevention claim/trust の作成、変更、削除は行いません。
- LuckPerms が無い場合、offline bypass detection は OP 状態に限定されます。online players の `reputationban.bypass` は保護されます。
- appeal と automatic unban workflow は後続フェーズに残しています。
- Folia support は含みません。
