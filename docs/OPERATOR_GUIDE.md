# Operator Guide

このガイドは、ReputationBan を導入・運用するサーバー運営、管理者、モデレーター向けです。ReputationBan 1.0.1 は PaperMC 26.1.2 / Java 25 向けの v1.0.1 production hardening candidate です。`v1.0.1` tag と GitHub Release はまだ作成していません。

Javaコード、DBスキーマ、config key を変更せずに、既存の機能を安全に運用するための使い方をまとめます。

## 導入後に最初に確認すること

導入直後は、いきなり自動BANへ寄せず、まず診断と通報審査の流れを確認してください。

推奨初期確認コマンドです。

```mcfunction
/rep version
/rep doctor
/rep integrations
/rep placeholders
/reports list all 10
```

確認するポイントです。

- `/rep version` で version が `1.0.1` であること。
- `/rep doctor` で database、tables、config、backup directory、support bundle が正常であること。
- `/rep integrations` で LuckPerms、CoreProtect、WorldGuard、GriefPrevention、PlaceholderAPI、DiscordSRV の状態を把握すること。
- `/rep placeholders` で PlaceholderAPI 用 placeholder を確認すること。
- `/reports list all 10` で既存通報の有無と表示を確認すること。
- server log に startup error、SQL error、command exception、secret の露出がないこと。

## 通報対応フロー

通報は、一覧、詳細、証拠、承認または却下の順に確認します。

```mcfunction
/reports list pending 10
/reports view <id>
/reports evidence <id>
/reports approve <id> <note>
/reports reject <id> <note>
```

基本フローです。

1. `/reports list pending 10` または `/reports list all 10` で対象を探します。
2. `/reports view <id>` で reporter、target、category、reason、status、時刻を確認します。
3. `/reports evidence <id>` で保存済みの連携証拠情報を確認します。
4. 通報内容、証拠、サーバールールを照合します。
5. 妥当なら `/reports approve <id> <note>` で承認します。
6. 不十分または虚偽の可能性が高い場合は `/reports reject <id> <note>` で却下します。

`<note>` には、あとで監査できる短い判断理由を残してください。個人情報、Discord bot token、Webhook URL、password、cookie、session は書かないでください。

## スコア管理

手動でスコアを調整する場合は、理由を残して実行します。

```mcfunction
/rep add <player> <points> <reason>
/rep remove <player> <points> <reason>
/rep set <player> <score> <reason>
```

運用上の注意です。

- `/rep remove` と `/rep set` でBANしきい値を下回る場合、`reputationban.admin.ban` 権限も必要です。
- OP、`reputationban.bypass`、LuckPerms bypass group は TargetProtectionService で保護されます。
- 手動変更も audit log と score history に残ります。
- 理由は簡潔にし、secret や不要な個人情報を含めないでください。

## BAN管理

BAN関連の主なコマンドです。

```mcfunction
/rep baninfo <player>
/rep banhistory <player> [limit]
/rep unban <player> <reason>
/rep pardon <player> <reason>
```

ReputationBan は Profile BAN API を使います。`/rep pardon` は unban、通報制限解除、スコア回復をまとめて行う運用向けコマンドです。

`/rep ban` という直接BAN用コマンドは 1.0.1 にはありません。BANは、通報承認またはスコア変更によってBANしきい値を跨いだときに、設定に従って処理されます。手動でBAN状態を確認する場合は `/rep baninfo` と `/rep banhistory` を使ってください。

## 監査ログ

最近の操作確認やCSV exportには次を使います。

```mcfunction
/rep audit recent
/rep audit export recent
/rep audit <player> [limit]
/rep audit export <player> [limit]
/rep audit type <eventType> [limit]
```

公開サーバーでは、定期的に `/rep audit recent` を確認してください。score変更、通報承認、BAN、unban、pardon、support bundle 作成、maintenance などの流れを追えます。

## メンテナンス

retention cleanup は preview で件数を確認してから、confirm 付きで実行します。

```mcfunction
/rep maintenance preview
/rep maintenance run confirm
```

`/rep maintenance run confirm` は cleanup 前に SQLite backup を作成します。削除対象には audit events、rejected reports、cancelled reports、設定次第で score history や bans が含まれます。

## バックアップ

手動バックアップは次を使います。

```mcfunction
/rep backup
```

本番では、定期的に `/rep backup` を実行し、サーバー全体のバックアップ方針とも組み合わせてください。backup file の保存場所と復旧手順は、運営チーム内で共有しておくことを推奨します。

## Support Bundle

診断情報をまとめる場合は次を使います。

```mcfunction
/rep support bundle
```

support bundle は DB files、WAL/SHM、server logs、Webhook URL を含めない設計です。ただし、共有前に必ず ZIP 内の `config-redacted.yml` を確認し、Discord bot token、Webhook URL、password、cookie、session、secret が伏せられていることを確認してください。

