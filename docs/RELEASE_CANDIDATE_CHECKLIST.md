# v0.15.0 リリース候補チェックリスト

v1.0.0 へ進む前に、v0.15.0 の release candidate として次を確認します。

## Build / Test / Review

- `./gradlew clean test build --warning-mode all` が成功します。
- `./scripts/review_code.sh` が成功します。
- `./scripts/run-local-smoke-check.sh` が成功します。
- JAR 名が `ReputationBan-0.15.0.jar` です。

## Docs Localization

- `./scripts/check-docs-localization.sh` が成功します。
- README.md と主要 docs に日本語説明があります。
- `/rep version`、`/reportbad`、`reputationban.admin`、`initial-score`、`notify.discord-webhook`、`retention` などの識別子が翻訳で壊れていません。

## Release Artifact Verification

- `./scripts/create-release-artifact.sh` が成功します。
- `./scripts/verify-release-artifact.sh` が成功します。
- release ZIP に README.md、CHANGELOG.md、主要 docs、JAR、checksum が含まれます。
- release ZIP に live `config.yml`、`reputationban.db`、WAL/SHM、logs は含まれません。

## Secret Scan

- docs と release ZIP に実 Discord Webhook URL 風の文字列がありません。
- Webhook URL values は logs、doctor output、audit metadata、CSV、support bundle、review archive に出ません。

## Support Bundle Safety

- `/rep support bundle` は `config-redacted.yml` を含みます。
- support bundle に DB files、server logs、live `config.yml`、Webhook URL が含まれません。
- 共有前に `<redacted>` の状態を確認します。

## Paper Runtime Smoke

- 可能な限り実 Paper 26.1.2 サーバーと Java 25 で確認します。
- `/rep version`、`/rep doctor`、`/rep support bundle`、`/rep backup`、`/reportbad` TAB 補完を確認します。
- 実施後は `./scripts/record-paper-runtime-smoke-result.sh --result PASS --note "Paper runtime smoke passed"` で記録します。
- 未実施を PASS 扱いにしません。未実施の場合は review archive の summary が `status=NOT_RUN` になります。

## Backup / Restore 注意

- `/rep maintenance run confirm` 前に backup が作成されることを確認します。
- `/rep backup before-release` の生成物を確認します。
- rollback 手順では server を止め、現在の DB を退避してから backup を復元します。

## v1.0.0 へ進む前の判断

- build/test/review_code/local smoke/release verification がすべて成功しています。
- Paper runtime smoke の PASS または未実施理由が明確です。
- 既知の未解決事項が v1.0.0 を妨げないことを確認します。
