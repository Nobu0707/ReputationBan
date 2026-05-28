# Command Reference

このリファレンスは ReputationBan 1.0.1 の主要コマンドと権限ノードを、運用中に探しやすい形でまとめたものです。詳細な運用フローは `docs/PLAYER_GUIDE.md` と `docs/OPERATOR_GUIDE.md` を参照してください。

## プレイヤーコマンド

| コマンド | 説明 | 主な権限 |
| --- | --- | --- |
| `/rep` | 自分の評判スコアを表示します。 | `reputationban.score.self` |
| `/reportbad <player> <category> <reason>` | プレイヤーを通報します。 | `reputationban.report` |

カテゴリ例です。

- `griefing`
- `abusive_chat`
- `spam`
- `scam`
- `harassment`
- `cheating`
- `other`

## 管理コマンド

| コマンド | 説明 | 主な権限 |
| --- | --- | --- |
| `/rep version` | version と対象ランタイムを表示します。 | なし |
| `/rep help` | 利用可能なコマンドを表示します。 | なし |
| `/rep check <player>` | 他プレイヤーのスコアを表示します。 | `reputationban.score.others` |
| `/rep history <player> [limit]` | score history を表示します。 | `reputationban.score.others` |
| `/rep reload` | 設定を再読み込みします。 | `reputationban.admin` |

## スコア管理コマンド

| コマンド | 説明 | 主な権限 |
| --- | --- | --- |
| `/rep add <player> <points> [reason...]` | スコアを加算します。 | `reputationban.admin.score` |
| `/rep remove <player> <points> [reason...]` | スコアを減算します。 | `reputationban.admin.score` |
| `/rep set <player> <score> [reason...]` | スコアを指定値にします。 | `reputationban.admin.score` |

BANしきい値を下回る操作には、追加で `reputationban.admin.ban` が必要です。

## BAN管理コマンド

| コマンド | 説明 | 主な権限 |
| --- | --- | --- |
| `/rep baninfo <player>` | Paper/Profile BAN と ReputationBan DB の状態を表示します。 | `reputationban.admin.ban` |
| `/rep banhistory <player> [limit]` | ReputationBan の BAN 履歴を表示します。 | `reputationban.admin.ban` |
| `/rep unban <player> [reason...]` | Profile BAN を解除し、active DB ban を解除済みにします。 | `reputationban.admin.ban` |
| `/rep pardon <player> [reason...]` | unban、通報停止解除、スコア回復をまとめて行います。 | `reputationban.admin.score` + `reputationban.admin.ban` |

`/rep ban` という直接BAN用コマンドは 1.0.1 にはありません。BANは通報承認やスコア変更でBANしきい値を跨いだときに設定に従って実行されます。

## 通報管理コマンド

| コマンド | 説明 | 主な権限 |
| --- | --- | --- |
| `/reports` | pending reports を表示します。 | `reputationban.admin.reports` |
| `/reports help` | report review commands を表示します。 | `reputationban.admin.reports` |
| `/reports list [pending|threshold_pending|approved|rejected|auto_accepted|cancelled|all] [limit]` | report を一覧表示します。 | `reputationban.admin.reports` |
| `/reports view <id>` | report 詳細を表示します。 | `reputationban.admin.reports` |
| `/reports evidence <id>` | 保存された連携証拠情報を表示します。 | `reputationban.admin.reports` |
| `/reports approve <id> [note...]` | pending report を承認し、減点を適用します。 | `reputationban.admin.reports` |
| `/reports reject <id> [note...]` | pending report を却下します。 | `reputationban.admin.reports` |

## 監査コマンド

| コマンド | 説明 | 主な権限 |
| --- | --- | --- |
| `/rep audit recent [limit]` | 最近の audit events を表示します。 | `reputationban.admin.audit` |
| `/rep audit <player> [limit]` | 対象プレイヤーの audit events を表示します。 | `reputationban.admin.audit` |
| `/rep audit type <eventType> [limit]` | event type で audit events を表示します。 | `reputationban.admin.audit` |
| `/rep audit export recent [limit]` | 最近の audit events を CSV export します。 | `reputationban.admin.audit` |
| `/rep audit export <player> [limit]` | 対象プレイヤーの audit events を CSV export します。 | `reputationban.admin.audit` |

## メンテナンスコマンド

| コマンド | 説明 | 主な権限 |
| --- | --- | --- |
| `/rep maintenance preview` | retention cleanup 対象件数を表示します。 | `reputationban.admin.maintenance` |
| `/rep maintenance run` | confirm 手順を表示します。 | `reputationban.admin.maintenance` |
| `/rep maintenance run confirm` | SQLite backup 作成後に retention cleanup を実行します。 | `reputationban.admin.maintenance` |

## バックアップ/サポートコマンド

| コマンド | 説明 | 主な権限 |
| --- | --- | --- |
| `/rep backup [reason...]` | 手動 SQLite backup を作成します。 | `reputationban.admin.maintenance` |
| `/rep doctor` | Webhook URL を出さずに運用診断を表示します。 | `reputationban.admin.diagnostics` |
| `/rep diagnostics` | `/rep doctor` の alias です。 | `reputationban.admin.diagnostics` |
| `/rep support bundle` | DB files や server logs を含めない診断 ZIP を作成します。 | `reputationban.admin.diagnostics` |

## 外部連携確認コマンド

| コマンド | 説明 | 主な権限 |
| --- | --- | --- |
| `/rep integrations` | optional integrations の状態を表示します。 | `reputationban.admin.integrations` |
| `/rep integrations test` | optional integrations に絞った安全な診断を実行します。 | `reputationban.admin.integrations` |
| `/rep placeholders` | PlaceholderAPI placeholders の一覧を表示します。 | `reputationban.admin.diagnostics` |
| `/reports evidence <id>` | report に保存された連携証拠情報を表示します。 | `reputationban.admin.reports` |

## 権限ノード一覧

| 権限 | 説明 | 既定 |
| --- | --- | --- |
| `reputationban.report` | `/reportbad` を使えます。 | true |
| `reputationban.score.self` | 自分のスコアを見られます。 | true |
| `reputationban.score.others` | 他プレイヤーのスコアと履歴を見られます。 | op |
| `reputationban.notify` | staff notification を受け取れます。 | op |
| `reputationban.admin` | 主な admin 権限をまとめて付与します。 | op |
| `reputationban.admin.reports` | reports を審査できます。 | op |
| `reputationban.admin.score` | スコア管理コマンドを使えます。 | op |
| `reputationban.admin.ban` | BAN管理コマンドとBANしきい値を跨ぐ承認に使います。 | op |
| `reputationban.admin.audit` | audit log の表示と export ができます。 | op |
| `reputationban.admin.maintenance` | retention cleanup preview/run と manual DB backup ができます。 | op |
| `reputationban.admin.diagnostics` | `/rep doctor`、`/rep diagnostics`、`/rep support bundle`、`/rep placeholders` を使えます。 | op |
| `reputationban.admin.integrations` | `/rep integrations` と `/rep integrations test` を使えます。 | op |
| `reputationban.bypass` | online 中の通報、減点、自動BAN対象から除外されます。 | op |

## Secret取り扱い

コマンド理由、review note、audit export、support bundle、GitHub Issue には、Discord bot token、Discord Webhook URL、password、cookie、session、secret を貼らないでください。
