# Phase 17 / v0.17.0

Phase 17 は、Phase 16 で追加した LuckPerms / CoreProtect 連携を運用で確認しやすくするフェーズです。

## 目的

- v0.17.0 へ更新します。
- `/reports evidence <id>` で `report_context` の詳細を表示します。
- `/rep integrations test` で LuckPerms / CoreProtect 連携だけに絞った診断を実行します。
- `/rep integrations` の表示を詳細化します。
- optional dependency safety check を review flow に組み込みます。
- Integration runtime smoke checklist と結果記録スクリプトを追加します。

## `/reports evidence`

`reputationban.admin.reports` 権限で、report に保存された LuckPerms / CoreProtect 証拠情報を provider ごとに表示します。保存情報が無い場合は「この通報に保存された連携情報はありません。」と表示します。

## `/rep integrations test`

`reputationban.admin.integrations` 権限で、LuckPerms plugin presence、API availability、player 実行時の primary group / reporterWeight / bypassGroup、CoreProtect API version、minimum API version、report context 設定を表示します。Phase 17 では CoreProtect の実 lookup はデフォルトでは行いません。

`/rep doctor` は ReputationBan 全体診断、`/rep integrations test` は外部連携だけの詳細診断です。

## Safety

- `src/main/java` に `import net.luckperms.` と `import net.coreprotect.` を追加しません。
- 外部 API 型は reflection adapter に閉じ込めます。
- CoreProtect rollback、restore、purge は行いません。
- LuckPerms の group 変更、権限変更、`saveUser` などの書き込みは行いません。

## 検証

Phase 17 の検証対象:

- `./gradlew clean test build --warning-mode all`
- `./scripts/check-docs-localization.sh`
- `./scripts/check-optional-dependency-safety.sh`
- `./scripts/review_code.sh`
- `./scripts/run-local-smoke-check.sh`
- `./scripts/create-release-artifact.sh`
- `./scripts/verify-release-artifact.sh`
- `./scripts/make-review-archive.sh "Phase 17"`

Paper runtime smoke と Integration runtime smoke は手動確認です。未実施の場合は review archive に `status=NOT_RUN` として残します。
