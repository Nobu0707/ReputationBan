# Codex実装プロンプト: ReputationBan Phase 1 / v0.1.0

あなたは実装担当のCodexです。空のワークスペースから、PaperMC 26.1.2 / Java 25 向けのMinecraftプラグイン `ReputationBan` を作成してください。ChatGPT側が仕様・要件定義とコードレビューを担当し、Codexは実装・テスト・ビルド成功・Gitコミット・GitHub pushまでを担当します。

## 0. 重要な前提

- OS/環境: WSL
- エディタ: VS Code
- 実装担当: Codex
- レビュー担当: ChatGPT
- Minecraft/Paper: PaperMC 26.1.2
- Java: 25
- ビルド: Gradle Kotlin DSL
- プラグイン方式: `plugin.yml` を使う通常のBukkit/Paperプラグイン
- 初期バージョン: `0.1.0`
- この実装は `Phase 1` とする
- NMS/CraftBukkit内部実装は使わない
- `paperweight-userdev` はPhase 1では使わない
- DBはSQLite
- パッケージ名: `dev.modplugin.reputationban`
- メインクラス: `dev.modplugin.reputationban.ReputationBanPlugin`
- GitHub remote URL: `TODO_REPLACE_WITH_GITHUB_REMOTE_URL`

作業前に `TODO_REPLACE_WITH_GITHUB_REMOTE_URL` を実際のGitHubリポジトリURLに置き換えてください。SSH例: `git@github.com:OWNER/ReputationBan.git`。HTTPS例: `https://github.com/OWNER/ReputationBan.git`。

## 1. Codexの完了条件

次の条件をすべて満たすまで作業してください。

1. 空のワークスペースからGitリポジトリを初期化する。
2. Gradleプロジェクトを作成する。
3. PaperMC 26.1.2 / Java 25でコンパイルできる。
4. `./gradlew clean test build` が成功する。
5. `build/libs/ReputationBan-0.1.0.jar` が生成される。
6. Phase 1 MVP機能を実装する。
7. `scripts/review_code.sh` を作成し、実行可能にする。
8. `./scripts/review_code.sh` が失敗しない状態にする。
9. Gitコミットを作成する。
10. GitHub remote `origin` を設定する。
11. `main` ブランチへpushする。
12. 最後に、実行したコマンド、ビルド結果、コミットハッシュ、push先URLを簡潔に報告する。

GitHub remote URLが未設定または認証不可の場合は、pushだけ失敗理由を明記し、それ以外は完了させてください。ただし、ローカルコミットまでは必ず完了してください。

## 2. Git初期化手順

空のワークスペースで以下を実施してください。

```bash
git init
git branch -M main
```

`.gitignore` を作成してください。最低限、以下を含めます。

```gitignore
.gradle/
build/
.idea/
.vscode/
*.iml
*.log
*.db
*.db-shm
*.db-wal
run/
server/
.DS_Store
```

作業完了時に以下を実施してください。

```bash
git add .
git commit -m "Phase 1: implement ReputationBan v0.1.0 MVP"
git remote add origin TODO_REPLACE_WITH_GITHUB_REMOTE_URL
git push -u origin main
```

`origin` が既に存在する場合は `git remote set-url origin ...` を使用してください。

## 3. Gradle要件

`settings.gradle.kts` と `build.gradle.kts` を作成してください。

### settings.gradle.kts 要件

- `rootProject.name = "ReputationBan"`
- `mavenCentral()` と PaperMC Maven repository を使う
- PaperMC repository: `https://repo.papermc.io/repository/maven-public/`

### build.gradle.kts 要件

- Java pluginを使用
- group: `dev.modplugin`
- version: `0.1.0`
- Java toolchain: 25
- `options.release.set(25)`
- dependency: `compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")`
- JUnit 5を最低限入れてテスト可能にする
- jar名は `ReputationBan-0.1.0.jar`

Gradle wrapperを作成してください。Java 25対応のため、Gradleは9.1.0以上を使用してください。可能なら安定版の最新Gradle wrapperを使ってください。

例:

```bash
gradle wrapper --gradle-version 9.5.1
```

ローカルに `gradle` コマンドが無い場合は、利用可能な方法でGradle wrapperを生成してください。wrapper生成後は `./gradlew` を使ってください。

## 4. ディレクトリ構成

最低限、以下の構成にしてください。

