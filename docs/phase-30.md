# Phase 30 / v1.0.0

Phase 30 は v1.0.0 tag / GitHub Release draft preparation フェーズです。新機能追加、DB schema 変更、v1.0.1 以降への version bump は行わず、Phase 29 の `1.0.0` final artifact を公開直前状態まで進めます。

## 目的

- Phase 29 の `ReputationBan-1.0.0.jar` と release artifact を再確認します。
- `v1.0.0` annotated tag を Phase 30 commit に作成し、GitHub へ push します。
- GitHub Release は draft として作成します。
- GitHub Release の公開、`draft=false` への変更、publish 操作はまだ行いません。
- Review archive に tag 状態、GitHub Release draft 状態、添付 asset 状態を含めます。

## 添付 artifact

GitHub Release draft には次を添付します。

- `build/release/ReputationBan-1.0.0.jar`
- `build/release/ReputationBan-1.0.0.jar.sha256`
- `build/release/ReputationBan-1.0.0-release.zip`
- `build/release/ReputationBan-1.0.0-release.zip.sha256`

## Runtime gates

- Paper runtime smoke: PASS
- Integration runtime smoke: PASS
- Player report/evidence runtime smoke: PASS, carried forward from `0.27.0`
- Runtime smoke consistency: PASS
- v1 release gates judgment: `READY_FOR_V1_RELEASE_WITH_DISCORDSRV_WARNING`

Player report/evidence runtime smoke の carry-forward 理由は、Phase 30 が docs/scripts/release/tag/draft preparation の更新に限定され、`/reportbad`、`/reports evidence`、`report_context` 生成、DB schema の runtime behavior を変更しないためです。

## DiscordSRV WARN

DiscordSRV は bot token 未設定または API unavailable の場合 WARN として扱います。DiscordSRV 通知はデフォルト無効であり、ReputationBan 本体、Paper runtime smoke、他の外部連携 runtime smoke の release gate は止めません。

本番で DiscordSRV 通知や account link を使う場合は、bot token 設定済み環境で `apiAvailable=true`、`active=true`、通知 channel 解決、account link context 表示を追加確認してください。

## Release draft 方針

`gh release create v1.0.0 --draft` は許可します。次の操作は禁止です。

- `gh release edit --draft=false`
- GitHub Release の publish
- 公開済み Release への変更
- `v1.0.1` 以降への version bump

`gh` が使えない、または認証されていない場合は `docs/V1_GITHUB_RELEASE_DRAFT_MANUAL.md` を作成し、tag、title、notes file、添付 asset、公開禁止を明記します。

## 次Phaseで行うこと

- GitHub Release draft の本文と添付 asset を最終確認します。
- DiscordSRV WARN を公開 notes で再確認します。
- ユーザーの明示承認後に GitHub Release を publish します。
- v1.0.1 以降の作業は publish 後に別 Phase として開始します。
