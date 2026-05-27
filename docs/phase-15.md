# Phase 15 / v0.15.0

Phase 15 は v1.0.0 直前のリリース候補確認フェーズです。Java 本体の機能追加、DB スキーマ変更、新コマンド追加ではなく、配布前に確認すべきドキュメント品質、release artifact、review archive、Paper 実機スモーク状態を明確にします。

## 目的

- v0.15.0 へ更新します。
- README.md と docs/*.md の日本語ドキュメント品質を確認します。
- command names、permission nodes、config keys、YAML examples、file names を翻訳で壊さないことを確認します。
- release artifact に必要な日本語 docs が入り、live `config.yml`、DB、logs が入らないことを検証します。
- `docs/RELEASE_CANDIDATE_CHECKLIST.md` で v1.0.0 前の判断材料をまとめます。
- Paper 実機スモークは手動実施が必要であり、未実施なら未実施として記録します。

## 翻訳しない識別子

次の識別子は日本語化しません。

- command names: `/rep version`、`/rep doctor`、`/rep support bundle`、`/rep backup`、`/reportbad`、`/reports`
- permission nodes: `reputationban.admin`、`reputationban.admin.audit`、`reputationban.admin.diagnostics`、`reputationban.admin.maintenance`、`reputationban.report`、`reputationban.notify`
- config keys: `initial-score`、`rating.min-unique-reports-before-deduction`、`notify.discord-webhook.url`、`retention`
- file names and paths: `plugin.yml`、`config.yml`、`README.md`、`CHANGELOG.md`、`docs/*.md`、`scripts/*.sh`

## Secret And Redaction

Discord Webhook URL はシークレットです。docs、logs、doctor output、audit metadata、CSV、support bundle、release artifact、review archive に実 URL 風の例を含めません。例示が必要な場合は `<redacted>` または `<webhook-url>` を使います。

## Release Artifact 検証

`scripts/verify-release-artifact.sh` は JAR と checksum だけでなく、README.md、CHANGELOG.md、主要 docs、日本語テキスト、禁止ファイル、実 Discord Webhook URL 風パターンを確認します。

## Paper 実機スモーク

Paper 実機スモークはローカル script だけでは代替できません。実 Paper サーバーで `/rep version`、`/rep doctor`、`/rep support bundle`、`/rep backup`、`/reportbad` TAB 補完を確認してください。

未実施の場合は PASS summary を作らず、review archive の `checks/latest-paper-runtime-smoke-summary.txt` に `status=NOT_RUN` と記録します。実施後は次のように記録します。

```bash
./scripts/record-paper-runtime-smoke-result.sh --result PASS --note "Paper runtime smoke passed"
```

## v0.15.0 の検証結果

- `./gradlew clean test build --warning-mode all`: 実施対象
- `./scripts/check-docs-localization.sh`: 実施対象
- `./scripts/review_code.sh`: 実施対象
- `./scripts/run-local-smoke-check.sh`: 実施対象
- `./scripts/create-release-artifact.sh`: 実施対象
- `./scripts/verify-release-artifact.sh`: 実施対象
- `./scripts/make-review-archive.sh "Phase 15"`: commit 後に作成
- Paper runtime smoke: 実 Paper サーバーで未実施なら `status=NOT_RUN`
