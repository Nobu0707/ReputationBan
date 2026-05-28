# Contributing

ReputationBan は PaperMC 26.1.2 / Java 25 向けの Spigot/Paper plugin です。v1.0.x は公開済み v1.0.0 の保守フェーズなので、変更は bugfix、docs、tests、maintenance を中心に小さく安全に進めます。

## 開発環境

- Java 25
- PaperMC 26.1.2
- Gradle Kotlin DSL
- SQLite

## テスト

PR 前に可能な範囲で次を実行してください。

```bash
./gradlew clean test build --warning-mode all
./scripts/check-docs-localization.sh
./scripts/check-optional-dependency-safety.sh
./scripts/check-paper-runtime-readiness.sh
./scripts/check-integration-runtime-readiness.sh
./scripts/check-discordsrv-runtime-readiness.sh
./scripts/check-player-report-runtime-readiness.sh
./scripts/check-runtime-smoke-consistency.sh
./scripts/check-v1-release-gates.sh
./scripts/review_code.sh
```

runtime smoke は実機環境がない場合、`NOT_RUN` / `HOLD` を正直に記録してください。未実施を PASS 扱いしないでください。

## optional dependency safety

- LuckPerms、CoreProtect、WorldGuard、GriefPrevention、PlaceholderAPI、DiscordSRV は任意連携です。
- 外部API型の直接 import は、既存の隔離クラスや reflection adapter の方針に合わせてください。
- 任意プラグインが未導入でも ReputationBan 本体が起動できることを維持してください。
- `./scripts/check-optional-dependency-safety.sh` を通してください。

## 破壊的操作禁止

次の操作は追加しません。

- CoreProtect rollback / restore / purge
- LuckPerms user data / permission の書き込み
- WorldGuard region / flag の作成、変更、削除
- GriefPrevention claim / trust の作成、変更、削除
- Discord から Minecraft command を実行する機能
- Discord role 変更

## secret safety

Discord bot token、Discord Webhook URL、password、session、cookie、secret は log、audit、CSV、support bundle、review archive、docs に出さないでください。Issue や PR でも実値を貼らないでください。

## PR前チェック

- 変更概要と変更種別を書いてください。
- DB migration の有無を書いてください。
- runtime smoke への影響を書いてください。
- optional dependency safety と secret scan を確認してください。
- v1.0.0 tag、GitHub Release、Release assets を変更しないでください。

## version方針

Phase 35 では version bump しません。v1.0.x は bugfix 中心、機能追加は原則 v1.1.0 以降の候補として扱います。
