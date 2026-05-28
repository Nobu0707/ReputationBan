# Phase 31a / v1.0.0 Published Release Notes Consistency

Phase 31a は公開済み `v1.0.0` GitHub Release 本文と生成レポートの整合修正フェーズです。tag、assets、version、DB schema、runtime behavior は変更しません。

## 目的

- 公開済み GitHub Release 本文から公開前の draft 文言を取り除きます。
- `scripts/generate-v1-release-notes.sh` の出力を `GitHub Release status: PUBLISHED` に揃えます。
- `scripts/generate-v1-go-no-go-report.sh` の出力を `GitHub Release status: PUBLISHED`、`Judgment: RELEASED_WITH_DISCORDSRV_WARNING` に揃えます。
- review archive に release notes body stale text check を含めます。

## Release

- Release URL: <https://github.com/Nobu0707/ReputationBan/releases/tag/v1.0.0>
- GitHub Release: published
- `isDraft=false`
- `isPrerelease=false`
- `v1.0.0` tag commit: `b422e72ec5a917cdc04dee902e96a0cef190026c`
- Tag status: CREATED
- GitHub Release status: PUBLISHED
- Next action: Post-release monitoring / bugfix intake

## Assets

GitHub Release `v1.0.0` の asset は変更しません。

- `ReputationBan-1.0.0.jar`
- `ReputationBan-1.0.0.jar.sha256`
- `ReputationBan-1.0.0-release.zip`
- `ReputationBan-1.0.0-release.zip.sha256`

## DiscordSRV WARN

DiscordSRV は bot token 未設定または API unavailable の場合 WARN として扱います。DiscordSRV 通知はデフォルト無効であり、ReputationBan 本体、Paper runtime smoke、他の外部連携 runtime smoke の release gate は止めません。

## Phase 31a 禁止事項

Phase 31a では次を行いません。

- `v1.0.0` tag の作り直し
- `v1.0.0` tag の force push
- GitHub Release の削除
- GitHub Release asset の削除
- GitHub Release asset の差し替え
- version 変更
- DB schema 変更
- 新機能追加
