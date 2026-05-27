# Phase 8 / v0.8.0

Phase 8 では、audit logging と retention maintenance を追加しました。

## Audit Events

`audit_events` は横断的な investigation log です。既存 tables は canonical data として維持します。

- `reports`: report state
- `score_history`: actual score changes
- `bans`: ban history
- `audit_events`: who did what, when, and why

Audit metadata は意図的に制限しています。Webhook URLs、tokens、cookies、passwords、session IDs、secrets のような secret-like keys は含めません。

## Commands

- `/rep audit recent [limit]`
- `/rep audit <player> [limit]`
- `/rep audit type <eventType> [limit]`
- `/rep audit export recent [limit]`
- `/rep audit export <player> [limit]`
- `/rep maintenance run`

CSV exports はデフォルトで `plugins/ReputationBan/exports/` に書き出します。

## Retention

`retention.audit-events-days`、`retention.rejected-reports-days`、`retention.cancelled-reports-days` が cleanup を制御します。`0` 以下でその data type の cleanup は無効です。

`retention.score-history-days` と `retention.bans-days` はデフォルト `0` です。operator が明示しない限り重要な履歴を削除しません。
