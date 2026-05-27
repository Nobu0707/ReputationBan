#!/usr/bin/env bash
set -euo pipefail

PROJECT_NAME="ReputationBan"
EXPECTED_VERSION="0.12.0"
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
require_file scripts/make-review-archive.sh
require_file scripts/run-local-smoke-check.sh
require_file scripts/run-paper-runtime-smoke-helper.sh
require_file scripts/create-release-artifact.sh
require_file CHANGELOG.md
require_file docs/INSTALLATION.md
require_file docs/CONFIGURATION.md
require_file docs/MIGRATION.md
require_file docs/RELEASE_READINESS.md
require_file docs/SUPPORT_BUNDLE.md
require_file docs/phase-12.md
require_file docs/runtime-smoke-checklist.md
require_file src/main/resources/plugin.yml
require_file src/main/resources/config.yml
require_dir "$EXPECTED_PACKAGE_DIR"
[[ -x ./gradlew ]] || fail "gradlew is not executable"
[[ -x ./scripts/review_code.sh ]] || fail "review_code.sh is not executable"
[[ -x ./scripts/make-review-archive.sh ]] || fail "make-review-archive.sh is not executable"
[[ -x ./scripts/run-local-smoke-check.sh ]] || fail "run-local-smoke-check.sh is not executable"
[[ -x ./scripts/run-paper-runtime-smoke-helper.sh ]] || fail "run-paper-runtime-smoke-helper.sh is not executable"
[[ -x ./scripts/create-release-artifact.sh ]] || fail "create-release-artifact.sh is not executable"

YML=src/main/resources/plugin.yml
grep -q "io.papermc.paper:paper-api:26.1.2.build" build.gradle.kts || fail "Paper API 26.1.2 dependency not found"
grep -q "org.xerial:sqlite-jdbc" "$YML" || fail "Missing sqlite-jdbc library"
grep -q "JavaLanguageVersion.of(25)" build.gradle.kts || fail "Java 25 toolchain not found"
grep -q 'version = "0.12.0"' build.gradle.kts || fail "build.gradle.kts version is not 0.12.0"
grep -q "options.release.set(25)" build.gradle.kts || fail "Java release 25 not found"

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
grep -q "^score-recovery:" "$CFG" || fail "Missing score-recovery config"
grep -q "^reporter-penalty:" "$CFG" || fail "Missing reporter-penalty config"
grep -q "^audit:" "$CFG" || fail "Missing audit config"
grep -q "export-directory" "$CFG" || fail "Missing audit export-directory config"
grep -q "^retention:" "$CFG" || fail "Missing retention config"
grep -q "audit-events-days" "$CFG" || fail "Missing audit-events retention config"
grep -q "score-history-days:[[:space:]]*0" "$CFG" || fail "score-history retention must default to 0"
grep -q "bans-days:[[:space:]]*0" "$CFG" || fail "bans retention must default to 0"
grep -q "min-unique-reports-before-deduction" "$CFG" || fail "Missing min-unique-reports-before-deduction config"
grep -q "report-window-days" "$CFG" || fail "Missing report-window-days config"
grep -q "min-playtime-minutes" "$CFG" || fail "Missing min-playtime-minutes config"
grep -q "min-account-age-days" "$CFG" || fail "Missing min-account-age-days config"
grep -q "^score-thresholds:" "$CFG" || fail "Missing score-thresholds config"
grep -q "discord-webhook:" "$CFG" || fail "Missing discord-webhook config section"
grep -q "url:[[:space:]]*\"\"" "$CFG" || fail "Default discord webhook URL must be empty"
for event_key in report-created report-approved report-rejected score-threshold-crossed auto-ban unban pardon reporter-penalty recovery-summary; do
  grep -q "$event_key:" "$CFG" || fail "Missing discord webhook event key: $event_key"
done

