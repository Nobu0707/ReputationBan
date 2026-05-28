# Issue Triage Guide

この文書は ReputationBan v1.0.0 公開後の issue / PR intake dry-run と v1.0.1 候補判断に使う triage guide です。

## 重大度分類

- `blocker`: server 起動不能、data loss risk、false ban / pardon failure、secret exposure、release artifact problem。
- `high`: 主要 command の失敗、必須 runtime smoke FAIL、optional plugin なしで単体動作が壊れる問題。
- `medium`: 特定 optional plugin、config、command flow で再現し、回避策がある不具合。
- `low`: 表示、文言、限定的 edge case、運用上の軽微な不便。
- `docs`: 実装変更なしで documentation、template、checklist の修正で解決できるもの。

## ラベル候補

- `bug`
- `integration`
- `discordsrv`
- `luckperms`
- `coreprotect`
- `worldguard`
- `griefprevention`
- `placeholderapi`
- `support-bundle`
- `docs`
- `v1.0.1-candidate`
- `needs-reproduction`
- `needs-logs`
- `security-sensitive`

## 初動

- secret を貼っていないか確認します。Discord bot token、Webhook URL、password、session、cookie、secret は公開 issue に残しません。
- support bundle の有無を確認します。
- `/rep doctor` の結果を確認します。
- `/rep integrations` と `/rep integrations test` の結果を確認します。
- runtime smoke 結果を確認します。
- `config.yml` の該当箇所を redacted で確認します。
- optional plugin version を確認します。

## v1.0.1へ入れる条件

- confirmed regression
- startup failure
- data loss risk
- false ban / pardon failure
- secret exposure
- release artifact problem

## v1.1.0以降へ送る条件

- new feature
- UI/GUI
- new integration
- behavior change requiring config redesign

## DiscordSRV Configured Smoke

DiscordSRV configured smoke が `NOT_RUN` / `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE` のままでも、それだけでは confirmed bug ではありません。token-configured environment で再現する実装不具合、secret exposure、startup failure、false moderation behavior が確認された場合だけ v1.0.1 candidate に昇格します。
