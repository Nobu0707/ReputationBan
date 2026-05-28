# v1.0.1 Candidates

この文書は ReputationBan v1.0.0 公開後に確認された bugfix 候補を整理するための一覧です。Phase 37 では v1.0.1 hotfix candidate の実装内容を固定しましたが、tag と GitHub Release は未作成です。

## Confirmed bug candidates

- Phase 37 production hardening

v1.0.1 hotfix candidate には新機能を入れず、100人規模運用前に必要な保護判定、SQLite hardening、文字列長制限、score mutation 一貫性、BAN DB/Bukkit 不整合検出、追加 index を入れます。

## Phase 37 Hotfix Candidate

- Version: `1.0.1`
- Artifact: `ReputationBan-1.0.1.jar`
- Release artifact: `ReputationBan-1.0.1-release.zip`
- `v1.0.1` tag: NOT_CREATED
- GitHub Release `v1.0.1`: NOT_CREATED
- Player report runtime smoke: code changed; Codex-only run is `NOT_RUN` unless a real two-player environment is available.

## Phase 36 Intake Baseline

- Open issues: none
- Open PRs: none
- Confirmed bug candidates: none
- v1.0.1 candidates: none selected
- DiscordSRV configured smoke: `NOT_RUN` / `HOLD`, operational verification candidate

Phase 36 の issue/PR intake dry-run では、GitHub open issues と open PRs は 0 件でした。`docs/MAINTENANCE_BASELINE.md` と `docs/ISSUE_TRIAGE_GUIDE.md` を追加し、今後の triage では confirmed regression、startup failure、data loss risk、false ban / pardon failure、secret exposure、release artifact problem を v1.0.1 候補として扱います。

## Operational verification candidates

- DiscordSRV token-configured runtime smoke: `NOT_RUN` / `HOLD`
- 詳細 judgment: `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE`

DiscordSRV token-configured smoke は Phase 34 で `NOT_RUN` として判断記録済みです。本番で DiscordSRV 通知や account link context を使う前の運用確認候補として残します。

## Docs/support improvements

- completed in Phase 35
- GitHub issue templates、PR template、`SUPPORT.md`、`SECURITY.md`、`CONTRIBUTING.md`、`docs/phase-35.md` を追加済みです。

## Feature requests

- v1.1.0以降
- v1.0.x は原則 bugfix 中心です。新機能、GUI、Discord button approval、Discord role 変更、Folia support、appeal workflow は v1.1.0 以降候補として扱います。

## DiscordSRV token-configured runtime smoke

- current status: `NOT_RUN`
- readiness judgment: `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE`
- Phase 34 decision: token-configured environment or production-use decision was not provided, so the smoke was deferred and not marked PASS.
- actual run: no
- reason: DiscordSRV bot token configured environment, bot online state, test Discord channel, and production-use intent were not available in this workspace.
- v1.0.1 bugfix ではなく運用確認候補です。
- DiscordSRV 本番利用時は `docs/DISCORDSRV_CONFIGURED_RUNTIME_SMOKE_CHECKLIST.md` に沿った実施を推奨します。
- smoke 結果から bug が再現した場合のみ、再現条件、影響範囲、回避策を v1.0.1 candidates として追記します。
- bot token、Webhook URL、secret、session、cookie、password は記録しません。

## 追記ルール

- bug report の発生環境、再現手順、影響範囲、回避策、support bundle の有無を記録します。
- `blocker`、`high`、`medium`、`low`、`docs` の重大度を付けます。
- v1.0.1 に入れる場合は、DB schema 変更なし、既存 behavior の bugfix、docs correction の範囲に収めます。
- 新機能、UI追加、Discord command execution、Discord role mutation、Folia support、appeal workflow は原則 v1.1.0 以降で扱います。

## v1.0.0から変更しないもの

- `v1.0.0` tag は移動しません。
- v1.0.0 GitHub Release assets は差し替えません。
- v1.0.0 の version 表記は変更しません。
- DB schema は Phase 35 では変更しません。
