# Phase 32 / v1.0.0 Post-release Monitoring

Phase 32 は ReputationBan v1.0.0 公開後監視、bugfix intake、v1.0.1 候補整理のフェーズです。新機能追加、version bump、`v1.0.1` tag 作成、GitHub Release asset 差し替え、DB schema 変更は行いません。

## 目的

- v1.0.0 公開後の GitHub Release 状態を再確認します。
- Release URL、asset 4件、sha256 artifact、release notes 本文を再確認します。
- 公開済み release notes に古い draft 文言が残っていないことを確認します。
- post-release monitoring docs を追加します。
- bugfix intake checklist を追加します。
- v1.0.1 candidates の扱いを明文化します。
- runtime smoke と release gate の最終状態を記録します。
- review archive に Phase 32 の確認結果を含めます。

## 公開状態

- GitHub Release URL: <https://github.com/Nobu0707/ReputationBan/releases/tag/v1.0.0>
- GitHub Release status: `PUBLISHED`
- `isDraft=false`
- `isPrerelease=false`
- `v1.0.0` commit: `b422e72ec5a917cdc04dee902e96a0cef190026c`
- version: `1.0.0`

## 追加ドキュメント

- `docs/POST_RELEASE_MONITORING.md`
- `docs/BUGFIX_INTAKE.md`
- `docs/V1_0_1_CANDIDATES.md`
- `docs/phase-32.md`

## v1.0.0は変更しない

Phase 32 では `v1.0.0` tag、GitHub Release assets、release zip、JAR、DB schema、runtime behavior を変更しません。必要な変更は docs-only と post-release intake の記録に限定します。
