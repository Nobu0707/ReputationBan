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

## Phase 3 / v0.3.0: 通報者ペナルティ・スコア回復

目的: スタッフ審査を土台に、虚偽通報への抑止とスコアの自然回復を追加する。

実装範囲:

- 手動スコア変更時のBAN権限ゲート強化
- /reports reject 時の false_report_count 加算
- false_report_count しきい値到達時の一時通報停止
- /reportbad 時の report_banned_until 確認
- /rep check への虚偽通報回数・通報停止状態表示
- score-recovery 設定による定期スコア回復
- score_history への recovery 記録
- players.last_recovery_at による二重回復防止
- /reports list <status> [limit]
- review_code.sh / make-review-archive.sh 強化

## Phase 4 / v0.4.0: BAN管理・審査安全化

目的: BANを伴う審査と復帰操作を安全にし、運営者がBAN状態を確認・解除できるようにする。

実装範囲:

- /reports approve 時のBAN権限ゲート
- approve直前のbypass/OP保護確認
- /rep banhistory <player> [limit]
- /rep baninfo <player>
- /rep unban <player> [reason]
- /rep pardon <player> [reason]
- Profile BAN解除と bans.unbanned_at / unbanned_by 更新
- pardon の score_history 記録
- review_code.sh / make-review-archive.sh Phase 4対応

## Phase 5以降: 外部連携・高度な悪用対策

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
