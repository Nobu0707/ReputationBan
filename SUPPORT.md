# Support

ReputationBan v1.0.0 の相談や不具合報告では、環境情報、診断結果、runtime smoke の状態をそろえると triage しやすくなります。secret は貼らないでください。

## サポートを受ける前に

可能な範囲で次を確認してください。

- `/rep version`
- `/rep doctor`
- `/rep integrations`
- `/rep integrations test`
- `/rep support bundle`
- `docs/BUGFIX_INTAKE.md`
- `docs/POST_RELEASE_MONITORING.md`
- `docs/SUPPORT_BUNDLE.md`
- `docs/SECURITY_REDACTION.md`

## 共有してほしい情報

- ReputationBan version
- Paper version
- Java version
- 導入している optional plugins と version
- 該当する `config.yml` の redacted 抜粋
- 実行した command と permission
- `/rep doctor` の結果
- `/rep integrations` と `/rep integrations test` の結果
- runtime smoke 結果
- `/rep support bundle` の有無
- 必要最小限の log 抜粋

## ログやconfig共有時のredaction

次の値は公開Issue、chat、review archive に貼らないでください。

- Discord bot token
- Discord Webhook URL
- password
- session / cookie
- secret
- private database
- server log 全体

`/rep support bundle` は DB files、WAL/SHM、server logs、Webhook URL を含めない診断 ZIP を作成します。共有前に `config-redacted.yml` を開き、secret が `<redacted>` になっていることを確認してください。

## DiscordSRV token configured smoke

Phase 34 時点の DiscordSRV token-configured runtime smoke は `NOT_RUN` / `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE` です。これは、本番で DiscordSRV を使う予定や token-configured 環境情報が提供されていないための正しい記録であり、PASS ではありません。

本番で DiscordSRV 通知または account link context を使う場合は、`docs/DISCORDSRV_CONFIGURED_RUNTIME_SMOKE_CHECKLIST.md` に沿って token configured 環境で確認してください。bot token の値は共有せず、configured 状態は yes/no だけで十分です。

## v1.0.1 intake

v1.0.1 候補は `docs/V1_0_1_CANDIDATES.md` に整理します。v1.0.x は原則 bugfix 中心です。新機能、GUI、Discord button approval、Discord role 変更、Folia support、appeal workflow は v1.1.0 以降の候補として扱います。
