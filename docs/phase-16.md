# Phase 16 / v0.16.0

Phase 16 は LuckPerms / CoreProtect 任意連携の土台フェーズです。以前の v1.0.0 readiness gate は後続に回し、外部プラグイン連携の安全な最小実装に限定します。

## 目的

- LuckPerms と CoreProtect を `softdepend` として扱います。
- 未導入、未ロード、API unavailable の場合でも ReputationBan 単体で起動し、通報、BAN、監査、support bundle が壊れないようにします。
- `/rep integrations` と `/rep doctor` で連携状態を確認できるようにします。
- LuckPerms primary group から reporter weight を計算し、通報 context と audit metadata に記録します。
- LuckPerms bypass-groups を既存の `reputationban.bypass` と OP 保護に加える補助保護として扱います。
- CoreProtect の周辺ブロックログを短い証拠サマリーとして `report_context` に保存します。

## 任意依存

- LuckPerms API: `net.luckperms:api:5.5`
- CoreProtect API: `net.coreprotect:coreprotect:23.2`
- CoreProtect Maven repository: `https://maven.playpro.com/`
- CoreProtect minimum API version: `11`

CoreProtect の Maven 座標は公式 README の dependency information を確認し、`net.coreprotect:coreprotect:23.2` を採用しました。

## 禁止事項

Phase 16 では以下を実装しません。

- CoreProtect rollback / restore / purge
- LuckPerms group / permission 書き込み
- WorldGuard 連携
- GriefPrevention 連携
- DiscordSRV 連携
- GUI
- Folia 対応
- v1.0.0 readiness gate

## 検証結果

- `./gradlew test --warning-mode all`: 成功
- `./gradlew clean test build --warning-mode all`: 最終確認で実行
- `./scripts/review_code.sh`: 最終確認で実行
- `./scripts/run-local-smoke-check.sh`: 最終確認で実行
- `./scripts/create-release-artifact.sh`: 最終確認で実行
- `./scripts/verify-release-artifact.sh`: 最終確認で実行
- `./scripts/make-review-archive.sh "Phase 16"`: commit 後に作成

Paper 実機 runtime smoke は環境依存の手動確認です。未実施の場合は PASS 扱いにせず、review archive の `latest-paper-runtime-smoke-summary.txt` に `status=NOT_RUN` として残します。
