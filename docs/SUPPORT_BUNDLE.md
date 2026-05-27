# Support Bundle

`/rep support bundle` は support、review、incident investigation のために診断 ZIP を作成します。DB や server logs を共有せず、必要な状態だけを確認できるようにする機能です。

## 出力

- Directory: `plugins/ReputationBan/support/`
- File name: `reputationban-support-YYYYMMDD-HHMMSS.zip`
- Permission: `reputationban.admin.diagnostics`

## 含まれるもの

- `meta.txt`: generation time、plugin version、server version、Java version、共有用に丸めた plugin data folder placeholder。
- `doctor.txt`: `/rep doctor` 相当の health details。Webhook URLs は含みません。
- `counts.txt`: table row counts のみ。
- `config-redacted.yml`: sensitive keys を伏せた `config.yml`。
- `README-SHARING.txt`: 共有前の安全確認メモ。
- `plugin.yml` と `changelog-excerpt.txt`: 利用可能な場合に含まれます。

## 含まれないもの

bundle には次を含めない方針です。

- `reputationban.db`、`reputationban.db-wal`、`reputationban.db-shm`
- `latest.log` や `debug.log` などの server logs
- live `config.yml`
- Discord Webhook URLs、passwords、tokens、sessions、cookies、その他 secrets
- user home directories や server filesystem layouts など共有不要な absolute paths

## 共有前確認

共有前に `config-redacted.yml` を開き、Webhook URL や token が残っていないことを確認してください。`meta.txt` と `doctor.txt` は local absolute path の代わりに `<plugin-data-folder>` のような placeholder を使います。bundle は safe by default を目指していますが、tickets や review threads に投稿する前の目視確認は必須です。
