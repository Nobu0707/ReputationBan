# Security And Redaction

ReputationBan は Discord Webhook URLs をシークレットとして扱います。logs、command output、audit metadata、CSV exports、support bundles、release artifacts、review archives に実 URL を出してはいけません。

## Support Bundles

`/rep support bundle` は reviewers や support channels に共有するための診断 ZIP です。含まれるのは diagnostics と redacted config です。SQLite database files、WAL/SHM files、live server logs、live `config.yml` は含みません。

`meta.txt` と `doctor.txt` は plugin data directory に `<plugin-data-folder>` を使い、共有不要な absolute paths を避けます。

## config-redacted.yml

`config-redacted.yml` は通常の運用設定を読める状態にしつつ、sensitive key names を伏せます。`url`、`webhook`、`password`、`token`、`secret`、`session`、`cookie` を含む keys は `<redacted>` に置換されます。

## Free Text

`Redactor.redactSecretLikeValue` は URL-like values と、`token abc123`、`password is hunter2`、`sessionId abc`、`webhook VALUE`、`url VALUE` のような free-text secret patterns も redaction します。ドキュメントでは実 Webhook URL 風の例を追加せず、必要な場合は `<redacted>` や `<webhook-url>` を使います。

## Review Archives

`scripts/make-review-archive.sh` は `checks/secret-scan.txt` を作成し、具体的な Discord Webhook URL pattern をその出力内で redaction します。review archive には release verification output と最新の Paper runtime smoke summary も含まれます。

secret scan は検出結果の確認用です。説明文に `token` や `secret` という単語があるだけで即失敗ではありません。実値らしきものが出ていないかを確認してください。

## 共有前チェック

support bundle、release artifact、review archive を共有する前に次を確認してください。

- Discord Webhook URL が含まれていません。
- database files や log files が含まれていません。
- `config-redacted.yml` で secret-like settings が `<redacted>` になっています。
- local absolute paths が最小化され、必要に応じて placeholders に置換されています。
