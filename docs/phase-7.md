# Phase 7 / v0.7.0

Phase 7 strengthens report safety for public servers.

## Reporter Requirements

`report-requirements.min-playtime-minutes` rejects `/reportbad` when the reporter's Paper statistic playtime is too low. `0` or less disables the check.

`report-requirements.min-account-age-days` rejects `/reportbad` when the reporter has not been recorded on this server long enough. This uses ReputationBan `players.first_seen`; it is not Mojang account age. `0` or less disables the check.

## Multi-Report Threshold

`rating.min-unique-reports-before-deduction` controls automatic deductions.

- `1`: non-review categories are `auto_accepted` and deducted immediately.
- `2+`: non-review categories are stored as `threshold_pending`.

When the same target and category receive enough unique `threshold_pending` reporters within `rating.report-window-days`, ReputationBan updates those pending reports to `auto_accepted` and applies one deduction. Later reports start a new threshold group.

`threshold_pending` is not staff review. Staff review still uses `pending`.

## Score Threshold Notifications

Downward crossings of `warning`, `watch`, `restricted`, and `final-warning` in `score-thresholds` send staff and Discord notifications with `score-threshold-crossed`. The `ban` threshold remains covered by the auto-ban event.
