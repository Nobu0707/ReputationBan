# Phase 21 / v0.21.0

Phase 21 では DiscordSRV を任意連携として追加しました。目的は、DiscordSRV の account link 状態を通報審査の補助文脈として保存し、必要な場合だけ DiscordSRV 経由のスタッフ通知を使えるようにすることです。

## 任意連携方針

- `plugin.yml` に `softdepend: DiscordSRV` を追加しました。
- Java ソースでは DiscordSRV API と JDA API を直接 import しません。
- `DiscordSrvReflectionAdapter` が `github.scarsz.discordsrv.DiscordSRV`、`getPlugin()`、`getAccountLinkManager()`、`getDiscordId(UUID)` を reflection で候補 lookup します。
- 通知は `getDestinationTextChannelForGameChannelName(channel)`、`sendMessage(String)`、`queue()` を reflection で呼びます。
- DiscordSRV compileOnly 依存は採用していません。Phase 21 は reflection only です。

## report_context

`/reportbad` 成功後、対象 category が `integrations.discordsrv.account-link-context.categories` に含まれる場合、provider `discordsrv` として account link summary を保存します。

`include-discord-ids` はデフォルト `false` です。false の場合、保存するのは `reporterLinked`、`targetLinked`、`includeDiscordIds=false` だけです。true にした場合のみ Discord ID を metadata に含めます。Discord ID はシークレットではありませんが個人情報に近いため、必要な場合だけ有効化してください。

`DISCORDSRV_CONTEXT_CAPTURED` audit event を追加しました。audit metadata には `reporterLinked`、`targetLinked`、`includeDiscordIds`、`category` を保存し、デフォルトでは Discord ID を含めません。

## Commands

- `/rep integrations`: DiscordSRV の configuredEnabled、pluginPresent、apiAvailable、active、accountLinkContextEnabled、includeDiscordIds、notificationsEnabled、notificationChannel を表示します。
- `/rep integrations test`: account link API availability と player sender の linked 状態を表示します。Discord へ test message は送信しません。
- `/rep doctor`: DiscordSRV integration を `active`、`unavailable`、`disabled` の簡易状態で表示します。
- `/reports view <id>` と `/reports evidence <id>`: provider `discordsrv` の summary を DiscordSRV 欄として表示します。

## Notifications

DiscordSRV 通知は `integrations.discordsrv.notifications.enabled` が `true` の場合だけ送信します。既存 Discord Webhook と二重通知になり得るため、デフォルトは無効です。

Phase 21 では NotificationEventType の event toggle 経由で `report-created`、`report-approved`、`report-rejected`、`auto-ban`、`unban`、`pardon`、`reporter-penalty` に対応しています。最低限の実装対象だった `report-created` と `auto-ban` は既存通知フローから送信されます。

`include-reasons=false` の場合、理由・メモ行を DiscordSRV 通知から除外します。長文は truncate します。

## 禁止事項

Phase 21 では以下を実装していません。

- Discord から Minecraft コマンドを実行する機能
- Discord ボタンやコンポーネントによる承認・却下
- Discord role の付与・剥奪
- DiscordSRV の設定変更やチャンネル設定変更
- DiscordSRV account link context を自動 BAN の唯一根拠にする処理

既存の安全方針として、CoreProtect rollback / restore / purge、LuckPerms 書き込み、WorldGuard region/flag 変更、GriefPrevention claim/trust 変更も引き続き行いません。

## 検証

v0.21.0 の検証対象は以下です。

- `./gradlew clean test build --warning-mode all`
- `./scripts/check-docs-localization.sh`
- `./scripts/check-optional-dependency-safety.sh`
- `./scripts/review_code.sh`
- `./scripts/run-local-smoke-check.sh`
- `./scripts/create-release-artifact.sh`
- `./scripts/verify-release-artifact.sh`
- `./scripts/make-review-archive.sh "Phase 21"`