grep -R "extends JavaPlugin" src/main/java >/dev/null || fail "Main JavaPlugin class not found"
grep -R "PlayerJoinEvent" src/main/java >/dev/null || fail "PlayerJoinEvent handling not found"
grep -R "CREATE TABLE" src/main/java >/dev/null || fail "Table creation SQL not found"
grep -R "getUniqueId" src/main/java >/dev/null || fail "UUID handling not found"
grep -R "reputationban.bypass" src/main/java >/dev/null || fail "bypass permission check not found"
grep -R "OfflinePlayer.*ban\\|\\.ban(reason" src/main/java >/dev/null || fail "Profile ban API usage not found"
grep -R "\"approve\"" src/main/java/dev/modplugin/reputationban/command/ReportsCommand.java >/dev/null || fail "/reports approve handling not found"
grep -R "\"reject\"" src/main/java/dev/modplugin/reputationban/command/ReportsCommand.java >/dev/null || fail "/reports reject handling not found"
grep -R "\"history\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep history handling not found"
grep -R "\"banhistory\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep banhistory handling not found"
grep -R "\"baninfo\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep baninfo handling not found"
grep -R "\"unban\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep unban handling not found"
grep -R "\"pardon\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep pardon handling not found"
grep -R "\"help\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep help handling not found"
grep -R "\"version\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep version handling not found"
grep -R "/rep version - プラグインバージョンを表示" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep help does not mention version"
grep -R "\"help\"" src/main/java/dev/modplugin/reputationban/command/ReportsCommand.java >/dev/null || fail "/reports help handling not found"
grep -R "\"add\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep add handling not found"
grep -R "\"remove\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep remove handling not found"
grep -R "\"set\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep set handling not found"
grep -R "false_report_count[[:space:]]*=[[:space:]]*false_report_count[[:space:]]*+[[:space:]]*1" src/main/java >/dev/null || fail "false_report_count increment not found"
grep -R "report_banned_until" src/main/java/dev/modplugin/reputationban >/dev/null || fail "report_banned_until handling not found"
grep -R "scoreRecoveryEnabled\\|recoveryPointsPerDay\\|recoveryNoReportDaysRequired" src/main/java >/dev/null || fail "score-recovery config reads not found"
grep -R '"recovery"' src/main/java >/dev/null || fail "recovery score_history source_type not found"
grep -R "last_recovery_at\\|recentlyRecovered" src/main/java >/dev/null || fail "recovery duplicate prevention not found"
grep -R "isDatabaseValue\\|cancelled|all" src/main/java/dev/modplugin/reputationban/command/ReportsCommand.java >/dev/null || fail "/reports list status handling not found"
grep -R "CREATE TABLE IF NOT EXISTS audit_events" src/main/java/dev/modplugin/reputationban/database/DatabaseManager.java >/dev/null || fail "audit_events table not found"
grep -R "idx_audit_events_created\\|idx_audit_events_target_created\\|idx_audit_events_actor_created\\|idx_audit_events_type_created" src/main/java/dev/modplugin/reputationban/database/DatabaseManager.java >/dev/null || fail "audit_events indexes not found"
grep -R "class AuditService" src/main/java/dev/modplugin/reputationban/service >/dev/null || fail "AuditService not found"
grep -R "record AuditEvent" src/main/java/dev/modplugin/reputationban/model >/dev/null || fail "AuditEvent not found"
grep -R "enum AuditEventType" src/main/java/dev/modplugin/reputationban/model >/dev/null || fail "AuditEventType not found"
grep -R "\"audit\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep audit command not found"
grep -R "\"maintenance\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep maintenance command not found"
grep -R "\"backup\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep backup command not found"
grep -R "\"support\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep support command not found"
grep -R "\"bundle\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java src/main/java/dev/modplugin/reputationban/util/CommandSuggestionUtil.java >/dev/null || fail "/rep support bundle handling not found"
grep -R "MAINTENANCE_RUN" src/main/java/dev/modplugin/reputationban >/dev/null || fail "MAINTENANCE_RUN audit event not found"
grep -R "MAINTENANCE_PREVIEW" src/main/java/dev/modplugin/reputationban src/test/java/dev/modplugin/reputationban >/dev/null || fail "MAINTENANCE_PREVIEW audit event not found"
grep -R "DB_BACKUP_CREATED" src/main/java/dev/modplugin/reputationban src/test/java/dev/modplugin/reputationban >/dev/null || fail "DB_BACKUP_CREATED audit event not found"
grep -R "SUPPORT_BUNDLE_CREATED" src/main/java/dev/modplugin/reputationban src/test/java/dev/modplugin/reputationban >/dev/null || fail "SUPPORT_BUNDLE_CREATED audit event not found"
grep -R "class SupportBundleService" src/main/java/dev/modplugin/reputationban/service >/dev/null || fail "SupportBundleService not found"
grep -R "record SupportBundleResult" src/main/java/dev/modplugin/reputationban/model >/dev/null || fail "SupportBundleResult not found"
grep -R "class Redactor\\|class ConfigRedactor" src/main/java/dev/modplugin/reputationban/util >/dev/null || fail "Redactor/ConfigRedactor not found"
grep -R "ZipOutputStream" src/main/java/dev/modplugin/reputationban/service/SupportBundleService.java >/dev/null || fail "ZipOutputStream usage not found"
grep -R "reputationban.db\\|latest.log\\|debug.log\\|logs/" src/main/java/dev/modplugin/reputationban/service/SupportBundleService.java >/dev/null || fail "support bundle DB/log exclusion not found"
grep -R "class ConfigValidator\\|record ConfigValidationIssue" src/main/java/dev/modplugin/reputationban/config >/dev/null || fail "ConfigValidator/ConfigValidationIssue not found"
grep -R "class SafePathResolver" src/main/java/dev/modplugin/reputationban/util >/dev/null || fail "SafePathResolver not found"
grep -R "\"preview\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java src/main/java/dev/modplugin/reputationban/util/CommandSuggestionUtil.java >/dev/null || fail "/rep maintenance preview handling not found"
grep -R "\"confirm\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java src/main/java/dev/modplugin/reputationban/util/CommandSuggestionUtil.java >/dev/null || fail "/rep maintenance run confirm handling not found"
grep -R "RUN_REQUIRES_CONFIRMATION" src/main/java/dev/modplugin/reputationban >/dev/null || fail "/rep maintenance run must require confirmation"
grep -R "reputationban-before-maintenance\\|backupDatabase\\|backups" src/main/java/dev/modplugin/reputationban/service/AuditService.java >/dev/null || fail "maintenance backup creation not found"
grep -R "secret-scan.txt" scripts/make-review-archive.sh >/dev/null || fail "review archive secret-scan output not found"
grep -R "class CsvEscaper" src/main/java/dev/modplugin/reputationban/util >/dev/null || fail "CsvEscaper not found"
grep -q "reputationban.admin.audit:" "$YML" || fail "Missing reputationban.admin.audit permission"
grep -q "reputationban.admin.maintenance:" "$YML" || fail "Missing reputationban.admin.maintenance permission"
grep -q "reputationban.admin.diagnostics:" "$YML" || fail "Missing reputationban.admin.diagnostics permission"
grep -A12 "reputationban.admin:" "$YML" | grep -q "reputationban.admin.diagnostics: true" || fail "admin diagnostics child permission missing"
grep -R "REPORT_THRESHOLD_REACHED" src/main/java/dev/modplugin/reputationban >/dev/null || fail "REPORT_THRESHOLD_REACHED audit not found"
grep -R "REPORT_CREATED" src/main/java/dev/modplugin/reputationban/service/ReportService.java >/dev/null || fail "report-created audit not found"
grep -R "REPORT_APPROVED" src/main/java/dev/modplugin/reputationban/service/ReportService.java >/dev/null || fail "report-approved audit not found"
grep -R "REPORT_REJECTED" src/main/java/dev/modplugin/reputationban/service/ReportService.java >/dev/null || fail "report-rejected audit not found"
grep -R "SCORE_CHANGED_ADMIN" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "admin score audit not found"
grep -R "AUTO_BAN" src/main/java/dev/modplugin/reputationban/service/PunishmentService.java >/dev/null || fail "auto-ban audit not found"
grep -R "UNBAN" src/main/java/dev/modplugin/reputationban/service/PunishmentService.java >/dev/null || fail "unban audit not found"
grep -R "PARDON" src/main/java/dev/modplugin/reputationban/service/PunishmentService.java >/dev/null || fail "pardon audit not found"
grep -R "REPORTER_PENALTY" src/main/java/dev/modplugin/reputationban/service/ReportService.java >/dev/null || fail "reporter penalty audit not found"
if grep -R "discord-webhook.url\\|discordWebhookConfig().url\\|webhooks" src/main/java/dev/modplugin/reputationban/service/AuditService.java src/main/java/dev/modplugin/reputationban/util/AuditMetadata.java >/dev/null; then
  fail "Possible webhook URL audit/CSV exposure detected"
