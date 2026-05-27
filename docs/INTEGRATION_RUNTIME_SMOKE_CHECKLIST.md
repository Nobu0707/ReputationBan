# Integration Runtime Smoke Checklist

Phase 17 の LuckPerms / CoreProtect 連携を実サーバーで確認するための手順です。未実施でも v0.17.0 の local release checks は失敗扱いにしませんが、v1.0.0 前には実施してください。

## 共通確認

- PaperMC 26.1.2 と Java 25 で起動します。
- `build/libs/ReputationBan-0.17.0.jar` を配置します。
- CoreProtect rollback、restore、purge は使いません。
- LuckPerms の group 変更、権限付与、権限剥奪、`saveUser` などの書き込みは行いません。

## LuckPerms 未導入

- `/rep integrations` で LuckPerms の `pluginPresent=false` を確認します。
- `/rep integrations test` で LuckPerms API unavailable が安全に表示されることを確認します。
- `/rep doctor` が失敗しないことを確認します。

## CoreProtect 未導入

- `/rep integrations` で CoreProtect の `pluginPresent=false` を確認します。
- `/rep integrations test` で CoreProtect API unavailable が安全に表示されることを確認します。
- `/reportbad griefing` 相当の通報が CoreProtect なしでも成功することを確認します。

## LuckPerms のみ導入

- `/rep integrations` で LuckPerms の `pluginPresent=true` と `apiAvailable=true` を確認します。
- player 実行の `/rep integrations test` で primary group、reporterWeight、bypassGroup が表示されることを確認します。
- console 実行の `/rep integrations test` で `LuckPerms user test: console sender, skipped` が表示されることを確認します。

## CoreProtect のみ導入

- `/rep integrations` で CoreProtect の API version、minimumApiVersion、lookupSeconds、radius、maxResults を確認します。
- `/rep integrations test` で実 lookup が skipped と表示されることを確認します。
- griefing report 後、`/reports evidence <id>` で CoreProtect summary または「保存された連携情報なし」が安全に表示されることを確認します。

## 両方導入

- `/rep integrations`
- `/rep integrations test`
- `/rep doctor`
- `/reportbad <player> griefing <reason>`
- `/reports evidence <id>`

上記を順番に確認し、通報処理が外部連携の有無に依存して失敗しないことを確認します。

## 結果記録

```bash
./scripts/record-integration-runtime-smoke-result.sh --result PASS --scenario "LuckPerms+CoreProtect" --note "manual smoke passed"
```

結果は `build/manual-smoke/integration-runtime-YYYYMMDD-HHMMSS/summary.txt` に保存され、review archive が最新 summary を収集します。
