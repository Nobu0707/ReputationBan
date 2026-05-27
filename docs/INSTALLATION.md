# Installation

ReputationBan 0.22.0 は PaperMC 26.1.2 と Java 25 を対象にしています。

## 導入条件

- PaperMC 26.1.2 の test server または production server
- Java 25 runtime
- `build/libs/ReputationBan-0.22.0.jar`
- SQLite JDBC は `plugin.yml` の `libraries` から読み込まれます。

## 初回導入

1. Paper server を停止します。
2. `ReputationBan-0.22.0.jar` を server の `plugins/` ディレクトリへ配置します。
3. server を一度起動し、`plugins/ReputationBan/config.yml` を生成します。
4. `plugins/ReputationBan/reputationban.db` が生成されていることを確認します。
5. `config.yml` を確認してから server を再起動します。
6. `/rep version` を実行します。
7. `/rep doctor` を実行します。

Discord Webhook 通知はデフォルトで無効です。あとから有効化する場合も、Webhook URL はシークレットとして扱い、tickets、logs、review archives、screenshots へ貼らないでください。

## アップデート時の注意

- 更新前に `plugins/ReputationBan/reputationban.db` をバックアップしてください。
- `plugins/ReputationBan/config.yml` も一緒に保管してください。
- 既存 server では、先に test server で `/rep doctor` と smoke commands を確認してください。

## Smoke Commands

production rollout 前に test server で確認してください。

- `/plugins`
- `/rep version`
- `/rep help`
- `/rep doctor`
- `/rep backup before-smoke`
- `/rep support bundle`
- `/reports list all 10`
- `/rep audit recent 10`
- `/rep maintenance preview`

BAN 関連コマンドは smoke check 用の test users のみに実行してください。
