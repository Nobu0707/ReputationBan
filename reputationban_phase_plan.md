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

## Phase 5 / v0.5.0: 運用性・監査性強化

目的: 外部連携に進む前に、日常運用で使うコマンドの発見性とBAN解除監査情報を改善する。

実装範囲:

- /reportbad, /rep, /reports のTAB補完
- /rep help
- /reports help
- bans.unban_reason 追加と既存DB向けマイグレーション
- unbanned_by と unban_reason の分離
- /rep unban と /rep pardon の表示メッセージ改善
- /rep banhistory への解除者・解除理由表示
- limit / ID の入力エラー明確化
- review_code.sh / make-review-archive.sh Phase 5対応

## Phase 6 / v0.6.0: Discord Webhook通知

目的: サーバー外でも通報・審査・BAN状況を追えるよう、秘匿性を保ったDiscord Webhook通知を追加する。

実装範囲:

- notify.discord-webhook セクション設定
- 旧 boolean 形式の後方互換
- 通知イベント種別ごとのON/OFF
- Java HttpClient の sendAsync による非同期Webhook送信
- 通報作成、承認、却下、自動BAN、BAN解除、pardon、通報者ペナルティ、回復サマリー通知
- Webhook URLのログ出力禁止
- JSONエスケープとcontent長制限
- 失敗ログのレート制限
- review_code.sh / make-review-archive.sh Phase 6対応

## Phase 7 / v0.7.0: 通報安全性強化

目的: 公開サーバー運用で悪用されやすい低評価・通報を、参加条件・複数通報しきい値・スコア警告通知で強化する。

実装範囲:

- `/reportbad` 通報者の累計プレイ時間チェック
- `/reportbad` 通報者のサーバー初参加 `players.first_seen` からの日数チェック
- `rating.min-unique-reports-before-deduction`
- `rating.report-window-days`
- `threshold_pending` report status
- 同一対象・同一カテゴリ・期間内ユニーク通報者数による一回だけの自動減点
- `/reports list threshold_pending`
- TAB補完への `threshold_pending` 追加
- `score-thresholds` warning/watch/restricted/final-warning の下方向跨ぎ通知
- Discord `score-threshold-crossed` 通知
- review_code.sh / make-review-archive.sh Phase 7対応
- version 0.7.0

注意:

- `report-requirements.min-account-age-days` は Mojang アカウント作成日ではなく、ReputationBanが記録するサーバー初参加日時からの日数。
- `threshold_pending` はスタッフ審査待ちではなく、複数通報しきい値待ち。

## Phase 8 / v0.8.0: 監査ログ・データ保持

目的: 実運用時の説明責任・調査性・保守性を高める。

実装範囲:

- `audit_events` テーブルとindex
- AuditService / AuditEvent / AuditEventType
- 通報作成、しきい値到達、審査、手動スコア変更、自動BAN、BAN解除、pardon、スコア回復、通報者ペナルティ、reload、maintenance の監査記録
- `/rep audit recent [limit]`
- `/rep audit <player> [limit]`
- `/rep audit type <eventType> [limit]`
- `/rep audit export recent [limit]`
- `/rep audit export <player> [limit]`
- `/rep maintenance run`
- CSVエスケープ
- retention設定
- Webhook URL等のシークレットを監査・CSV・ログへ出さない方針
- review_code.sh / make-review-archive.sh Phase 8対応
- version 0.8.0

## Phase 9 / v0.9.0: 運用ハードニング

目的: v1.0.0前に、監査・CSV・maintenance・設定検証・実機スモーク確認の安全性を高める。

実装範囲:

- unban / pardon / maintenance audit actor の `actor_uuid` と `actor_name` 分離
- `CommandActor`
- `SafePathResolver` による audit CSV export の data folder 外脱出防止
- `/rep maintenance preview`
- `/rep maintenance run confirm`
- `/rep maintenance run` の confirm 案内化
- maintenance 実行前 SQLite backup
- `MAINTENANCE_PREVIEW`
- `ConfigValidator` / `ConfigValidationIssue`
- 起動時と `/rep reload` 時の設定検証ログと実行者サマリ
- runtime smoke スクリプトとチェックリスト
- review archive secret scan
- review_code.sh / make-review-archive.sh Phase 9対応
- version 0.9.0

## Phase 10 / v0.10.0: 運用診断・リリース準備

目的: v1.0.0前に、Phase 9レビューで見つかった軽微な永続ID問題を修正し、運用者向けの安全な診断コマンドとレビューアーカイブ収集を整える。

実装範囲:

- `bans.unbanned_by_name` カラム追加と既存DB向けマイグレーション
- `bans.unbanned_by` はプレイヤーUUID文字列または `CONSOLE`、`bans.unbanned_by_name` は表示名として分離
- `/rep banhistory` で解除者ID、解除者名、解除理由を表示
- `/rep doctor` / `/rep diagnostics`
- `reputationban.admin.diagnostics`
- `DIAGNOSTICS_RUN`
- DB接続、主要テーブル、設定検証、Discord enabled/urlConfigured、audit export directory safety、retention、pending/threshold_pending/active DB bans の軽量診断
- Webhook URLをdoctor、audit metadata、CSV、ログ、レビューアーカイブへ出さない方針の継続
- `scripts/run-local-smoke-check.sh` の結果をレビューアーカイブへ収集
- review_code.sh / make-review-archive.sh Phase 10対応
- release readiness docs
- version 0.10.0

## Phase 11以降: 外部連携・高度な悪用対策

実装候補:

- 管理GUI
- 信頼度システム
- 接触判定
- 集団通報検知
- CoreProtect連携
- LuckPerms連携
- WorldGuard/GriefPrevention連携
- 設定可能なメッセージファイル
