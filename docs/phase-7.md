# Phase 7 / v0.7.0

Phase 7 では、public server 向けに report safety を強化しました。

## Reporter Requirements

`report-requirements.min-playtime-minutes` は reporter の Paper statistic playtime が不足している場合に `/reportbad` を拒否します。`0` 以下で無効です。

`report-requirements.min-account-age-days` は ReputationBan がこの server で初めて記録した `players.first_seen` からの日数で判定します。Mojang account age ではありません。`0` 以下で無効です。

## Multi-Report Threshold

`rating.min-unique-reports-before-deduction` は automatic deductions に必要なユニーク通報者数です。

- `1`: staff review 不要 category は `auto_accepted` になり、すぐ減点します。
- `2+`: staff review 不要 category は `threshold_pending` として保存します。

同じ target と category に対して `rating.report-window-days` 内に必要数の unique `threshold_pending` reporters が集まると、該当 reports を `auto_accepted` に更新し、1回だけ減点します。

`threshold_pending` は staff review 待ちではありません。staff review は `pending` を使います。

## Score Threshold Notifications

`score-thresholds` の `warning`、`watch`、`restricted`、`final-warning` を下方向に跨いだ場合、`score-threshold-crossed` で staff と Discord に通知します。`ban` threshold は auto-ban event が扱います。
