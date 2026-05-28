# Migration

ReputationBan 0.27.0 はデフォルトで `plugins/ReputationBan/reputationban.db` に SQLite database を保存します。

## 更新前

1. server を停止します。
2. `plugins/ReputationBan/reputationban.db` をバックアップします。
3. `plugins/ReputationBan/config.yml` をバックアップします。
4. plugin JAR を `ReputationBan-0.27.0.jar` に置き換えます。
5. server を起動して `/rep doctor` を実行します。

## 自動マイグレーション

ReputationBan は起動時に不足している tables を作成し、既知の不足 columns を追加します。現在の主な schema は次の通りです。

- `players`
- `reports`
- `score_history`
- `bans`
- `audit_events`

以前の phase からの既知 migration には次が含まれます。

- `players.last_recovery_at`
- `players.first_seen`
- `players.last_seen`
- `bans.unban_reason`
- `bans.unbanned_by_name`
- `audit_events`

## ロールバック時の注意

アップデート前の database backup を復元せずに古い JAR へ戻さないでください。古い plugin version は新しい columns や audit data を理解できない可能性があります。rollback が必要な場合は server を停止し、現在の SQLite file を退避し、更新前の SQLite file と対応する config を復元してから古い JAR を起動してください。

## Maintenance Backups

`/rep maintenance run confirm` は retention cleanup の前に `plugins/ReputationBan/backups/reputationban-before-maintenance-*.db` を作成します。これは maintenance rollback 用であり、アップデート前 backup の代わりではありません。

`/rep backup [reason]` は `plugins/ReputationBan/backups/reputationban-manual-backup-*.db` を作成します。アップデート前や maintenance 前の checkpoint として使えます。WAL/SHM sidecar files がある場合は一緒にコピーされます。
