# Phase 35

Phase 35 は v1.0.0 公開後の support / GitHub issue templates / contribution docs phase です。version bump、`v1.0.1` tag 作成、GitHub Release asset 差し替え、GitHub Release 削除、`v1.0.0` tag 移動は行いません。

## 目的

- GitHub issue templates を追加し、bug report、integration issue、support request、feature request を分けて受け付ける。
- bug report 時に必要な Paper version、Java version、ReputationBan version、optional integrations、support bundle、runtime smoke 結果を集めやすくする。
- DiscordSRV / optional integrations / support bundle / smoke 結果の記入欄を用意する。
- `SECURITY.md`、`SUPPORT.md`、`CONTRIBUTING.md` を追加する。
- v1.0.1 candidates への triage 導線を整える。
- 既存の release status、runtime gates、secret safety を再確認する。
- review archive に Phase 35 の確認結果を含める。

## GitHub issue templates

追加した template:

- `.github/ISSUE_TEMPLATE/bug_report.yml`
- `.github/ISSUE_TEMPLATE/integration_issue.yml`
- `.github/ISSUE_TEMPLATE/support_request.yml`
- `.github/ISSUE_TEMPLATE/feature_request.yml`
- `.github/ISSUE_TEMPLATE/config.yml`
- `.github/pull_request_template.md`

各 template では Discord bot token、Discord Webhook URL、password、session、cookie、secret を貼らないことを明記します。DiscordSRV の bot token configured 状態は yes/no だけを扱い、token 値は要求しません。

## SUPPORT.md / SECURITY.md / CONTRIBUTING.md

- `SUPPORT.md` は `/rep version`、`/rep doctor`、`/rep integrations`、`/rep support bundle`、bugfix intake、post-release monitoring、redaction 方針を案内します。
- `SECURITY.md` は support target、脆弱性報告方法、公開Issueに貼ってはいけない情報、support bundle、緊急度、対応方針を整理します。
- `CONTRIBUTING.md` は Java 25、PaperMC 26.1.2、Gradle、test commands、optional dependency safety、外部API直接 import 制限、破壊的操作禁止、PR前チェック、v1.0.x の bugfix 中心方針を整理します。

## v1.0.1 intake

現時点の confirmed bug candidates はありません。DiscordSRV token-configured runtime smoke は `NOT_RUN` / `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE` の運用確認候補として残します。Phase 35 の docs/support improvements は完了扱いです。feature requests は原則 v1.1.0 以降候補として整理します。

## v1.0.0は変更しない

- version は `1.0.0` のままです。
- `v1.0.0` tag は Phase 30 commit `b422e72ec5a917cdc04dee902e96a0cef190026c` を指したままです。
- GitHub Release `v1.0.0` は published のままです。
- GitHub Release assets は差し替えません。
- DB schema、runtime behavior、新機能は変更しません。