```text
ReputationBan/
├─ .gitignore
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradlew
├─ gradlew.bat
├─ gradle/
├─ scripts/
│  └─ review_code.sh
└─ src/
   └─ main/
      ├─ java/
      │  └─ dev/
      │     └─ modplugin/
      │        └─ reputationban/
      │           ├─ ReputationBanPlugin.java
      │           ├─ command/
      │           │  ├─ ReportBadCommand.java
      │           │  ├─ RepCommand.java
      │           │  └─ ReportsCommand.java
      │           ├─ config/
      │           │  └─ PluginConfig.java
      │           ├─ database/
      │           │  └─ DatabaseManager.java
      │           ├─ listener/
      │           │  └─ PlayerJoinListener.java
      │           ├─ model/
      │           │  ├─ PlayerRecord.java
      │           │  └─ ReportCategory.java
      │           ├─ service/
      │           │  ├─ PlayerDataService.java
      │           │  ├─ ReportService.java
      │           │  ├─ ScoreService.java
      │           │  └─ PunishmentService.java
      │           └─ util/
      │              └─ DurationParser.java
      └─ resources/
         ├─ plugin.yml
         └─ config.yml
```

構成は多少変更してもよいですが、責務分離は維持してください。

## 5. plugin.yml 要件

`src/main/resources/plugin.yml` を作成してください。

必須内容:

```yaml
name: ReputationBan
version: 0.1.0
main: dev.modplugin.reputationban.ReputationBanPlugin
api-version: '26.1.2'
description: Reputation score based moderation and temporary ban plugin.
author: MOD_PLUGIN

libraries:
  - org.xerial:sqlite-jdbc:3.53.1.0

commands:
  reportbad:
    description: 悪質なプレイヤーを通報します
    usage: /reportbad <player> <category> <reason>
    permission: reputationban.report
    aliases: [bad]

  rep:
    description: 評判スコアを確認・管理します
    usage: /rep
    aliases: [score]

  reports:
    description: 通報一覧を確認します
    usage: /reports
    permission: reputationban.admin.reports

permissions:
  reputationban.report:
    description: プレイヤーを通報できます
    default: true

  reputationban.score.self:
    description: 自分のスコアを確認できます
    default: true

  reputationban.score.others:
    description: 他人のスコアを確認できます
    default: op

  reputationban.notify:
    description: スタッフ通知を受け取ります
    default: op

  reputationban.admin:
    description: ReputationBanの管理権限
    default: op
    children:
      reputationban.admin.reports: true
      reputationban.admin.score: true
      reputationban.admin.ban: true
      reputationban.score.others: true
      reputationban.notify: true

  reputationban.admin.reports:
    description: 通報を管理できます
    default: op

  reputationban.admin.score:
    description: スコアを管理できます
    default: op

  reputationban.admin.ban:
    description: BANを管理できます
    default: op

  reputationban.bypass:
    description: 通報・減点・自動BANの対象外になります
    default: op
```

## 6. config.yml 要件

`src/main/resources/config.yml` を作成してください。

```yaml
initial-score: 100
max-score: 100

rating:
  enabled: true
  default-deduction: 10
  require-reason: true
  min-reason-length: 5
  min-unique-reports-before-deduction: 1
  report-window-days: 7

categories:
  griefing:
    display-name: "荒らし"
    deduction: 15
    staff-review-required: false

  abusive_chat:
    display-name: "暴言"
    deduction: 5
    staff-review-required: false

  spam:
    display-name: "スパム"
    deduction: 5
    staff-review-required: false

  scam:
    display-name: "詐欺"
    deduction: 10
    staff-review-required: true

  harassment:
    display-name: "嫌がらせ"
    deduction: 10
    staff-review-required: false

  cheating:
    display-name: "チート疑い"
    deduction: 0
    staff-review-required: true

  other:
    display-name: "その他"
    deduction: 0
    staff-review-required: true

cooldowns:
  global-report-seconds: 300
  same-target-cooldown-days: 14
  max-reports-per-day: 5
  max-reports-per-week: 15

report-requirements:
  min-playtime-minutes: 60
  min-account-age-days: 1

score-thresholds:
  warning: 70
  watch: 50
  restricted: 30
  final-warning: 10
  ban: 0

score-recovery:
  enabled: true
  points-per-day: 2
  max-score: 100
  no-report-days-required: 7

ban:
  enabled: true
  threshold: 0
  source: "ReputationBan"
  durations:
    first: "1d"
    second: "7d"
    third: "30d"
    fourth: "permanent"

notify:
  in-game-staff: true
  console: true
  discord-webhook: false
  staff-permission: "reputationban.notify"

reporter-penalty:
  enabled: true
  false-report-threshold: 5
  report-ban-days: 7

database:
  type: "sqlite"
  file: "reputationban.db"
```

