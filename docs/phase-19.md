# Phase 19 / v0.19.0

Phase 19 は、GriefPrevention を任意連携として追加するフェーズです。LuckPerms、CoreProtect、WorldGuard と同じく、外部プラグインが無い環境でも ReputationBan 本体が起動できることを優先します。

## 目的

- v0.19.0 へ更新します。
- `plugin.yml` に `GriefPrevention` の `softdepend` を追加します。
- `/rep integrations`、`/rep integrations test`、`/rep doctor` に GriefPrevention 状態を表示します。
- `/reportbad` 後に GriefPrevention claim context を `report_context` provider `griefprevention` として保存します。
- `/reports view <id>` と `/reports evidence <id>` で GriefPrevention 文脈を表示します。
- `GRIEFPREVENTION_CONTEXT_CAPTURED` audit event を追加します。

## Reflection 方針

Java ソースでは GriefPrevention API 型を直接 import しません。`GriefPreventionReflectionAdapter` が `Class.forName` で次の候補を試し、`instance`、`dataStore`、`getClaimAt(Location, boolean, null)` を reflection で呼びます。

- `me.ryanhamshire.GriefPrevention.GriefPrevention`
- `me.ryanhamshire.griefprevention.GriefPrevention`

外へ出す値は ReputationBan 独自の `GriefPreventionClaimSummary` と `GriefPreventionClaimEntry` に変換します。

## report_context

保存 provider は `griefprevention` です。summary と metadata には claim 有無、world、block 座標、claim id、admin claim、設定に応じた owner、trust count、境界座標を含めます。

privacy のため `include-claim-owner` と `include-trust-counts` はデフォルト `false` です。claim context は審査補助であり、自動 BAN の唯一根拠にはしません。

## 禁止事項

- GriefPrevention claim の作成、変更、削除は行いません。
- GriefPrevention trust/permission の変更は行いません。
- WorldGuard region/flag、CoreProtect rollback/restore/purge、LuckPerms 権限変更も引き続き行いません。

## 検証

v0.19.0 の検証対象:

- `./gradlew clean test build --warning-mode all`
- `./scripts/check-docs-localization.sh`
- `./scripts/check-optional-dependency-safety.sh`
- `./scripts/review_code.sh`
- `./scripts/run-local-smoke-check.sh`
- `./scripts/create-release-artifact.sh`
- `./scripts/verify-release-artifact.sh`
- `./scripts/make-review-archive.sh "Phase 19"`