fi
grep -R "THRESHOLD_PENDING\\|threshold_pending" src/main/java/dev/modplugin/reputationban/model/ReportStatus.java >/dev/null || fail "threshold_pending status not found"
grep -R "minUniqueReportsBeforeDeduction\\|min-unique-reports-before-deduction" src/main/java/dev/modplugin/reputationban >/dev/null || fail "min-unique-reports-before-deduction read/use not found"
grep -R "reportWindowDays\\|report-window-days" src/main/java/dev/modplugin/reputationban >/dev/null || fail "report-window-days read/use not found"
grep -R "COUNT(DISTINCT reporter_uuid)" src/main/java/dev/modplugin/reputationban >/dev/null || fail "unique reporter threshold aggregation not found"
grep -R "minPlaytimeMinutes\\|min-playtime-minutes" src/main/java/dev/modplugin/reputationban >/dev/null || fail "min-playtime-minutes read/use not found"
grep -R "minAccountAgeDays\\|min-account-age-days" src/main/java/dev/modplugin/reputationban >/dev/null || fail "min-account-age-days read/use not found"
grep -R "Statistic.PLAY_ONE_MINUTE\\|getStatistic" src/main/java/dev/modplugin/reputationban >/dev/null || fail "playtime Statistic check not found"
grep -R "firstSeen\\|first_seen" src/main/java/dev/modplugin/reputationban >/dev/null || fail "first_seen account age check not found"
grep -R "ScoreThresholdPolicy\\|ScoreThresholdCrossing" src/main/java/dev/modplugin/reputationban >/dev/null || fail "score threshold policy not found"
grep -R "SCORE_THRESHOLD_CROSSED" src/main/java/dev/modplugin/reputationban >/dev/null || fail "SCORE_THRESHOLD_CROSSED notification use not found"
grep -R "threshold_pending" src/main/java/dev/modplugin/reputationban/command/ReportsCommand.java >/dev/null || fail "/reports list threshold_pending handling not found"
grep -R "threshold_pending" src/main/java/dev/modplugin/reputationban/util/CommandSuggestionUtil.java src/main/java/dev/modplugin/reputationban/model/ReportStatus.java >/dev/null || fail "threshold_pending TAB completion not found"
grep -R "ManualScoreChangeGate\\|requiresBanPermission" src/main/java/dev/modplugin/reputationban/command/RepCommand.java src/main/java/dev/modplugin/reputationban/util >/dev/null || fail "manual score ban gate logic not found"
grep -R "ReviewApprovalGate\\|hasBanPermission" src/main/java/dev/modplugin/reputationban >/dev/null || fail "/reports approve ban permission gate not found"
grep -R "isTargetProtected\\|reputationban.bypass.*isOp\\|isOp.*reputationban.bypass" src/main/java/dev/modplugin/reputationban/command/ReportsCommand.java >/dev/null || fail "approve target protection check not found"
grep -R "unbanned_at.*unbanned_by\\|unbanned_by.*unbanned_at" src/main/java >/dev/null || fail "ban unban DB update not found"
grep -R "unban_reason" src/main/java/dev/modplugin/reputationban >/dev/null || fail "unban_reason column/handling not found"
grep -R "ALTER TABLE bans ADD COLUMN unban_reason TEXT" src/main/java/dev/modplugin/reputationban/database/DatabaseManager.java >/dev/null || fail "bans unban_reason migration not found"
grep -R "unbanned_by_name TEXT" src/main/java/dev/modplugin/reputationban/database/DatabaseManager.java >/dev/null || fail "bans.unbanned_by_name column not found"
grep -R "ALTER TABLE bans ADD COLUMN unbanned_by_name TEXT" src/main/java/dev/modplugin/reputationban/database/DatabaseManager.java >/dev/null || fail "bans unbanned_by_name migration not found"
grep -R "actor.databaseActorId()" src/main/java/dev/modplugin/reputationban/service/PunishmentService.java >/dev/null || fail "unbanned_by actor.databaseActorId() storage not found"
grep -R "unbanned_by_name = ?.*actor.name()\\|actor.name()" src/main/java/dev/modplugin/reputationban/service/PunishmentService.java >/dev/null || fail "unbanned_by_name actor.name() storage not found"
grep -R "databaseActorId" src/main/java/dev/modplugin/reputationban >/dev/null || fail "CommandActor.databaseActorId usage not found"
grep -R "markActiveBansUnbanned" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep unban does not call markActiveBansUnbanned"
grep -R "punishmentService.pardon" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "/rep pardon does not call pardon service"
grep -R '"pardon"' src/main/java >/dev/null || fail "pardon score_history source_type not found"
grep -R "BanListType.PROFILE\\|ProfileBanList" src/main/java >/dev/null || fail "Profile BAN pardon API usage not found"
grep -R "TabCompleter\\|onTabComplete\\|TabExecutor" src/main/java/dev/modplugin/reputationban >/dev/null || fail "TAB completion implementation not found"
grep -R "categories().keySet()" src/main/java/dev/modplugin/reputationban/command/ReportBadTabCompleter.java >/dev/null || fail "/reportbad category completion not found"
grep -R "repSubcommands" src/main/java/dev/modplugin/reputationban/command/RepTabCompleter.java src/main/java/dev/modplugin/reputationban/util/CommandSuggestionUtil.java >/dev/null || fail "/rep subcommand completion not found"
grep -R "candidates.add(\"version\")" src/main/java/dev/modplugin/reputationban/util/CommandSuggestionUtil.java >/dev/null || fail "/rep version TAB completion not found"
grep -R "\"doctor\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java src/main/java/dev/modplugin/reputationban/util/CommandSuggestionUtil.java >/dev/null || fail "/rep doctor handling/completion not found"
grep -R "\"diagnostics\"" src/main/java/dev/modplugin/reputationban/command/RepCommand.java src/main/java/dev/modplugin/reputationban/util/CommandSuggestionUtil.java >/dev/null || fail "/rep diagnostics handling/completion not found"
grep -R "class DiagnosticService" src/main/java/dev/modplugin/reputationban/service >/dev/null || fail "DiagnosticService not found"
grep -R "record DiagnosticReport" src/main/java/dev/modplugin/reputationban/model >/dev/null || fail "DiagnosticReport not found"
grep -R "DIAGNOSTICS_RUN" src/main/java/dev/modplugin/reputationban src/test/java/dev/modplugin/reputationban >/dev/null || fail "DIAGNOSTICS_RUN audit event not found"
grep -R "pluginDataFolder\\|databaseFileExists\\|backupDirectoryWritable\\|auditExportDirectorySafe" src/main/java/dev/modplugin/reputationban/model src/main/java/dev/modplugin/reputationban/service src/main/java/dev/modplugin/reputationban/command >/dev/null || fail "Phase 11 doctor fields not found"
grep -R "reportStatuses\\|reportsSecondArgumentSuggestions" src/main/java/dev/modplugin/reputationban/command/ReportsTabCompleter.java src/main/java/dev/modplugin/reputationban/util/CommandSuggestionUtil.java >/dev/null || fail "/reports list status completion not found"
grep -R "CommandArgumentParser.parseLimit" src/main/java/dev/modplugin/reputationban/command >/dev/null || fail "explicit limit parsing not found"
grep -R "class NotificationService" src/main/java/dev/modplugin/reputationban/notification >/dev/null || fail "NotificationService not found"
grep -R "class DiscordWebhookClient" src/main/java/dev/modplugin/reputationban/notification >/dev/null || fail "DiscordWebhookClient not found"
grep -R "enum NotificationEventType" src/main/java/dev/modplugin/reputationban/notification >/dev/null || fail "NotificationEventType not found"
grep -R "java.net.http.HttpClient\\|HttpClient.newHttpClient" src/main/java/dev/modplugin/reputationban/notification >/dev/null || fail "HttpClient usage not found"
grep -R "sendAsync" src/main/java/dev/modplugin/reputationban/notification >/dev/null || fail "HttpClient sendAsync usage not found"
grep -R "JsonEscaper\\|escape(String" src/main/java/dev/modplugin/reputationban/notification >/dev/null || fail "JSON escaping not found"
grep -R "MAX_CONTENT_LENGTH\\|truncateContent" src/main/java/dev/modplugin/reputationban/notification >/dev/null || fail "Discord content length limit not found"
grep -R "lastFailureLogAt\\|rateLimitFailureLogSeconds" src/main/java/dev/modplugin/reputationban/notification >/dev/null || fail "Discord failure log rate limiting not found"
grep -R "REPORT_CREATED" src/main/java/dev/modplugin/reputationban/command/ReportBadCommand.java >/dev/null || fail "report-created notification not found"
grep -R "REPORT_APPROVED" src/main/java/dev/modplugin/reputationban/command/ReportsCommand.java >/dev/null || fail "report-approved notification not found"
grep -R "REPORT_REJECTED" src/main/java/dev/modplugin/reputationban/command/ReportsCommand.java >/dev/null || fail "report-rejected notification not found"
grep -R "AUTO_BAN" src/main/java/dev/modplugin/reputationban/service/PunishmentService.java >/dev/null || fail "auto-ban notification not found"
grep -R "UNBAN" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "unban notification not found"
grep -R "PARDON" src/main/java/dev/modplugin/reputationban/command/RepCommand.java >/dev/null || fail "pardon notification not found"
grep -R "REPORTER_PENALTY" src/main/java/dev/modplugin/reputationban/command/ReportsCommand.java >/dev/null || fail "reporter-penalty notification not found"
grep -R "setAutoCommit(false)" src/main/java >/dev/null || fail "Transactional setAutoCommit(false) pattern not found"
grep -R "commit()" src/main/java >/dev/null || fail "Transactional commit() pattern not found"
grep -R "rollback()" src/main/java >/dev/null || fail "Transactional rollback() pattern not found"

