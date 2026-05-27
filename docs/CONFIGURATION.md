# Configuration

このドキュメントでは ReputationBan 0.14.0 の主な `config.yml` セクションを説明します。YAML key は変更しないでください。

## Score

- `initial-score`: 初めて記録されるプレイヤーに付与する初期スコアです。
- `max-score`: 通常の回復と表示で使うスコア上限です。

## Rating

- `rating.enabled`: report によるスコア変更を有効化します。
- `rating.default-deduction`: category に個別指定がない場合の減点値です。
- `rating.min-reason-length`: report reason の最小文字数です。
- `rating.min-unique-reports-before-deduction`: 自動減点に必要なユニーク通報者数です。
- `rating.report-window-days`: 複数通報しきい値の集計期間です。

## Categories

`categories` は report category key、表示名、deduction、staff review が必要かどうかを定義します。players や reports が作成された後は、category key を安易に変更しないでください。

## Cooldowns

- `cooldowns.global-report-seconds`: 同じ reporter が次に report できるまでの最短秒数です。
- `cooldowns.same-target-cooldown-days`: 同じ reporter が同じ target を再通報できるまでの日数です。
- `cooldowns.max-reports-per-day`: 1日あたりの report 上限です。
- `cooldowns.max-reports-per-week`: 1週間あたりの report 上限です。

## Report Requirements

- `report-requirements.min-playtime-minutes`: `/reportbad` に必要な server playtime です。
- `report-requirements.min-account-age-days`: ReputationBan がその player を初めて見た日から必要な日数です。Mojang account creation date ではありません。

## Score Thresholds

`score-thresholds` は `warning`、`watch`、`restricted`、`final-warning`、`ban` のしきい値を制御します。BAN 以外のしきい値を下方向に跨いだ場合は staff notification が送信されます。

## Score Recovery

- `score-recovery.enabled`: scheduled recovery を有効化します。
- `score-recovery.points-per-day`: 1日あたりの回復量です。
- `score-recovery.max-score`: 回復上限です。
- `score-recovery.no-report-days-required`: 回復前に必要な report なし期間です。

## Ban

- `ban.enabled`: automatic profile bans を有効化します。
- `ban.threshold`: score がこの値以下のとき automatic ban logic の対象になります。
- `ban.source`: BAN に表示する source です。
- `ban.durations`: BAN 回数ごとの duration です。

## Notify

- `notify.console`: staff notification を console に送信します。
- `notify.in-game-staff`: online staff に staff notification を送信します。
- `notify.staff-permission`: staff notification の受信権限です。

## Notify Discord Webhook

`notify.discord-webhook` はデフォルトで無効です。`url` はシークレットです。commit、review archive、support logs、screenshots に含めないでください。ReputationBan は URL そのものではなく、設定済みかどうかだけを表示する方針です。

`/rep support bundle` は live config ではなく `config-redacted.yml` を書き出します。`url`、`webhook`、`password`、`token`、`secret`、`session`、`cookie` を含む key は `<redacted>` に置換されます。

## Audit

- `audit.enabled`: audit event recording を制御します。
- `audit.export-directory`: CSV export directory です。plugin data folder の内側へ制限されます。
- `audit.max-display-limit`: command display の上限です。
- `audit.max-export-limit`: CSV export の上限です。

## Retention

Retention settings は audit events、rejected reports、cancelled reports、score history、bans の cleanup を制御します。`score-history-days` と `bans-days` はデフォルト `0` で、削除しない設定です。

## Database

- `database.type`: 現在は SQLite です。
- `database.file`: `plugins/ReputationBan/` 配下の SQLite file name です。
