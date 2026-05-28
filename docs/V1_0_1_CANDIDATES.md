# v1.0.1 Candidates

この文書は ReputationBan v1.0.0 公開後に確認された bugfix 候補を整理するための一覧です。現時点では v1.0.1 の内容、tag、Release は未確定です。

## 現時点の候補

- 未確定。実ユーザー報告または追加 runtime smoke の結果が入ったら追記します。
- DiscordSRV token-configured smoke は Phase 34 で `NOT_RUN` として判断記録済みです。本番で DiscordSRV 通知や account link context を使う前の運用確認候補として残します。

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
- DB schema は Phase 33 では変更しません。
