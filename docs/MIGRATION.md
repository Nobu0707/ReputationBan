# Migration

ReputationBan 0.11.0 uses SQLite under `plugins/ReputationBan/reputationban.db` by default.

## Before Updating

1. Stop the server.
2. Back up `plugins/ReputationBan/reputationban.db`.
3. Back up `plugins/ReputationBan/config.yml`.
4. Replace the plugin JAR with `ReputationBan-0.11.0.jar`.
5. Start the server and run `/rep doctor`.

## Automatic Schema Migration

ReputationBan creates missing tables and adds known missing columns during startup. The current schema includes:

- `players`
- `reports`
- `score_history`
- `bans`
- `audit_events`

Known migrations from earlier phases include:

- `players.last_recovery_at`
- `players.first_seen`
- `players.last_seen`
- `bans.unban_reason`
- `bans.unbanned_by_name`
- `audit_events`

## Rollback Notes

Do not roll back the JAR without also restoring the database backup from before the upgrade. Older plugin versions may not understand newer columns or audit data. If rollback is required, stop the server, move the current SQLite file aside, restore the backed-up SQLite file, restore the matching config, and then start the older JAR.

## Maintenance Backups

`/rep maintenance run confirm` creates `plugins/ReputationBan/backups/reputationban-before-maintenance-*.db` before retention cleanup. This backup is for maintenance rollback, not a replacement for full pre-upgrade backups.