if grep -R "net\.minecraft\|org\.bukkit\.craftbukkit\|CraftPlayer\|NMS" src/main/java >/dev/null; then
  fail "NMS/CraftBukkit usage detected"
fi
if grep -R "BanList.Type.NAME\|getBanList(BanList.Type\|@SuppressWarnings(\"deprecation\")" src/main/java >/dev/null; then
  fail "Deprecated name ban usage detected"
fi
if grep -R "profileBanList\.pardon[[:space:]]*(.*targetName\|profileBanList\.pardon[[:space:]]*(.*Name\|profileBanList\.pardon[[:space:]]*(.*String" src/main/java >/dev/null; then
  fail "Deprecated name pardon API usage detected"
fi
if grep -R "discord\.com/api/webhooks" src/main/java >/dev/null; then
  fail "Hard-coded Discord webhook URL detected in Java sources"
fi
if grep -RE "https://(canary\\.|ptb\\.)?discord(app)?\\.com/api/webhooks/[0-9]+/[A-Za-z0-9_-]{20,}" src/main/java src/main/resources >/dev/null; then
  fail "Concrete Discord webhook URL detected in main sources/resources"
fi
if grep -R "logger\.\(info\|warning\|severe\|log\).*url\|url.*logger\.\(info\|warning\|severe\|log\)" src/main/java/dev/modplugin/reputationban/notification >/dev/null; then
  fail "Possible webhook URL logging detected"
