# ReputationBan 開発フェーズ計画

## Phase 1 / v0.1.0: MVP

目的: 点数制BANプラグインとして最小限動作する状態を作る。

実装範囲:

- Git初期化、Gradleプロジェクト作成、GitHub push
- PaperMC 26.1.2 / Java 25対応
- plugin.yml / config.yml
- SQLite DB初期化
- players / reports / score_history / bans テーブル
- PlayerJoin時の初期スコア登録
- /rep
- /rep check <player>
- /rep reload
- /reportbad <player> <category> <reason>
- 通報保存
- カテゴリ別減点
- クールダウン
- 同一対象再通報制限
- 通報件数上限
- score_history保存
- score <= 0 の期限付きBAN
- /reports の直近表示
- JUnit最小テスト
- review_code.sh

完了条件:

- ./gradlew clean test build 成功
- ./scripts/review_code.sh 成功
- build/libs/ReputationBan-0.1.0.jar 生成
- Gitコミット作成
- GitHub push完了

## Phase 2 / v0.2.0: スタッフ審査・管理機能

目的: 自動減点だけでなく、スタッフによる審査・修正・復旧を可能にする。

実装候補:

- /reports view <id>
- /reports approve <id>
- /reports reject <id>
- /rep history <player>
- /rep set <player> <score>
- /rep add <player> <points>
- /rep remove <player> <points>
- /rep pardon <player>
- /rep undo <actionId>
- 虚偽通報カウント
- 通報者の一時通報禁止
- スコア回復タスク
- BAN履歴表示
- 手動unban補助

## Phase 3 / v0.3.0: 外部連携・高度な悪用対策

目的: 公開サーバー運用で使いやすい高度なモデレーション機能へ拡張する。

実装候補:

- Discord Webhook通知
- 管理GUI
- 信頼度システム
- 接触判定
- 集団通報検知
- CoreProtect連携
- LuckPerms連携
- WorldGuard/GriefPrevention連携
- 設定可能なメッセージファイル
- 監査ログ強化