## 7. Phase 1 MVP機能要件

### 7.1 起動・終了

`ReputationBanPlugin`:

- `JavaPlugin` を継承する。
- `saveDefaultConfig()` を実行する。
- `PluginConfig` を読み込む。
- `DatabaseManager` を初期化する。
- DBテーブルを作成する。
- サービス群を初期化する。
- `PlayerJoinListener` を登録する。
- `/rep`, `/reportbad`, `/reports` のコマンドExecutorを登録する。
- `onDisable()` でDB接続やExecutorServiceを安全に閉じる。

### 7.2 SQLite DB

DBファイルは以下に作成する。

```text
plugins/ReputationBan/reputationban.db
```

`DatabaseManager` は以下を行う。

- plugin data folderを作成する。
- SQLite JDBCで接続する。
- WAL設定を可能なら有効にする。
- テーブル作成SQLを実行する。
- 非同期DB処理用の単一スレッドExecutorを持つ。
- `CompletableFuture` を使った非同期メソッドを提供する。
- Bukkit/Paper API操作は非同期スレッドで行わない。

作成するテーブル:

```sql
CREATE TABLE IF NOT EXISTS players (
  uuid TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  score INTEGER NOT NULL,
  ban_count INTEGER NOT NULL DEFAULT 0,
  false_report_count INTEGER NOT NULL DEFAULT 0,
  report_banned_until INTEGER,
  first_seen INTEGER,
  last_seen INTEGER
);

CREATE TABLE IF NOT EXISTS reports (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  reporter_uuid TEXT NOT NULL,
  reporter_name TEXT NOT NULL,
  target_uuid TEXT NOT NULL,
  target_name TEXT NOT NULL,
  category TEXT NOT NULL,
  reason TEXT NOT NULL,
  status TEXT NOT NULL,
  deduction INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL,
  reviewed_by TEXT,
  reviewed_at INTEGER,
  review_note TEXT
);

CREATE TABLE IF NOT EXISTS score_history (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  target_uuid TEXT NOT NULL,
  target_name TEXT NOT NULL,
  old_score INTEGER NOT NULL,
  new_score INTEGER NOT NULL,
  delta INTEGER NOT NULL,
  reason TEXT NOT NULL,
  source_type TEXT NOT NULL,
  source_id INTEGER,
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS bans (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  target_uuid TEXT NOT NULL,
  target_name TEXT NOT NULL,
  reason TEXT NOT NULL,
  ban_type TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  expires_at INTEGER,
  created_by TEXT,
  unbanned_at INTEGER,
  unbanned_by TEXT
);
```

可能なら以下のindexも作成する。

```sql
CREATE INDEX IF NOT EXISTS idx_reports_reporter_target_created ON reports(reporter_uuid, target_uuid, created_at);
CREATE INDEX IF NOT EXISTS idx_reports_target_created ON reports(target_uuid, created_at);
CREATE INDEX IF NOT EXISTS idx_score_history_target_created ON score_history(target_uuid, created_at);
```

### 7.3 プレイヤーデータ

`PlayerJoinListener`:

- `PlayerJoinEvent` でプレイヤーUUIDと名前を取得する。
- 初回参加なら `players` に `score = initial-score` で挿入する。
- 既存なら `name` と `last_seen` を更新する。

`PlayerDataService`:

- `ensurePlayer(UUID, name)`
- `getPlayerRecord(UUID)`
- `findByName(String name)` またはオンラインプレイヤー優先の検索
- `getScore(UUID)`
- `incrementBanCount(UUID)`

すべてUUIDを主キーにする。名前は表示・検索補助としてのみ使う。

### 7.4 /rep コマンド

`/rep`:

- プレイヤーのみ実行可能。
- 権限 `reputationban.score.self` を確認する。
- 自分のスコアを表示する。

表示例:

```text
[ReputationBan] あなたの評判スコア: 100 / 100
```

`/rep check <player>`:

