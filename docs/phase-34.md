# Phase 34

Phase 34 は v1.0.0 公開後の docs/scripts-only phase です。version bump、`v1.0.1` tag 作成、GitHub Release asset 差し替え、GitHub Release 削除、`v1.0.0` tag 移動は行いません。

## 目的

- v1.0.0 GitHub Release が公開済みであることを再確認する。
- DiscordSRV token-configured runtime smoke を実施できる環境か判定する。
- 実施できない場合は `NOT_RUN` / `DEFERRED` 相当として理由を記録し、PASS 扱いにしない。
- v1.0.1 candidates と post-release monitoring に DiscordSRV configured smoke の扱いを反映する。
- review archive に DiscordSRV configured smoke の判断結果を含める。

## DiscordSRV configured smokeの実施判断

Phase 34 時点では DiscordSRV bot token configured 環境、bot online 状態、テスト用 Discord チャンネル、本番利用予定または確認意図が提供されていないため、token-configured smoke は実施していません。

記録結果:

- status: `NOT_RUN`
- result: `NOT_RUN`
- scenario: `DiscordSRV configured smoke deferred`
- judgment: `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE`
- reason: token-configured environment or production-use decision not provided

未設定環境や本番利用予定が未確定の環境は PASS 扱いにしません。DiscordSRV を本番で使う場合は、`docs/DISCORDSRV_CONFIGURED_RUNTIME_SMOKE_CHECKLIST.md` に沿って別途 token-configured smoke を実施します。

## Tokenを記録しない方針

Discord bot token、Webhook URL、secret、session、cookie、password は docs、logs、review archive、support bundle に出しません。Codex は bot token を要求せず、結果記録にも実値を含めません。

## v1.0.1候補整理

DiscordSRV token-configured runtime smoke の未実施は v1.0.1 bugfix ではなく、運用確認候補です。smoke 実施時に実装不具合が再現した場合のみ、再現条件、影響範囲、回避策を `docs/V1_0_1_CANDIDATES.md` へ追記し、v1.0.1 candidate に昇格します。

## v1.0.0から変更しないもの

- version は `1.0.0` のままです。
- `v1.0.0` tag は Phase 30 commit `b422e72ec5a917cdc04dee902e96a0cef190026c` を指したままです。
- GitHub Release `v1.0.0` は published のままです。
- GitHub Release assets は差し替えません。
- DB schema、runtime behavior、新機能は変更しません。