## 外部連携の見方

連携状態は次で確認します。

```mcfunction
/rep integrations
/rep integrations test
/reports evidence <id>
```

各連携の役割です。

- LuckPerms: bypass-groups、reporter weight、offline lookup を扱います。v1.0.1 では offline bypass protection により、未ロードの保護対象も設定に応じて保護できます。
- CoreProtect: `griefing` 通報時の周辺ブロックログを証拠情報として保存します。rollback、restore、purge は実行しません。
- WorldGuard: 通報地点の region context を保存します。region や flag は変更しません。
- GriefPrevention: 通報地点の claim context を保存します。claim や trust は変更しません。
- PlaceholderAPI: `%reputationban_score%` などの placeholder を提供します。
- DiscordSRV: account link context と通知を扱います。bot token 未設定なら WARN です。通知はデフォルト無効です。

## 100人規模運用時の推奨設定

公開サーバーや100人規模の運用では、初期段階から完全自動BANへ寄せすぎないことを推奨します。まず複数通報、通報条件、cooldown、staff review を厚めにしてください。

推奨例です。

```yaml
rating:
  min-unique-reports-before-deduction: 3
  report-window-days: 7

report-requirements:
  min-playtime-minutes: 60
  min-account-age-days: 1

cooldowns:
  global-report-seconds: 300
  same-target-cooldown-days: 14
  max-reports-per-day: 5
  max-reports-per-week: 15
```

公開サーバーでの推奨です。

- いきなり完全自動BANへ寄せすぎない。
- 初期運用では `staff-review-required` を多めにする。
- 定期的に `/rep audit recent` を見る。
- `/rep backup` を定期的に取る。
- support bundle 共有時に secret を確認する。

## 本番前チェックリスト

- PaperMC 26.1.2 / Java 25 で起動する。
- `/rep version` が `1.0.1` を表示する。
- `/rep doctor` が重大な ERROR を出していない。
- `/rep integrations` で使う連携の状態を把握している。
- `/reports list all 10` が例外なく動く。
- `/rep backup` が成功する。
- `/rep support bundle` が作成でき、redaction を確認済み。
- `rating.min-unique-reports-before-deduction` と `staff-review-required` が運用方針に合っている。
- Discord Webhook 通知または DiscordSRV 通知を使う場合、secret を公開せずに設定済み状態だけを確認している。
- v1.0.1 hardening の注意点を運営チームで共有している。

## トラブル対応

問題が起きた場合は、まず次を集めてください。

- ReputationBan version、Paper version、Java version。
- 導入している optional plugins と version。
- 実行した command、permission、対象プレイヤー。
- `/rep doctor` の結果。
- `/rep integrations` と `/rep integrations test` の結果。
- `/rep support bundle`。
- 必要最小限の redacted log。

公開Issueやsupport bundleには、Discord bot token、Webhook URL、password、cookie、session、secret を貼らないでください。

## DiscordSRV WARNの扱い

DiscordSRV は optional integration です。DiscordSRV が未導入、bot token 未設定、API unavailable の場合は WARN になり得ます。

ReputationBan 本体、通報、スコア管理、他の optional integration が正常なら、DiscordSRV WARN だけで直ちに本番停止にする必要はありません。ただし、本番で DiscordSRV 通知または account link context を使う場合は、token configured 環境で追加確認してください。

通知はデフォルト無効です。Discord から Minecraft コマンドを実行する機能や Discord role 変更はありません。

## v1.0.1 hardeningの注意

v1.0.1 では、本番運用向けに次の安全性が強化されています。

- OP、`reputationban.bypass`、LuckPerms bypass group の保護判定を TargetProtectionService に集約。
- LuckPerms offline lookup による offline bypass protection。
- SQLite `busy_timeout` と `synchronous = NORMAL`。
- DatabaseManager shutdown hardening。
- report reason、review note、audit reason、report_context summary の長さ制限。
- report_context retention と orphan cleanup。
- staff-review report の transaction 化。
- production-risk config warning。

特に `integrations.luckperms.offline-lookup.fail-closed-for-bypass: true` は、lookup failure や timeout 時に対象を保護扱いにする安全側設定です。連携障害時にBAN回避が起きる可能性があるため、運用方針に合わせて確認してください。

## 禁止・注意事項

- Discord bot token / Webhook URL / password / cookie / sessionをIssueやsupport bundleに貼らないでください。
- CoreProtect rollback/restore/purgeはReputationBanから実行しません。
- LuckPerms権限変更はReputationBanから行いません。
- WorldGuard region/flag変更はReputationBanから行いません。
- GriefPrevention claim/trust変更はReputationBanから行いません。
- DiscordからMinecraftコマンドは実行しません。
