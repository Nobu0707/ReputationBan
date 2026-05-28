# Post-release Monitoring / v1.0.0

この文書は ReputationBan v1.0.0 公開後の監視項目です。新機能追加、version bump、`v1.0.0` tag 移動、GitHub Release asset 差し替えは行いません。

## 公開状態

- GitHub Release URL: <https://github.com/Nobu0707/ReputationBan/releases/tag/v1.0.0>
- Release status: `PUBLISHED`
- `isDraft=false`
- `isPrerelease=false`
- Release assets:
  - `ReputationBan-1.0.0.jar`
  - `ReputationBan-1.0.0.jar.sha256`
  - `ReputationBan-1.0.0-release.zip`
  - `ReputationBan-1.0.0-release.zip.sha256`

## 導入後確認

1. PaperMC 26.1.2 / Java 25 の test server または本番前 staging server を用意します。
2. `ReputationBan-1.0.0.jar` を `plugins/` に配置します。
3. server を起動し、初回起動ログに ReputationBan の enable error がないことを確認します。
4. `/plugins` で ReputationBan が有効化されていることを確認します。
5. `/rep version` で version が `1.0.0` であることを確認します。
6. `/rep doctor` で DB、backup、Discord webhook、support bundle の診断を確認します。
7. `/rep integrations` と `/rep integrations test` で任意連携の状態を確認します。
8. `latest.log` に startup error、SQL error、command exception、Webhook URL の露出がないことを確認します。

## DiscordSRV Warning

DiscordSRV は bot token 未設定、DiscordSRV API unavailable、または optional plugin 未導入の場合に WARN 扱いです。v1.0.0 では DiscordSRV 通知はデフォルト無効であり、ReputationBan 本体、Paper runtime smoke、他の optional integration runtime smoke の release gate は止めません。

本番で DiscordSRV 通知または account link context を使う場合は、bot token 設定済み環境で追加 smoke を実施し、`/rep integrations`、`/rep integrations test`、`/reports evidence <id>` の表示を確認してください。

Phase 33 以降は `docs/DISCORDSRV_CONFIGURED_RUNTIME_SMOKE_CHECKLIST.md` を使い、結果を `scripts/record-discordsrv-runtime-smoke-result.sh` で記録します。未設定環境では `NOT_RUN` として記録し、`scripts/check-discordsrv-runtime-readiness.sh` は `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE` を表示します。bot token、Webhook URL、secret、session、cookie、password は記録しません。

Phase 34 では DiscordSRV bot token configured 環境、本番利用予定、テスト用 Discord チャンネルが提供されていないため、DiscordSRV token-configured runtime smoke は実施せず `NOT_RUN` として記録しました。これは PASS ではなく、本番で DiscordSRV 通知または account link context を使う前の運用確認候補です。

## 問題報告時に集める情報

- Paper version
- Java version
- ReputationBan version
- 導入している optional plugins と version
- 該当する `config.yml` の設定値
- 実行した command と permission
- 発生時刻と関連する server log
- runtime smoke の結果
- DiscordSRV token-configured runtime smoke の結果。未実施の場合は `NOT_RUN` / `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE`
- `/rep doctor` の結果
- `/rep integrations` と `/rep integrations test` の結果
- `/rep support bundle` で作成した support bundle

## Support Bundle

`/rep support bundle` は DB files、WAL/SHM、server logs、Webhook URL を含めない診断 ZIP を作成します。共有前に ZIP 内の `config-redacted.yml` を確認し、Webhook URL、token、secret、password、session ID が伏せられていることを確認してください。

Webhook URL、bot token、secret、private config、実ユーザーの個人情報は issue、chat、review archive に貼らないでください。必要な場合は redacted value または再現用の最小 config で共有します。
