# Configuration

This document summarizes the main `config.yml` sections for ReputationBan 0.11.0.

## Score

- `initial-score`: score assigned to a player when first recorded.
- `max-score`: upper bound for normal score recovery and display.

## Rating

- `rating.enabled`: enables report-based score changes.
- `rating.default-deduction`: fallback deduction when a category does not override it.
- `rating.min-reason-length`: minimum report reason length.
- `rating.min-unique-reports-before-deduction`: number of unique reporters required before automatic deduction.
- `rating.report-window-days`: time window for multi-report threshold aggregation.

## Categories

`categories` defines report category keys, display names, deductions, and whether staff review is required. Keep keys stable once players and reports exist.

## Cooldowns

- `cooldowns.global-report-seconds`: minimum time between reports from the same reporter.
- `cooldowns.same-target-cooldown-days`: minimum time before the same reporter can report the same target again.
- `cooldowns.max-reports-per-day`: daily report limit.
- `cooldowns.max-reports-per-week`: weekly report limit.

## Report Requirements

- `report-requirements.min-playtime-minutes`: minimum server playtime before reporting.
- `report-requirements.min-account-age-days`: minimum days since ReputationBan first saw the player on this server.

## Score Thresholds

`score-thresholds` controls warning, watch, restricted, final-warning, and ban threshold values. Non-ban thresholds trigger staff notifications when crossed downward.

## Score Recovery

- `score-recovery.enabled`: enables scheduled recovery.
- `score-recovery.points-per-day`: recovery amount.
- `score-recovery.max-score`: recovery cap.
- `score-recovery.no-report-days-required`: quiet period before recovery.

## Ban

- `ban.enabled`: enables automatic profile bans.
- `ban.threshold`: score at or below which automatic ban logic applies.
- `ban.source`: source shown for bans.
- `ban.durations`: duration by repeated ban count.

## Notify

- `notify.console`: sends staff notifications to console.
- `notify.in-game-staff`: sends staff notifications to online staff.
- `notify.staff-permission`: permission for staff notification recipients.

## Discord Webhook

`notify.discord-webhook` is disabled by default. The `url` value is a secret. Never commit it, paste it into review archives, or share it in support logs. ReputationBan should only display whether the URL is configured.

## Audit

- `audit.enabled`: controls audit event recording.
- `audit.export-directory`: CSV export directory, constrained under the plugin data folder.
- `audit.max-display-limit`: maximum command display limit.
- `audit.max-export-limit`: maximum CSV export limit.

## Retention

Retention settings control cleanup for audit events, rejected reports, cancelled reports, score history, and bans. `score-history-days` and `bans-days` default to `0`, meaning no cleanup.

## Database

- `database.type`: currently SQLite.
- `database.file`: SQLite file name under `plugins/ReputationBan/`.
