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

## Phase 11 / v0.11.0: リリース準備・実機スモーク支援

目的: v1.0.0前に、配布・導入・実機確認しやすい状態へ整える。

実装範囲:

- version 0.11.0
- `/rep version`
- `/rep help` とTAB補完への `version` 追加
- `/rep doctor` への `pluginDataFolder`、`databaseFileExists`、`discordWebhookEnabled`、`discordWebhookUrlConfigured`、`auditExportDirectorySafe`、`backupDirectoryWritable` 追加
- `DIAGNOSTICS_RUN` metadata への安全なDB/backup状態追加
- `CHANGELOG.md`
- `docs/INSTALLATION.md`
- `docs/CONFIGURATION.md`
- `docs/MIGRATION.md`
- `docs/RELEASE_READINESS.md`
- `scripts/run-paper-runtime-smoke-helper.sh`
- `run-local-smoke-check.sh` の `REPUTATIONBAN_SKIP_BUILD` 対応
- `make-review-archive.sh` からの local smoke 重複ビルド軽減
- review_code.sh / make-review-archive.sh Phase 11対応

注意:

- Webhook URLは表示、ログ、監査metadata、CSV、レビューアーカイブへ出さない。
- Paper runtime smoke helperは既存DBやサーバーディレクトリを削除しない。
- Phase 11ではGUI、外部保護プラグイン連携、Folia対応は実装しない。

## Phase 12 / v0.12.0: サポートバンドル・配布artifact整備

目的: v1.0.0前に、問題発生時に安全に調査できる状態と配布物を確実に作れる状態を整える。

実装範囲:

- version 0.12.0
- `/rep backup [reason]`
- `DB_BACKUP_CREATED`
- `/rep support bundle`
- `SUPPORT_BUNDLE_CREATED`
- `config-redacted.yml` 生成用の Redactor / ConfigRedactor
- support bundle からDB、WAL/SHM、server logs、Webhook URLを除外
- `scripts/create-release-artifact.sh`
- `build/release/ReputationBan-0.12.0.jar`
- `build/release/ReputationBan-0.12.0.jar.sha256`
- `build/release/ReputationBan-0.12.0-release.zip`
- `docs/SUPPORT_BUNDLE.md`
- `docs/phase-12.md`
- review_code.sh / make-review-archive.sh Phase 12対応

注意:

- Webhook URLは表示、ログ、監査metadata、CSV、support bundle、release artifact、レビューアーカイブへ出さない。
- support bundleにDBやserver logsを含めない。
- release zipに実config.yml、DB、logsを含めない。
- Phase 12ではGUI、外部保護プラグイン連携、Folia対応は実装しない。

## Phase 13 / v0.13.0: リリース候補ハードニング

目的: v1.0.0候補へ進む前に、配布物、support bundle、redaction、実機スモーク記録を最終確認しやすくする。

実装範囲:

- version 0.13.0
- free-text reason の token/password/secret/session/cookie/webhook/url 風値のredaction強化
- support bundle の `meta.txt` / `doctor.txt` で共有不要な絶対パスを丸める
- support bundle安全性検証ロジックとテスト強化
- `scripts/verify-release-artifact.sh`
- release ZIP自体の `.sha256`
- `docs/SECURITY_REDACTION.md`
- `docs/PAPER_RUNTIME_SMOKE_REPORT_TEMPLATE.md`
- `scripts/record-paper-runtime-smoke-result.sh`
- review_code.sh / make-review-archive.sh Phase 13対応

注意:

- Webhook URLは表示、ログ、監査metadata、CSV、support bundle、release artifact、レビューアーカイブへ出さない。
- support bundleにDBやserver logsを含めず、共有不要な絶対パスを避ける。
- release zipに実config.yml、DB、logsを含めない。
- Phase 13ではGUI、外部保護プラグイン連携、Folia対応は実装しない。

## Phase 14 / v0.14.0: 日本語ドキュメント整備

