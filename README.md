# ReputationBan

ReputationBan は、通報とスタッフ操作をもとにプレイヤーの評判スコアを管理する PaperMC 向け moderation プラグインです。データは SQLite に保存し、未処理通報の審査、監査ログ、バックアップ、support bundle、設定に基づく Profile BAN を扱います。

現在のバージョン: `0.23.0`

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
- [Paper runtime smoke checklist](docs/runtime-smoke-checklist.md)
- [Release candidate checklist](docs/RELEASE_CANDIDATE_CHECKLIST.md)
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

JAR は `build/libs/ReputationBan-0.23.0.jar` に生成されます。

## 現在の位置づけ

v0.23.0 は Paper runtime smoke 自動化フェーズです。`~/servers/paper-26.1.2/start.sh` が `screen` で Paper を起動する実機テスト環境を前提に、`scripts/run-paper-runtime-smoke.sh` が JAR 配置、screen session 特定、console command 投入、ログ検査、必要時の stop を行います。環境がない場合は `NOT_RUN` として記録し、PASS 扱いにはしません。

v1.0.0 へ進む前に [Release candidate checklist](docs/RELEASE_CANDIDATE_CHECKLIST.md)、[Paper runtime smoke checklist](docs/runtime-smoke-checklist.md)、[Integration runtime smoke checklist](docs/INTEGRATION_RUNTIME_SMOKE_CHECKLIST.md) を確認してください。Paper / integration 実機スモークを実行していない場合は PASS と扱わず、未実施として記録します。

## 現在の制限

- GUI menus、Discord ボタン承認、Discord role 変更、Discord から Minecraft コマンドを実行する機能は未実装です。
- WorldGuard region/flag の作成、変更、削除は行いません。
- GriefPrevention claim/trust の作成、変更、削除は行いません。
- LuckPerms が無い場合、offline bypass detection は OP 状態に限定されます。online players の `reputationban.bypass` は保護されます。
- appeal と automatic unban workflow は後続フェーズに残しています。
- Folia support は含みません。
