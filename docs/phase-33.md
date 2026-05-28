# Phase 33

Phase 33 は v1.0.0 公開後の docs/scripts-only phase です。version bump、`v1.0.1` tag 作成、GitHub Release asset 差し替え、`v1.0.0` tag 移動は行いません。

## 目的

- DiscordSRV token-configured runtime smoke の checklist を追加する。
- DiscordSRV configured smoke の結果を `PASS` / `FAIL` / `NOT_RUN` で記録できるようにする。
- token 未設定環境を `NOT_RUN` / `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE` として明確に扱う。
- review archive に DiscordSRV configured smoke の readiness と latest summary を収集する。
- v1.0.1 candidates docs に DiscordSRV configured smoke の状態を残す。

## Tokenを記録しない方針

Discord bot token、Webhook URL、secret、session、cookie、password は docs、logs、review archive、support bundle に出しません。記録スクリプトは `note` に実値らしい secret が含まれる場合、summary へ保存する前に `<redacted>` へ置き換えます。

## NOT_RUN/HOLDの扱い

DiscordSRV bot token が未設定、DiscordSRV bot が online でない、または token-configured smoke を実施できない環境では PASS にしません。通常 readiness check は exit 0 で `NOT_RUN` と `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE` を表示し、`--strict` では PASS 以外を non-zero にします。

## v1.0.1候補整理

DiscordSRV token-configured runtime smoke は v1.0.1 bugfix ではなく、DiscordSRV を本番利用する前の運用確認候補です。smoke の結果から実装不具合が再現した場合のみ、bugfix intake と v1.0.1 candidates に具体的な再現条件を追記します。