- 権限 `reputationban.score.others` または `reputationban.admin.score` を確認する。
- 対象プレイヤーのスコア、BAN回数、虚偽通報回数を表示する。
- オンラインプレイヤーを優先して検索し、DBに存在する名前も検索する。

`/rep reload`:

- 権限 `reputationban.admin` を確認する。
- configを再読み込みする。

### 7.5 /reportbad コマンド

形式:

```text
/reportbad <player> <category> <reason>
```

必須チェック:

- 実行者はプレイヤーのみ。
- 権限 `reputationban.report` を確認する。
- `rating.enabled` がfalseなら拒否。
- 対象プレイヤーを名前で検索する。
- 自分自身への通報は禁止。
- 対象が `reputationban.bypass` 権限を持つオンラインプレイヤーなら拒否。
- カテゴリは `config.yml` の `categories` に存在する必要がある。
- 理由は `rating.min-reason-length` 以上。
- `cooldowns.global-report-seconds` の全体クールダウンを確認する。
- `cooldowns.same-target-cooldown-days` の同一対象再通報制限をDB上のreportsから確認する。
- `cooldowns.max-reports-per-day` と `cooldowns.max-reports-per-week` を確認する。
- `report_banned_until` が現在時刻より未来なら通報不可。

通報成功時:

- `reports` に保存する。
- `staff-review-required: true` のカテゴリはPhase 1では `pending` として保存し、自動減点しない。
- `staff-review-required: false` のカテゴリは `auto_accepted` として保存し、対象スコアを `deduction` 分だけ減点する。
- 減点時は `score_history` に保存する。
- スコア変更後、しきい値に応じて通知する。
- `score <= ban.threshold` の場合、BAN処理を呼ぶ。

### 7.6 スコア更新

`ScoreService`:

- `applyDelta(UUID targetUuid, String targetName, int delta, String reason, String sourceType, Long sourceId)` を実装する。
- `old_score`, `new_score`, `delta` を `score_history` に保存する。
- `max-score` を超えないようにする。
- 減点で0未満になってもよいが、表示上はそのまま扱う。

### 7.7 BAN処理

`PunishmentService`:

- `ban.enabled` がfalseなら何もしない。
- 対象がオンラインで `reputationban.bypass` を持つ場合はBANしない。
- `ban_count` に応じてBAN期間を選ぶ。
  - 0回: `ban.durations.first`
  - 1回: `ban.durations.second`
  - 2回: `ban.durations.third`
  - 3回以上: `ban.durations.fourth`
- `1d`, `7d`, `30d`, `permanent` を解析する `DurationParser` を作る。
- `bans` テーブルに記録する。
- Paper/Bukkit APIで対象をBANする。
- オンラインならKickする。
- `players.ban_count` を増やす。
- スタッフへ通知する。

Paper APIのメソッド差異でコンパイルエラーが出る場合は、26.1.2のJavadocに合わせて修正してください。NMSは使わないでください。

### 7.8 /reports コマンド

Phase 1では最小実装でよい。

- `/reports` で直近10件程度の通報を表示する。
- 権限 `reputationban.admin.reports` を確認する。
- pending/auto_acceptedの状態がわかるようにする。

承認/却下機能はPhase 2で実装するため、Phase 1では未実装メッセージでもよい。

## 8. メッセージと安全性

- すべてのプレイヤー向けメッセージには `[ReputationBan]` prefixを付ける。
- DBエラーはプレイヤーに詳細を出さず、コンソールにログ出力する。
- 非同期DB処理中にBukkit APIを直接呼ばない。
- Bukkit APIを呼ぶ必要がある処理はメインスレッドに戻す。
- `CompletableFuture` の例外を握りつぶさない。
- SQLはPreparedStatementを使う。
- UUIDを主キーにする。
- プレイヤー名だけで永続管理しない。

## 9. テスト要件

最低限、JUnitで以下をテストしてください。

- `DurationParser` が `1d`, `7d`, `30d`, `12h`, `permanent` を正しく処理する。
- 不正な期間文字列が適切に拒否される。
- `ReportCategory` または `PluginConfig` 周辺の単純な値検証。

Mockito等は必須ではありません。Paperサーバーを起動する統合テストはPhase 1では不要です。

## 10. レビュー用スクリプト

`scripts/review_code.sh` を作成し、実行可能にしてください。

このスクリプトは最低限、以下を確認します。

