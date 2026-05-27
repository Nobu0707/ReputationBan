# Phase 6 / v0.6.0

Phase 6 では、Discord Webhook notifications を追加しつつ Webhook URL の秘匿を維持しました。

## 主な変更

- plugin version を `0.6.0` に更新しました。
- `notify.discord-webhook` section と event-level toggles を追加しました。
- 古い boolean 形式の `notify.discord-webhook` との互換性を維持しました。
- `NotificationService`、`DiscordWebhookClient`、`NotificationEventType` を追加しました。
- Java `HttpClient#sendAsync` で非同期送信します。
- report creation、approval/rejection、auto-ban、unban、pardon、reporter penalty、recovery summary の通知を追加しました。
- JSON escaping、Discord content truncation、failure log rate limiting を追加しました。
- Webhook URL は logs に出しません。
- payload escaping、truncation、event keys、webhook config の JUnit coverage を追加しました。

## 検証

- `./gradlew clean test build --warning-mode all`
- `./scripts/review_code.sh`
- `./scripts/make-review-archive.sh "Phase 6"`
