# Bugfix Intake / v1.0.1 hotfix candidate

この文書は ReputationBan v1.0.0 公開後の bug report 受付、triage、v1.0.1 hotfix candidate 整理のための checklist です。新機能追加は原則 v1.1.0 以降で扱います。

Phase 35 以降は GitHub issue templates を使って、bug report、integration issue、support request、feature request を分けて受け付けます。Phase 36 では issue/PR intake dry-run を行い、open issues / open PRs は none、confirmed bug candidates は none として baseline を作成しました。公開Issueでは Webhook URL、bot token、secret、password、session ID、cookie を貼らないでください。

## 受付時の確認項目

- Paper version
- Java version
- ReputationBan version
- 導入している optional plugins
- optional plugins の version
- 該当する `config.yml` の設定箇所
- 実行した command と permission
- 発生した server、world、player 状態
- 関連する `latest.log` の抜粋
- `/rep doctor` の結果
- `/rep integrations` と `/rep integrations test` の結果
- `/rep support bundle` の ZIP
- Paper runtime smoke、integration runtime smoke、player report runtime smoke の結果
- DiscordSRV token-configured runtime smoke の結果。未実施なら `NOT_RUN` として扱い、PASS にはしません。
- 使用した GitHub issue template。bug report、integration issue、support request、feature request のどれかを記録します。
- `docs/ISSUE_TRIAGE_GUIDE.md` の severity と label 候補。

Webhook URL、bot token、secret、password、session ID、private database、server log 全体は共有しないでください。必要な情報だけを redacted して添付します。

## 重大度分類

- `blocker`: server 起動不能、data loss、BAN/審査の重大な誤動作、secret 漏えい、v1.0.0 artifact 利用不能。
- `high`: 主要 command が失敗する、runtime smoke の必須項目が FAIL、optional plugin 無しで単体動作が壊れる。
- `medium`: 特定 optional integration、特定 config、特定 command flow で再現する不具合。回避策がある。
- `low`: 表示崩れ、軽微な文言、運用上の不便、限定的な edge case。
- `docs`: 実装は正しく、README、docs、release notes、checklist の説明修正で解決できるもの。

## v1.0.1候補にする条件

- v1.0.0 公開後に実ユーザーまたは運用 smoke で再現した bug。
- version `1.0.0` の範囲で修正すべき regression。
- DB schema 変更なしで直せる bugfix。
- 新機能ではなく、既存機能の安全性、互換性、診断性を改善する修正。
- DiscordSRV token-configured smoke が未実施で、本番利用前の確認として残す必要がある項目。
- DiscordSRV token-configured smoke の結果から、実装不具合として再現した項目。単なる未実施は bugfix ではなく運用確認候補です。
- Phase 34 の DiscordSRV token-configured smoke は `NOT_RUN` です。token-configured environment または production-use decision が提供されていないためであり、v1.0.1 bugfix には昇格しません。
- docs-only で誤解や危険な運用を防げる項目。

Phase 37 では、issue 起因ではなく production hardening review 起因の v1.0.1 hotfix candidate を実装しました。以後 open issue または open PR が追加された場合は、再現条件、影響範囲、回避策、support bundle の有無を確認してから `docs/V1_0_1_CANDIDATES.md` に追記します。

Phase 38 では `docs/PLAYER_GUIDE.md`、`docs/OPERATOR_GUIDE.md`、`docs/COMMAND_REFERENCE.md` を追加しました。問い合わせが使い方や運用手順の誤解に由来する場合は、まず該当ガイドへのリンクで解決できるか確認してください。

## Hotfixが必要な条件

- secret が log、audit、CSV、support bundle、review archive に露出する。
- ReputationBan が server 起動を妨げる。
- Profile BAN、unban、pardon、report approval が重大に誤動作する。
- SQLite data loss、migration failure、backup failure が再現する。
- GitHub Release asset が破損しており、sha256 または release zip 検証に失敗する。

Hotfix が必要な場合でも、`v1.0.0` tag の移動、既存 Release asset の差し替え、DB schema の無断変更は行いません。必要な修正は別 commit、別 tag、別 Release として扱います。
