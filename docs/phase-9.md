# Phase 9 / v0.9.0

Phase 9 では、v1.0.0 前に operational paths を hardening しました。

## Audit And CSV

- command actors は `actor_uuid` と `actor_name` を分離して保持します。
- console actions は `actor_uuid=NULL`、`actor_name=CONSOLE` を使います。
- audit CSV exports は `plugins/ReputationBan/` 配下に制限されます。unsafe relative path や absolute path は `exports` に fallback します。

## Maintenance

- `/rep maintenance preview` は削除せず cleanup candidates を数えます。
- `/rep maintenance run` は confirm guidance のみを表示します。
- `/rep maintenance run confirm` は削除前に `backups/reputationban-before-maintenance-YYYYMMDD-HHMMSS.db` を作成します。
- `MAINTENANCE_PREVIEW` と `MAINTENANCE_RUN` audit events は cleanup counts を記録します。

## Config Validation

startup と `/rep reload` で score bounds、cooldowns、report requirements、retention days、audit limits、Discord timeout range、unsafe audit export paths を検証します。Phase 9 では errors が plugin を無効化するわけではありませんが、logs と reload sender summary に明確に表示します。

## Runtime Smoke

repository-side checks には `scripts/run-local-smoke-check.sh` を使い、その後 PaperMC 26.1.2 server と Java 25 で `docs/runtime-smoke-checklist.md` に従って確認します。