- Gitリポジトリであること
- 必須ファイルが存在すること
- `plugin.yml` の name/version/main/api-version が正しいこと
- `/reportbad`, `/rep`, `/reports` が定義されていること
- 主要permissionが定義されていること
- `config.yml` に必要設定があること
- Paper API 26.1.2依存があること
- Java 25 toolchainがあること
- NMS/CraftBukkit使用がないこと
- `./gradlew clean test build --warning-mode all` が成功すること
- JAR内に `plugin.yml` とメインクラスが入っていること
- 最低1コミットあること

以下のスクリプト内容をベースにしてよいです。必要に応じてプロジェクトに合わせて修正してください。

```bash
#!/usr/bin/env bash
set -euo pipefail

PROJECT_NAME="ReputationBan"
EXPECTED_VERSION="0.1.0"
EXPECTED_MAIN="dev.modplugin.reputationban.ReputationBanPlugin"
EXPECTED_API_VERSION="26.1.2"
EXPECTED_PACKAGE_DIR="src/main/java/dev/modplugin/reputationban"
EXPECTED_JAR_PREFIX="ReputationBan"

fail() { echo "[FAIL] $*" >&2; exit 1; }
warn() { echo "[WARN] $*" >&2; }
pass() { echo "[PASS] $*"; }
info() { echo "[INFO] $*"; }
require_file() { [[ -f "$1" ]] || fail "Missing required file: $1"; pass "Found $1"; }
require_dir() { [[ -d "$1" ]] || fail "Missing required directory: $1"; pass "Found $1"; }
require_command() { command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"; pass "Command available: $1"; }
extract_yaml_value() { grep -E "^$2:" "$1" | head -n 1 | sed -E "s/^$2:[[:space:]]*//" | tr -d "'\""; }

require_command git
require_command grep
require_command sed
require_command find
require_command sort
require_command tail

[[ -d .git ]] || fail "Not a Git repository"
git rev-parse --is-inside-work-tree >/dev/null || fail "Not inside a Git work tree"

require_file settings.gradle.kts
require_file build.gradle.kts
require_file gradlew
require_file src/main/resources/plugin.yml
require_file src/main/resources/config.yml
require_dir "$EXPECTED_PACKAGE_DIR"
[[ -x ./gradlew ]] || fail "gradlew is not executable"

grep -q "io.papermc.paper:paper-api:26.1.2.build" build.gradle.kts || fail "Paper API 26.1.2 dependency not found"
grep -q "JavaLanguageVersion.of(25)" build.gradle.kts || fail "Java 25 toolchain not found"

YML=src/main/resources/plugin.yml
[[ "$(extract_yaml_value "$YML" name)" == "$PROJECT_NAME" ]] || fail "Invalid plugin.yml name"
[[ "$(extract_yaml_value "$YML" version)" == "$EXPECTED_VERSION" ]] || fail "Invalid plugin.yml version"
[[ "$(extract_yaml_value "$YML" main)" == "$EXPECTED_MAIN" ]] || fail "Invalid plugin.yml main"
[[ "$(extract_yaml_value "$YML" api-version)" == "$EXPECTED_API_VERSION" ]] || fail "Invalid plugin.yml api-version"

grep -q "reportbad:" "$YML" || fail "Missing reportbad command"
grep -q "rep:" "$YML" || fail "Missing rep command"
grep -q "reports:" "$YML" || fail "Missing reports command"
grep -q "reputationban.report:" "$YML" || fail "Missing reputationban.report"
grep -q "reputationban.bypass:" "$YML" || fail "Missing reputationban.bypass"
grep -q "org.xerial:sqlite-jdbc" "$YML" || fail "Missing sqlite-jdbc library"

CFG=src/main/resources/config.yml
grep -q "^initial-score:[[:space:]]*100" "$CFG" || fail "Missing initial-score: 100"
grep -q "^max-score:[[:space:]]*100" "$CFG" || fail "Missing max-score: 100"
grep -q "^categories:" "$CFG" || fail "Missing categories"
grep -q "same-target-cooldown-days" "$CFG" || fail "Missing same-target-cooldown-days"
grep -q "global-report-seconds" "$CFG" || fail "Missing global-report-seconds"
grep -q "threshold:[[:space:]]*0" "$CFG" || fail "Missing ban threshold 0"

grep -R "extends JavaPlugin" src/main/java >/dev/null || fail "Main JavaPlugin class not found"
grep -R "PlayerJoinEvent" src/main/java >/dev/null || fail "PlayerJoinEvent handling not found"
grep -R "CREATE TABLE" src/main/java >/dev/null || fail "Table creation SQL not found"
grep -R "getUniqueId" src/main/java >/dev/null || fail "UUID handling not found"
grep -R "reputationban.bypass" src/main/java >/dev/null || fail "bypass permission check not found"

if grep -R "net\.minecraft\|org\.bukkit\.craftbukkit\|CraftPlayer\|NMS" src/main/java >/dev/null; then
  fail "NMS/CraftBukkit usage detected"
fi

./gradlew clean test build --warning-mode all

JAR="$(find build/libs -maxdepth 1 -type f -name "${EXPECTED_JAR_PREFIX}-*.jar" | sort | tail -n 1 || true)"
[[ -n "$JAR" ]] || fail "Built jar not found"
require_command jar
jar tf "$JAR" | grep -q "plugin.yml" || fail "plugin.yml missing from jar"
jar tf "$JAR" | grep -q "dev/modplugin/reputationban/ReputationBanPlugin.class" || fail "Main class missing from jar"

git rev-list --count HEAD >/dev/null || fail "No commits found"

pass "Review checks completed"
```

