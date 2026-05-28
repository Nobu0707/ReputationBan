# Phase 31 / v1.0.0

Phase 31 は GitHub Release draft verification / publish フェーズです。新機能追加、DB schema 変更、version bump、tag 作り直し、asset 差し替えは行わず、Phase 30 で作成した `v1.0.0` GitHub Release draft を確認して公開しました。

## 目的

- Git working tree が clean であることを確認します。
- `v1.0.0` tag が local/remote に存在し、Phase 30 commit を指すことを確認します。
- GitHub Release draft が `tagName=v1.0.0`、`isDraft=true`、`isPrerelease=false`、必須 asset 4件を持つことを確認します。
- JAR / release ZIP の SHA256 と内容を確認します。
- v1 release gates が READY であることを確認します。
- GitHub Release を公開し、公開後に `isDraft=false` を確認します。
- 公開結果を docs と review archive に記録します。

## Tag / Release

- `v1.0.0` tag object: `e2a0b89897db12b50716b5ea11fe49ada687a5c8`
- `v1.0.0` tag commit: `b422e72ec5a917cdc04dee902e96a0cef190026c`
- GitHub Release: published
- Release URL: <https://github.com/Nobu0707/ReputationBan/releases/tag/v1.0.0>
- `isDraft=false`
- `isPrerelease=false`
- Published at: `2026-05-28T12:00:28+09:00` (`2026-05-28T03:00:28+00:00`)

## Assets

GitHub Release `v1.0.0` には次の asset 4件があります。

- `ReputationBan-1.0.0.jar`
- `ReputationBan-1.0.0.jar.sha256`
- `ReputationBan-1.0.0-release.zip`
- `ReputationBan-1.0.0-release.zip.sha256`

SHA256:

- JAR: `6a693f35852c122a6a054193bdafb8529b91b081ba4b97a7b260e9ec825b0443`
- Release ZIP: `b660e03d4e721f27e1645a1b747f30b208f844322de40ed2b9fa86e23b51d797`

## Runtime Gates

- Paper runtime smoke: PASS
- Integration runtime smoke: PASS
- Player report/evidence runtime smoke: PASS, carried forward from `0.27.0`
- Runtime smoke consistency: PASS
- v1 release gates judgment: `READY_FOR_V1_RELEASE_WITH_DISCORDSRV_WARNING`
- `scripts/review_code.sh`: PASS

## DiscordSRV WARN

DiscordSRV は bot token 未設定または API unavailable の場合 WARN として扱います。DiscordSRV 通知はデフォルト無効であり、ReputationBan 本体、Paper runtime smoke、他の外部連携 runtime smoke の release gate は止めません。

本番で DiscordSRV 通知や account link を使う場合は、bot token 設定済み環境で `apiAvailable=true`、`active=true`、通知 channel 解決、account link context 表示を追加確認してください。

## Phase 31 禁止事項

Phase 31 では次を行っていません。

- version 変更
- tag の作り直し
- tag の force push
- asset の差し替え
- 新機能追加
- DB schema 変更
- GitHub Release 削除