目的: v1.0.0候補へ進む前に、README.md と docs/*.md を日本語読者向けに整え、配布物とレビューアーカイブの確認対象を v0.14.0 に更新する。

実装範囲:

- version 0.14.0
- README.md の日本語化
- docs/*.md の日本語化
- `docs/phase-14.md`
- `CHANGELOG.md` の日本語読者向け整理
- command names、permission nodes、config keys、file names、package names、script names は翻訳しない
- Discord Webhook URL の実例を追加しない
- support bundle と review archive に Webhook URL、DB、logs を含めない方針の明文化
- `scripts/review_code.sh` の Phase 14 対応
- `scripts/run-local-smoke-check.sh` の v0.14.0 対応
- `scripts/create-release-artifact.sh` の v0.14.0 対応
- `scripts/verify-release-artifact.sh` の v0.14.0 対応
- `scripts/make-review-archive.sh` の Phase 14 review signals 追加

注意:

- Phase 14では Java 本体の機能追加、DBスキーマ変更、コマンド追加は行わない。
- YAML key、permission node、command name は変更しない。
- Webhook URLは表示、ログ、監査metadata、CSV、support bundle、release artifact、レビューアーカイブへ出さない。
- secret scanは確認用であり、説明文に `token` や `secret` があるだけでは即失敗にしない。

## Phase 15 / v0.15.0: リリース候補確認ゲート

目的: v1.0.0候補へ進む前に、日本語ドキュメント品質、release artifact、review archive、Paper実機スモーク状態を確認しやすくする。

実装範囲:

- version 0.15.0
- `docs/phase-15.md`
- `docs/RELEASE_CANDIDATE_CHECKLIST.md`
- `scripts/check-docs-localization.sh`
- README.md と docs/*.md の日本語テキスト確認
- command names、permission nodes、config keys、YAML examples を翻訳しない方針の確認
- docs 内の実 Discord Webhook URL 風パターン検出
- `scripts/review_code.sh` の Phase 15 対応
- `scripts/run-local-smoke-check.sh` の v0.15.0 対応
- `scripts/create-release-artifact.sh` の v0.15.0 対応
- `scripts/verify-release-artifact.sh` の localized docs と禁止ファイル検証強化
- `scripts/make-review-archive.sh` の docs localization 収集と Paper runtime smoke 未実施 summary 明確化

注意:

- Phase 15では Java 本体の機能追加、DBスキーマ変更、コマンド追加は行わない。
- Paper runtime smokeを実施していない場合はPASS扱いにせず、`status=NOT_RUN` と記録する。
- Webhook URLは表示、ログ、監査metadata、CSV、support bundle、release artifact、レビューアーカイブへ出さない。
- v1.0.0前に、可能な限り実Paperサーバーで `/rep version`、`/rep doctor`、`/rep support bundle`、`/rep backup`、`/reportbad` TAB補完を確認する。

## Phase 17 / v0.17.0: LuckPerms / CoreProtect 運用診断強化

目的: Phase 16 で追加した LuckPerms / CoreProtect 連携を、運用で確認、説明、レビューしやすい状態にする。

実装範囲:

- version 0.17.0
- `/reports evidence <id>` の追加
- `/rep integrations test` の追加
- `/rep integrations` の詳細表示
- LuckPerms metadata に `primaryGroup`、`reporterWeight`、`bypassGroup`、`applyWeightToDeduction` を保存
- CoreProtect metadata に `resultCount`、`lookupSeconds`、`radius`、`category`、`world`、`x`、`y`、`z`、`apiVersion` を保存
- CoreProtect summary の日本語運用向け改善
- `scripts/check-optional-dependency-safety.sh`
- `docs/INTEGRATION_RUNTIME_SMOKE_CHECKLIST.md`
- `scripts/record-integration-runtime-smoke-result.sh`
- `COREPROTECT_CONTEXT_CAPTURED` と `INTEGRATION_STATUS_CHECKED` audit events
- `docs/INTEGRATIONS.md` と `docs/phase-17.md`
- review/release archive scripts の v0.17.0 対応

注意:

- CoreProtect rollback、restore、purge は実行しない。
- LuckPerms の group/permission 書き込みは実行しない。
- LuckPerms / CoreProtect は任意依存であり、未導入や片方だけ導入の環境でも ReputationBan 本体は起動できる設計にする。
- LuckPerms のオフラインユーザー情報が未ロードの場合、Phase 17 では `default-weight` 扱いになる場合がある。
- CoreProtect lookup は審査補助であり、rollback、restore、purge は行わない。
- `apply-weight-to-deduction` は Phase 17 では metadata と表示用であり、減点量へは未反映。
- WorldGuard、GriefPrevention、DiscordSRV、GUI、Folia、v1.0.0 readiness gate は後続。

## Phase 18 / v0.18.0: WorldGuard 任意連携

目的: WorldGuard / WorldEdit を任意依存として扱い、通報時の region context を審査補助として保存する。

実装範囲:

- version 0.18.0
- `WorldEdit` / `WorldGuard` の `softdepend`
- EngineHub repository と WorldGuard / WorldEdit `compileOnly` dependency
- `WorldGuardReflectionAdapter`
- `/rep integrations` と `/rep integrations test` の WorldGuard 表示
- `/rep doctor` の WorldGuard 簡易状態表示
- `/reportbad` 後の `report_context` provider `worldguard`
- `/reports view <id>` と `/reports evidence <id>` の WorldGuard 表示
- `WORLDGUARD_CONTEXT_CAPTURED` audit event
- `docs/INTEGRATIONS.md` と `docs/phase-18.md`
- review/release archive scripts の v0.18.0 対応

注意:

- `src/main/java` に `import com.sk89q.worldguard.` と `import com.sk89q.worldedit.` を追加しない。
- WorldGuard / WorldEdit 未導入でも ReputationBan 本体は起動できる設計にする。
- WorldGuard region/flag の作成、変更、削除は行わない。
- WorldGuard context は審査補助であり、自動 BAN の唯一根拠にしない。
- CoreProtect rollback、restore、purge と LuckPerms 書き込み操作は引き続き行わない。

## Phase 19 / v0.19.0: GriefPrevention 任意連携

目的: GriefPrevention を任意依存として扱い、通報時の claim context を審査補助として保存する。

実装範囲:

- version 0.19.0
- `GriefPrevention` の `softdepend`
- `GriefPreventionReflectionAdapter`
- `/rep integrations` と `/rep integrations test` の GriefPrevention 表示
- `/rep doctor` の GriefPrevention 簡易状態表示
- `/reportbad` 後の `report_context` provider `griefprevention`
- `/reports view <id>` と `/reports evidence <id>` の GriefPrevention 表示
- `GRIEFPREVENTION_CONTEXT_CAPTURED` audit event
- `docs/INTEGRATIONS.md` と `docs/phase-19.md`
- review/release archive scripts の v0.19.0 対応

注意:

- `src/main/java` に GriefPrevention API の直接 import を追加しない。
- GriefPrevention 未導入でも ReputationBan 本体は起動できる設計にする。
- GriefPrevention claim/trust の作成、変更、削除は行わない。
- GriefPrevention context は審査補助であり、自動 BAN の唯一根拠にしない。
- CoreProtect rollback、restore、purge、LuckPerms 書き込み、WorldGuard region/flag 変更は引き続き行わない。

## Phase 20以降: 外部連携・高度な悪用対策

実装候補:

- 管理GUI
- 接触判定
- 集団通報検知
- DiscordSRV連携
- 設定可能なメッセージファイル