fi

./gradlew clean test build --warning-mode all

JAR="build/libs/${EXPECTED_JAR_PREFIX}-${EXPECTED_VERSION}.jar"
[[ -f "$JAR" ]] || fail "Expected jar not found: $JAR"
require_command jar
jar tf "$JAR" | grep -q "plugin.yml" || fail "plugin.yml missing from jar"
jar tf "$JAR" | grep -q "dev/modplugin/reputationban/ReputationBanPlugin.class" || fail "Main class missing from jar"

grep -q "EXPECTED_VERSION=\"0.12.0\"" scripts/run-local-smoke-check.sh || fail "run-local-smoke-check.sh does not check v0.12.0"
grep -q "REPUTATIONBAN_SKIP_BUILD" scripts/run-local-smoke-check.sh || fail "run-local-smoke-check.sh does not support REPUTATIONBAN_SKIP_BUILD"
grep -q "VERSION=\"0.12.0\"" scripts/create-release-artifact.sh || fail "create-release-artifact.sh does not target v0.12.0"
grep -q "build/release" scripts/create-release-artifact.sh || fail "create-release-artifact.sh does not write build/release"
grep -q "sha256sum" scripts/create-release-artifact.sh || fail "create-release-artifact.sh does not create sha256"
grep -q "release.zip" scripts/create-release-artifact.sh || fail "create-release-artifact.sh does not create release zip"
grep -q "local-smoke-check.txt" scripts/make-review-archive.sh || fail "make-review-archive.sh does not create local-smoke-check.txt"
grep -q "./scripts/run-local-smoke-check.sh" scripts/make-review-archive.sh || fail "make-review-archive.sh does not record run-local-smoke-check.sh status"
grep -q "create-release-artifact.txt" scripts/make-review-archive.sh || fail "make-review-archive.sh does not create create-release-artifact.txt"
grep -q "./scripts/create-release-artifact.sh" scripts/make-review-archive.sh || fail "make-review-archive.sh does not record create-release-artifact status"
grep -q "secret-scan.txt" scripts/make-review-archive.sh || fail "make-review-archive.sh does not create secret-scan.txt"
grep -q "run-paper-runtime-smoke-helper" scripts/make-review-archive.sh || fail "make-review-archive.sh review signals do not mention paper runtime helper"
grep -Eq "CHANGELOG|INSTALLATION|CONFIGURATION|MIGRATION|RELEASE_READINESS" scripts/make-review-archive.sh || fail "make-review-archive.sh review signals do not mention release docs"
bash -n scripts/run-paper-runtime-smoke-helper.sh || fail "run-paper-runtime-smoke-helper.sh syntax check failed"
bash -n scripts/create-release-artifact.sh || fail "create-release-artifact.sh syntax check failed"
./scripts/create-release-artifact.sh
[[ -f "build/release/ReputationBan-0.12.0.jar" ]] || fail "release jar not found"
[[ -f "build/release/ReputationBan-0.12.0.jar.sha256" ]] || fail "release jar sha256 not found"
[[ -f "build/release/ReputationBan-0.12.0-release.zip" ]] || fail "release zip not found"
jar tf "build/release/ReputationBan-0.12.0-release.zip" | grep -q "README.md" || fail "release zip missing README.md"
if jar tf "build/release/ReputationBan-0.12.0-release.zip" | grep -E '(^|/)(config\.yml|reputationban\.db|latest\.log|debug\.log)$|(^|/)logs/' >/dev/null; then
  fail "release zip contains forbidden config, DB, or logs"
fi

if [[ "${REPUTATIONBAN_SKIP_LOCAL_SMOKE:-0}" != "1" ]]; then
  REPUTATIONBAN_SKIP_REVIEW_CODE=1 REPUTATIONBAN_SKIP_BUILD=1 ./scripts/run-local-smoke-check.sh
fi

git rev-list --count HEAD >/dev/null || fail "No commits found"

if git status --porcelain | grep -E '(^|/).*:Zone.Identifier$' >/dev/null; then
  fail "Zone.Identifier file is present in working tree"
fi

pass "Review checks completed"
