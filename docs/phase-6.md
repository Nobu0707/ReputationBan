# Phase 6 / v0.6.0

Phase 6 adds Discord Webhook notifications while keeping webhook URLs secret.

## Changes

- Bumped plugin version to `0.6.0`.
- Adds `notify.discord-webhook` section config with per-event toggles.
- Keeps compatibility with the old boolean `notify.discord-webhook` setting.
- Adds `NotificationService`, `DiscordWebhookClient`, and `NotificationEventType`.
- Sends Discord notifications through Java `HttpClient#sendAsync`.
- Adds JSON escaping and Discord content truncation.
- Rate-limits Discord delivery failure logs and never logs the webhook URL.
- Adds notifications for report creation, report approval/rejection, auto-ban, unban, pardon, reporter penalty, and optional recovery summaries.
- Adds JUnit coverage for payload escaping, truncation, event keys, and webhook config behavior.

## Validation

- `./gradlew clean test build --warning-mode all`
- `./scripts/review_code.sh`
- `./scripts/make-review-archive.sh "Phase 6"` after the Phase 6 commit