## 11. フェーズ計画

### Phase 1 / v0.1.0: MVP

実装対象:

- Git初期化
- Gradleプロジェクト
- plugin.yml/config.yml
- SQLite DB初期化
- PlayerJoin時の初期スコア登録
- `/rep`
- `/rep check <player>`
- `/rep reload`
- `/reportbad <player> <category> <reason>`
- 通報保存
- クールダウン
- 同一対象再通報制限
- 1日/1週間の通報上限
- カテゴリ別の自動減点またはpending保存
- score_history保存
- score <= 0 の期限付きBAN
- `/reports` の直近表示
- JUnit最小テスト
- レビュー用スクリプト
- build成功
- commit/push

### Phase 2 / v0.2.0: 審査・運営機能

Phase 1では実装しない。将来実装する。

- `/reports view <id>`
- `/reports approve <id>`
- `/reports reject <id>`
- 虚偽通報カウント
- 通報者の一時通報禁止
- `/rep history <player>`
- `/rep set/add/remove/pardon`
- スコア回復タスク
- BAN取り消し・手動解除補助
- より詳細な通知

### Phase 3 / v0.3.0: 連携・高度な悪用対策

Phase 1では実装しない。将来実装する。

- Discord Webhook通知
- 管理GUI
- 信頼度システム
- 接触判定
- CoreProtect/LuckPerms等のsoftdepend連携
- 集団通報検知
- 監査ログ強化
- 設定可能なメッセージファイル

## 12. 実装上の注意

- コードは読みやすさと保守性を優先する。
- 1クラスに詰め込まず、責務を分ける。
- SQLはPreparedStatementを使う。
- DB例外はログ出力し、必要ならプレイヤーには一般的な失敗メッセージだけ返す。
- `CompletableFuture` の完了後にプレイヤーへメッセージを送るときは、メインスレッドに戻す。
- `Bukkit.getScheduler().runTask(plugin, ...)` を使ってメインスレッド処理へ戻す。
- 非同期処理内で `Player`, `World`, `Entity`, `Bukkit` の状態変更をしない。
- UUIDを必ず使う。
- 対象プレイヤーがオフラインの場合でも、DBに既存レコードがあれば通報対象にできるようにする。
- ただし、Phase 1では完全なプレイヤー名解決は簡易実装でよい。
- `reputationban.bypass` 持ちのオンライン対象は通報・減点・BAN対象外にする。
- オフライン対象のbypass判定はPhase 1では完全でなくてよいが、実装可能ならPermissionAttachment等に依存せず安全側に倒す。

## 13. 最終報告フォーマット

作業完了後、以下の形式で報告してください。

```text
Phase 1 / v0.1.0 completed.

Build:
- ./gradlew clean test build: SUCCESS
- ./scripts/review_code.sh: SUCCESS
- Artifact: build/libs/ReputationBan-0.1.0.jar

Git:
- Branch: main
- Commit: <commit hash>
- Remote: <origin url>
- Push: SUCCESS or FAILED + reason

Implemented:
- <主要機能の箇条書き>

Notes:
- <制約、未実装、Phase 2に回したもの>
```
