# DiscordSRV Configured Runtime Smoke Checklist

## 目的

DiscordSRV bot token 設定済み環境で、ReputationBan の DiscordSRV 連携が安全に動くことを確認します。

## 事前注意

- bot token は docs、logs、review archive、support bundle に出しません。
- token 値を command output、issue、chat、review comment に貼りません。
- DiscordSRV 通知はデフォルト無効です。
- テスト用 Discord チャンネルを使います。
- 本番 Discord へ通知する場合は、事前に関係者へ告知します。
- 未設定環境を PASS 扱いにせず、`NOT_RUN` / `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE` として記録します。

## 前提

- Paper runtime smoke が PASS している。
- Integration runtime smoke が PASS している。
- DiscordSRV plugin が installed である。
- DiscordSRV bot token が configured である。
- DiscordSRV bot が online である。
- `integrations.discordsrv.enabled=true` である。
- 通知テストする場合のみ `integrations.discordsrv.notifications.enabled=true` である。

## 確認項目

1. server を起動する。
2. `/rep integrations` を実行する。
3. `/rep integrations test` を実行する。
4. `/rep doctor` を実行する。
5. DiscordSRV の `apiAvailable=true` を確認する。
6. DiscordSRV の `active=true` を確認する。
7. account link 済み player で `/reportbad` を実行する。
8. `/reports evidence <id>` に DiscordSRV context が表示されることを確認する。
9. `include-discord-ids=false` の場合 ID が hidden になることを確認する。
10. `include-discord-ids=true` の場合、意図した場合のみ ID が出ることを確認する。
11. 通知有効時、`report-created` 通知が指定チャンネルに出ることを確認する。
12. 通知無効時、DiscordSRV 経由通知が出ないことを確認する。
13. Discord から Minecraft command が実行されないことを確認する。
14. Discord role 変更が行われないことを確認する。
15. `NoClassDefFoundError` / `ClassNotFoundException` がないことを確認する。
16. bot token や Webhook URL が logs に出ないことを確認する。

## PASS条件

- DiscordSRV `active=true` である。
- `/rep integrations test` が error なしで完了する。
- `/reports evidence <id>` で DiscordSRV context が確認できる。
- 通知が設定どおりに出る、または出ない。
- secret 漏えいがない。

## FAIL条件

- ReputationBan が起動に失敗する。
- DiscordSRV 連携で例外が出る。
- token が露出する。
- 意図しない Discord command 実行または role 変更が起きる。
