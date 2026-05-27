# Phase 18 / v0.18.0

Phase 18 は、WorldGuard を任意連携として追加するフェーズです。LuckPerms / CoreProtect と同じく、外部プラグインが無い環境でも ReputationBan 本体が起動できることを重視します。

## 目的

- v0.18.0 へ更新します。
- `plugin.yml` に `WorldEdit` / `WorldGuard` の `softdepend` を追加します。
- EngineHub repository と `compileOnly` dependency を追加します。
- `/rep integrations`、`/rep integrations test`、`/rep doctor` に WorldGuard 状態を表示します。
- `/reportbad` 後に WorldGuard region context を `report_context` provider `worldguard` として保存します。
- `/reports view <id>` と `/reports evidence <id>` で WorldGuard 文脈を表示します。
- `WORLDGUARD_CONTEXT_CAPTURED` audit event を追加します。

## Optional Safe 方針

Java ソースでは `com.sk89q.worldguard.*` と `com.sk89q.worldedit.*` を直接 import しません。WorldGuard / WorldEdit API への接触は `WorldGuardReflectionAdapter` に閉じ込め、外へ出す値は ReputationBan 独自の `WorldGuardRegionSummary` と `WorldGuardRegionEntry` に変換します。

採用 dependency:

- WorldGuard API: `com.sk89q.worldguard:worldguard-bukkit:7.0.13`
- WorldEdit API: `com.sk89q.worldedit:worldedit-bukkit:7.3.0`
- EngineHub repository: `https://maven.enginehub.org/repo/`

Paper API と WorldGuard / WorldEdit の推移依存 constraint が衝突するため、WorldGuard / WorldEdit は `compileOnly` かつ transitive dependencies disabled としています。ReputationBan 本体は reflection only なので、外部 API 型は compile classpath の確認用に留めています。

## Report Context

`integrations.worldguard.report-context.categories` に含まれる category の `/reportbad` で、通報者の現在地に適用される region context を保存します。metadata には `regionCount`、`world`、`x`、`y`、`z`、`maxRegions` を含めます。flag は region に直接設定されていて、設定の `include-flags` に一致するものだけ保存します。

region が 0 件の場合も、`WorldGuard: regions 0 ...` の summary を保存する設計です。これは「保護領域外だった」ことも審査補助になるためです。

owner/member は privacy のためデフォルト `hidden` です。設定で有効にした場合も Phase 18 では count 形式に留めます。

## 禁止事項

- WorldGuard region の作成、変更、削除は行いません。
- WorldGuard flag の変更は行いません。
- WorldGuard context を自動 BAN の唯一根拠にしません。
- CoreProtect rollback、restore、purge は行いません。
- LuckPerms group/permission 書き込みは行いません。

## 検証

Phase 18 の検証対象:

- `./gradlew clean test build --warning-mode all`
- `./scripts/check-docs-localization.sh`
- `./scripts/check-optional-dependency-safety.sh`
- `./scripts/review_code.sh`
- `./scripts/run-local-smoke-check.sh`
- `./scripts/create-release-artifact.sh`
- `./scripts/verify-release-artifact.sh`
- `./scripts/make-review-archive.sh "Phase 18"`

Paper runtime smoke と Integration runtime smoke は手動確認です。未実施の場合は review archive に `status=NOT_RUN` として残します。
