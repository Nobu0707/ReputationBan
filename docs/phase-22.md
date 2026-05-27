# Phase 22 / v0.22.0

Phase 22 は、新しい外部連携を追加せず、Phase 16 から Phase 21 で追加した外部連携の runtime 安全性と実機 smoke readiness を強化するフェーズです。

## 目的

- DiscordSRV reflection adapter を、DiscordSRV API class が ReputationBan 側 classloader から見えない場合にも動きやすくします。
- DiscordSRV 通知で Bukkit / DiscordSRV API に触る処理を main thread task 内へ寄せます。
- integration runtime smoke 未実施を PASS 扱いせず、HOLD として明示します。
- v1.0.0 前に必要な optional plugin 実機確認条件を docs と scripts に残します。

## DiscordSRV Reflection Adapter

`DiscordSrvReflectionAdapter` は、まず Bukkit PluginManager の `getPlugin("DiscordSRV")` で得た plugin instance を使います。そこから `getAccountLinkManager` と `getDestinationTextChannelForGameChannelName` を reflection で探し、取得できない場合だけ `Class.forName("github.scarsz.discordsrv.DiscordSRV")` と static `getPlugin()` route を fallback として試します。

DiscordSRV / JDA の直接 import は追加していません。plugin missing、manager missing、channel missing、Discord ID null は unavailable または empty として扱い、ReputationBan 本体処理を失敗させません。

## DiscordSRV Notification Thread Boundary

`DiscordSrvNotificationService#send` は呼び出し元 thread を問わず使えるよう、外側では notifications enabled と event toggle の軽い config 判定だけを行います。`integration.detail(config)`、`shouldNotify` の active 判定、message sanitize、`sendMessage` は `plugin.getServer().getScheduler().runTask(plugin, ...)` 内で実行します。

既存の Discord Webhook 通知は Java HttpClient の async 送信であり、このフェーズでは変更していません。

## Integration Runtime Readiness

`scripts/check-integration-runtime-readiness.sh` を追加しました。通常モードでは最新の `build/manual-smoke/integration-runtime-*/summary.txt` を読み、summary がない場合も exit 0 で以下を表示します。

```text
integration runtime smoke: NOT_RUN
judgment: HOLD_FOR_INTEGRATION_RUNTIME_SMOKE
```

`--strict` では `result=PASS` 以外を non-zero として扱います。CI や v1.0.0 直前の gate では strict mode を使ってください。

## Integration Runtime Smoke Helper

`scripts/run-integration-runtime-smoke-helper.sh` を追加しました。`REPUTATIONBAN_PAPER_DIR`、`REPUTATIONBAN_PAPER_JAR`、`REPUTATIONBAN_JAVA_BIN` が未設定の場合は、JAR copy 手順、optional plugin 組み合わせ、確認コマンド、DiscordSRV 確認項目、結果記録コマンドを表示して exit 0 します。

helper は server directory、SQLite DB、logs、optional plugin data を削除しません。

## v0.22.0 検証結果

- `./gradlew clean test build --warning-mode all`
- `./scripts/check-docs-localization.sh`
- `./scripts/check-optional-dependency-safety.sh`
- `./scripts/check-integration-runtime-readiness.sh`
- `bash -n scripts/run-integration-runtime-smoke-helper.sh`
- `./scripts/review_code.sh`
- `./scripts/run-local-smoke-check.sh`
- `./scripts/create-release-artifact.sh`
- `./scripts/verify-release-artifact.sh`
- `./scripts/make-review-archive.sh "Phase 22"`

Paper runtime smoke と integration runtime smoke は、実 Paper server で実施した場合のみ PASS として記録します。未実施の場合は `NOT_RUN` / `HOLD_FOR_INTEGRATION_RUNTIME_SMOKE` のまま残します。
