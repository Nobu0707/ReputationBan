#!/usr/bin/env bash
set -euo pipefail

# ReputationBan review archive generator.
# Usage:
#   bash scripts/make-review-archive.sh
#   bash scripts/make-review-archive.sh "Phase 16"
#
# The optional argument is an expected substring of HEAD's commit subject.
# If it does not match, the script exits before producing an archive, which
# prevents uploading a stale review tarball.

EXPECTED_SUBJECT="${1:-}"

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

HEAD_SHA="$(git rev-parse --short=12 HEAD)"
HEAD_SUBJECT="$(git show -s --format=%s HEAD)"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"
STAMP="$(date +%Y%m%d-%H%M%S)"

if [[ -n "$EXPECTED_SUBJECT" && "$HEAD_SUBJECT" != *"$EXPECTED_SUBJECT"* ]]; then
  echo "ERROR: HEAD subject does not match expected phase/commit." >&2
  echo "  expected substring: $EXPECTED_SUBJECT" >&2
  echo "  actual HEAD:        $HEAD_SHA $HEAD_SUBJECT" >&2
  exit 2
fi

OUTDIR="/tmp/reputationban-review-$HEAD_SHA-$STAMP"
ARCHIVE="$ROOT/reputationban-review-$HEAD_SHA-$STAMP.tar.gz"
LATEST="$ROOT/reputationban-review-latest.tar.gz"

rm -rf "$OUTDIR"
mkdir -p "$OUTDIR"/{meta,diff,file-diffs,files,checks,runtime-smoke,release-prep}
COMMAND_STATUS="$OUTDIR/checks/command-status.txt"
: > "$COMMAND_STATUS"
PRESERVED_MANUAL_SMOKE_DIR="$OUTDIR/meta/preserved-manual-smoke"
PRESERVED_PLAYER_REPORT_DIR="$OUTDIR/meta/preserved-player-report-runtime"
PRESERVED_PLAYER_REPORT_NAME=""

if [[ -d "$ROOT/build/manual-smoke" ]]; then
  mkdir -p "$PRESERVED_MANUAL_SMOKE_DIR"
  cp -R "$ROOT/build/manual-smoke/." "$PRESERVED_MANUAL_SMOKE_DIR/"
fi

LATEST_PLAYER_REPORT_BEFORE_CLEAN="$(find "$ROOT/build/manual-smoke" -maxdepth 2 -path '*/player-report-runtime-*/summary.txt' -type f 2>/dev/null | sort | tail -n 1 || true)"
if [[ -n "$LATEST_PLAYER_REPORT_BEFORE_CLEAN" && -f "$LATEST_PLAYER_REPORT_BEFORE_CLEAN" ]]; then
  PRESERVED_PLAYER_REPORT_NAME="$(basename "$(dirname "$LATEST_PLAYER_REPORT_BEFORE_CLEAN")")"
  mkdir -p "$PRESERVED_PLAYER_REPORT_DIR"
  cp -R "$(dirname "$LATEST_PLAYER_REPORT_BEFORE_CLEAN")/." "$PRESERVED_PLAYER_REPORT_DIR/"
fi

{
  echo "generatedAt=$STAMP"
  echo "repo=$ROOT"
  echo "branch=$BRANCH"
  echo "head=$HEAD_SHA"
  echo "subject=$HEAD_SUBJECT"
  echo "expectedSubject=$EXPECTED_SUBJECT"
} > "$OUTDIR/meta/review-info.txt"

git log --oneline -10 > "$OUTDIR/meta/git-log-oneline.txt"
git status --short > "$OUTDIR/meta/git-status-short.txt"
git show --stat --oneline --no-color HEAD > "$OUTDIR/meta/head-stat.txt"
git show --name-status --no-color HEAD > "$OUTDIR/meta/head-name-status.txt"
git diff-tree --root --no-commit-id --name-only -r HEAD > "$OUTDIR/meta/changed-files.txt"
git remote -v > "$OUTDIR/meta/git-remotes.txt"
git tag --list --sort=creatordate > "$OUTDIR/meta/tags.txt"
git show --no-color HEAD > "$OUTDIR/diff/head-full-diff.txt"

# Per-file diffs and HEAD file contents for every changed file.
while IFS= read -r file; do
  [[ -z "$file" ]] && continue

  safe="${file//\//__}"
  safe="${safe// /_}"

  git show --no-color HEAD -- "$file" > "$OUTDIR/file-diffs/${safe}.diff.txt" || true

  # Store the committed file content when it exists at HEAD.
  # Deleted files will fail here and are skipped.
  if git cat-file -e "HEAD:$file" 2>/dev/null; then
    mkdir -p "$OUTDIR/files/$(dirname "$file")"
    git show "HEAD:$file" > "$OUTDIR/files/$file" || true
  fi
done < "$OUTDIR/meta/changed-files.txt"

# Lightweight command outputs useful for review. These should never print secrets.
{
  echo "## rg ban and report paths"
  rg -n "punishIfNeeded|OfflinePlayer|\\.ban\\(|isBanned|ban_count|unban_reason|unbanned_by|score_history|TabCompleter|onTabComplete|setAutoCommit|commit\\(|rollback\\(" src/main/java/dev/modplugin/reputationban || true
  echo
  echo "## rg discord notification paths"
  rg -n "NotificationService|DiscordWebhookClient|NotificationEventType|HttpClient|sendAsync|webhook|discord" src/main/java/dev/modplugin/reputationban src/main/resources/config.yml || true
  echo
  echo "## rg review workflow"
  rg -n "ReputationBan|diff-tree --root|Zone.Identifier|gitattributes|review_code" .gitattributes .gitignore scripts || true
  echo
  echo "## rg phase 7 report safety"
  rg -n "threshold_pending|min-unique-reports-before-deduction|report-window-days|min-playtime-minutes|min-account-age-days|ScoreThreshold|SCORE_THRESHOLD_CROSSED|COUNT\\(DISTINCT" src/main/java src/test/java src/main/resources README.md docs reputationban_phase_plan.md scripts || true
  echo
  echo "## rg phase 8 audit retention"
  rg -n "audit_events|AuditService|AuditEventType|CsvEscaper|maintenance|retention|reputationban.admin.audit|reputationban.admin.maintenance|REPORT_THRESHOLD_REACHED" src/main/java src/test/java src/main/resources README.md docs reputationban_phase_plan.md scripts || true
  echo
  echo "## rg phase 9 operational hardening"
  rg -n "ConfigValidator|ConfigValidationIssue|SafePathResolver|MAINTENANCE_PREVIEW|maintenance preview|run confirm|backup|runtime-smoke|secret-scan|CommandActor" src/main/java src/test/java src/main/resources README.md docs reputationban_phase_plan.md scripts || true
  echo
  echo "## rg phase 10 diagnostics readiness"
  rg -n "unbanned_by_name|databaseActorId|DiagnosticService|DiagnosticReport|DIAGNOSTICS_RUN|doctor|diagnostics|local-smoke-check" src/main/java src/test/java src/main/resources README.md docs reputationban_phase_plan.md scripts || true
  echo
  echo "## rg phase 11 release readiness"
  rg -n "CHANGELOG|INSTALLATION|CONFIGURATION|MIGRATION|RELEASE_READINESS|run-paper-runtime-smoke-helper|/rep version|\"version\"|version" src/main/java src/test/java src/main/resources README.md CHANGELOG.md docs reputationban_phase_plan.md scripts || true
  echo
  echo "## rg phase 12 support and release artifacts"
  rg -n "SupportBundleService|SupportBundleResult|SUPPORT_BUNDLE_CREATED|DB_BACKUP_CREATED|Redactor|ConfigRedactor|ZipOutputStream|create-release-artifact|support bundle|/rep backup" src/main/java src/test/java src/main/resources README.md CHANGELOG.md docs reputationban_phase_plan.md scripts || true
  echo
  echo "## rg phase 13 release candidate hardening"
  rg -n "verify-release-artifact|record-paper-runtime-smoke-result|PAPER_RUNTIME_SMOKE_REPORT_TEMPLATE|SECURITY_REDACTION|SupportBundleSafety|PathRedactor|redactSecretLikeValue|release.zip.sha256" src/main/java src/test/java src/main/resources README.md CHANGELOG.md docs reputationban_phase_plan.md scripts || true
  echo
  echo "## rg phase 14 documentation localization"
  rg -n "日本語化|README|INSTALLATION|CONFIGURATION|RELEASE_READINESS|SUPPORT_BUNDLE|SECURITY_REDACTION|phase-14|0\\.14\\.0" README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts src/main/resources/plugin.yml || true
  echo
  echo "## rg phase 15 release candidate checks"
  rg -n "check-docs-localization|RELEASE_CANDIDATE_CHECKLIST|status=NOT_RUN|docs-localization|0\\.15\\.0|phase-15|Phase 15|release candidate" README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts src/main/resources/plugin.yml || true
  echo
  echo "## rg phase 16 integrations"
  rg -n "LuckPerms|CoreProtect|IntegrationService|LuckPermsIntegration|CoreProtectIntegration|report_context|COREPROTECT_CONTEXT_CAPTURED|/rep integrations|performLookup|performRollback|performRestore|performPurge" src/main/java src/test/java src/main/resources README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts settings.gradle.kts || true
  echo
  echo "## rg phase 16a optional dependency class loading"
  rg -n "LuckPermsReflectionAdapter|CoreProtectReflectionAdapter|Class\\.forName|getRegistration|getPlugin\\(\"CoreProtect\"\\)|NoClassDefFoundError|import net\\.luckperms|import net\\.coreprotect" src/main/java src/test/java src/main/resources README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts settings.gradle.kts || true
  echo
  echo "## rg phase 17 integration diagnostics"
  rg -n "reports evidence|integrations test|check-optional-dependency-safety|record-integration-runtime-smoke-result|INTEGRATION_RUNTIME_SMOKE_CHECKLIST|CoreProtect metadata|LuckPerms metadata|applyWeightToDeduction|ReportContextFormatter|/reports evidence|/rep integrations test" src/main/java src/test/java src/main/resources README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts settings.gradle.kts || true
  echo
  echo "## rg phase 18 WorldGuard integration"
  rg -n "WorldGuard|WorldEdit|WorldGuardIntegration|WorldGuardReflectionAdapter|WORLDGUARD_CONTEXT_CAPTURED|provider = worldguard|provider=worldguard|\"worldguard\"|BukkitAdapter|ApplicableRegionSet|ProtectedRegion|addRegion|removeRegion|setFlag|saveChanges" src/main/java src/test/java src/main/resources README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts settings.gradle.kts || true
  echo
  echo "## rg phase 19 GriefPrevention integration"
  rg -n "GriefPrevention|GriefPreventionIntegration|GriefPreventionReflectionAdapter|GRIEFPREVENTION_CONTEXT_CAPTURED|provider = griefprevention|provider=griefprevention|\"griefprevention\"|getClaimAt|createClaim|deleteClaim|resizeClaim|changeClaimOwner|setOwner|setManagers|setBuilders|setContainers|setAccessors" src/main/java src/test/java src/main/resources README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts settings.gradle.kts || true
  echo
  echo "## rg phase 20 PlaceholderAPI integration"
  rg -n "PlaceholderAPI|PlaceholderApiIntegration|ReputationBanPlaceholderExpansion|PlaceholderExpansion|PlaceholderCacheService|PlaceholderValueProvider|PlayerReputationSummary|/rep placeholders|placeholderapi" src/main/java src/test/java src/main/resources README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts settings.gradle.kts || true
  echo
  echo "## rg phase 21 DiscordSRV integration"
  rg -n "DiscordSRV|DiscordSrvIntegration|DiscordSrvReflectionAdapter|DISCORDSRV_CONTEXT_CAPTURED|provider = discordsrv|provider=discordsrv|\"discordsrv\"|getAccountLinkManager|getDiscordId|sendMessage|queue|net\\.dv8tion|github\\.scarsz\\.discordsrv" src/main/java src/test/java src/main/resources README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts settings.gradle.kts || true
  echo
  echo "## rg phase 22 integration runtime readiness"
  rg -n "check-integration-runtime-readiness|run-integration-runtime-smoke-helper|HOLD_FOR_INTEGRATION_RUNTIME_SMOKE|DiscordSrvReflectionAdapter|getPlugin\\(\"DiscordSRV\"\\)|getDestinationTextChannelForGameChannelName|runTask|DiscordSRV notification" src/main/java src/test/java src/main/resources README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts settings.gradle.kts || true
  echo
  echo "## rg phase 23 Paper runtime smoke automation"
  rg -n "run-paper-runtime-smoke|check-paper-runtime-readiness|HOLD_FOR_PAPER_RUNTIME_SMOKE|paper-runtime-smoke-auto|paper-runtime-readiness|REPUTATIONBAN_PAPER_DIR|REPUTATIONBAN_PAPER_START_SCRIPT|REPUTATIONBAN_SCREEN_NAME|screen -ls|screen -S|start.sh|paper-26.1.2|0\\.23\\.0" README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts src/main/resources/plugin.yml || true
  echo
  echo "## rg phase 24 integration runtime smoke automation"
  rg -n "run-integration-runtime-smoke|integration-runtime-smoke-auto|runtime-smoke/integration-runtime-latest|runtime-smoke/paper-runtime-latest|REPUTATIONBAN_INTEGRATION_PLUGIN_DIR|REPUTATIONBAN_INTEGRATION_RESTORE_PLUGINS|PaperPlugins|staged-plugins|plugin-restore|reputationban-integration-smoke|0\\.24\\.0" README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts src/main/resources/plugin.yml || true
  echo
  echo "## rg phase 27 player report runtime smoke result recording"
  rg -n "player-report-runtime|PLAYER_REPORT_RUNTIME_SMOKE_CHECKLIST|check-player-report-runtime-readiness|record-player-report-runtime-smoke-result|manualConfirmed|manual-checklist|--manual-confirmed|HOLD_FOR_PLAYER_REPORT_RUNTIME_SMOKE|/reportbad|/reports evidence|report_context|0\\.27\\.0" README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts src/main/resources/plugin.yml || true
  echo
  echo "## rg phase 29 v1 final artifact preparation"
  rg -n "check-v1-release-gates|generate-v1-go-no-go-report|generate-v1-release-notes|READY_FOR_V1_RELEASE|READY_FOR_V1_RELEASE_WITH_DISCORDSRV_WARNING|V1_RELEASE_PLAN|V1_RELEASE_EXECUTION_PLAN|ReputationBan-v1-go-no-go-report|ReputationBan-v1\\.0\\.0-release-notes|v1 release gates|Go/No-Go|Tag status|GitHub Release status|carriedForwardFrom" README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts src/main/resources/plugin.yml || true
  echo
  echo "## rg phase 30 v1 tag and GitHub Release draft preparation"
  rg -n "phase-30|Phase 30|v1\\.0\\.0 tag|GitHub Release draft|draft=false|release-draft command|v1-tag-status|github-release-draft-status|release-draft-assets|V1_GITHUB_RELEASE_DRAFT_MANUAL|CREATED_MATCHES_HEAD" README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts src/main/resources/plugin.yml || true
  echo
  echo "## rg phase 31 v1 GitHub Release publish"
  rg -n "phase-31|Phase 31|GitHub Release: published|releasePublished|github-release-status-after-publish|release-assets-after-publish|v1-release-publish-status|CREATED_BEHIND_HEAD_ALLOWED|isDraft=false|isPrerelease=false" README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts src/main/resources/plugin.yml || true
  echo
  echo "## rg phase 31a published release notes consistency"
  rg -n "phase-31a|Phase 31a|release-notes-body-check|GitHub Release status: PUBLISHED|RELEASED_WITH_DISCORDSRV_WARNING|Post-release monitoring / bugfix intake" README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts src/main/resources/plugin.yml || true
  echo
  echo "## rg phase 32 post-release monitoring"
  rg -n "phase-32|Phase 32|POST_RELEASE_MONITORING|BUGFIX_INTAKE|V1_0_1_CANDIDATES|post-release monitoring|bugfix intake|v1\\.0\\.1 candidates|Post-release documentation updates" README.md CHANGELOG.md docs reputationban_phase_plan.md scripts build.gradle.kts src/main/resources/plugin.yml || true
} > "$OUTDIR/checks/rg-review-signals.txt"

{
  echo "## secret scan patterns"
  echo "Patterns: discord.com/api/webhooks, webhook-url, webhookUrl, password, sessionId, token, secret"
  echo
  rg -n -i "discord\\.com/api/webhooks|webhook-url|webhookUrl|password|sessionId|token|secret" \
    src/main src/test scripts docs README.md reputationban_phase_plan.md build.gradle.kts settings.gradle.kts 2>/dev/null \
    | sed -E 's#https://(canary\.|ptb\.)?discord(app)?\.com/api/webhooks/[0-9]+/[A-Za-z0-9_-]+#[REDACTED_DISCORD_WEBHOOK]#g' || true
} > "$OUTDIR/checks/secret-scan.txt"

run_logged() {
  local name="$1"
  local output="$2"
  shift 2
  set +e
  "$@" > "$output" 2>&1
  local code=$?
  set -e
  echo "$name=$code" >> "$COMMAND_STATUS"
}

run_logged "git-diff-check" "$OUTDIR/checks/git-diff-check.txt" git diff --check

run_logged "gradle-clean-test-build" \
  "$OUTDIR/checks/gradle-clean-test-build.txt" \
  ./gradlew clean test build --warning-mode all

if [[ -d "$PRESERVED_MANUAL_SMOKE_DIR" ]]; then
  mkdir -p "$ROOT/build/manual-smoke"
  cp -R "$PRESERVED_MANUAL_SMOKE_DIR/." "$ROOT/build/manual-smoke/"
  echo "preserve-manual-smoke=restored" >> "$COMMAND_STATUS"
else
  echo "preserve-manual-smoke=not-found" >> "$COMMAND_STATUS"
fi

if [[ -x "$ROOT/scripts/review_code.sh" ]]; then
  run_logged "review-code" "$OUTDIR/checks/review-code.txt" "$ROOT/scripts/review_code.sh"
else
  echo "review-code=missing" >> "$COMMAND_STATUS"
fi

if [[ -x "$ROOT/scripts/check-docs-localization.sh" ]]; then
  run_logged "./scripts/check-docs-localization.sh" "$OUTDIR/checks/docs-localization.txt" "$ROOT/scripts/check-docs-localization.sh"
else
  echo "./scripts/check-docs-localization.sh=missing" >> "$COMMAND_STATUS"
fi

if [[ -x "$ROOT/scripts/check-optional-dependency-safety.sh" ]]; then
  run_logged "./scripts/check-optional-dependency-safety.sh" "$OUTDIR/checks/optional-dependency-safety.txt" "$ROOT/scripts/check-optional-dependency-safety.sh"
else
  echo "./scripts/check-optional-dependency-safety.sh=missing" >> "$COMMAND_STATUS"
fi

if [[ -n "$PRESERVED_PLAYER_REPORT_NAME" && -f "$PRESERVED_PLAYER_REPORT_DIR/summary.txt" ]]; then
  if ! find "$ROOT/build/manual-smoke" -maxdepth 2 -path '*/player-report-runtime-*/summary.txt' -type f 2>/dev/null | grep -q .; then
    mkdir -p "$ROOT/build/manual-smoke/$PRESERVED_PLAYER_REPORT_NAME"
    cp -R "$PRESERVED_PLAYER_REPORT_DIR/." "$ROOT/build/manual-smoke/$PRESERVED_PLAYER_REPORT_NAME/"
    echo "preserve-player-report-runtime=restored:$PRESERVED_PLAYER_REPORT_NAME" >> "$COMMAND_STATUS"
  else
    echo "preserve-player-report-runtime=not-needed" >> "$COMMAND_STATUS"
  fi
else
  echo "preserve-player-report-runtime=not-found" >> "$COMMAND_STATUS"
fi

if [[ -x "$ROOT/scripts/run-paper-runtime-smoke.sh" ]]; then
  run_logged "./scripts/run-paper-runtime-smoke.sh" "$OUTDIR/checks/paper-runtime-smoke-auto.txt" "$ROOT/scripts/run-paper-runtime-smoke.sh"
else
  echo "./scripts/run-paper-runtime-smoke.sh=missing" >> "$COMMAND_STATUS"
fi

if [[ -x "$ROOT/scripts/check-paper-runtime-readiness.sh" ]]; then
  run_logged "./scripts/check-paper-runtime-readiness.sh" "$OUTDIR/checks/paper-runtime-readiness.txt" "$ROOT/scripts/check-paper-runtime-readiness.sh"
else
  echo "./scripts/check-paper-runtime-readiness.sh=missing" >> "$COMMAND_STATUS"
fi

if [[ -x "$ROOT/scripts/run-integration-runtime-smoke.sh" ]]; then
  run_logged "./scripts/run-integration-runtime-smoke.sh" "$OUTDIR/checks/integration-runtime-smoke-auto.txt" "$ROOT/scripts/run-integration-runtime-smoke.sh"
else
  echo "./scripts/run-integration-runtime-smoke.sh=missing" >> "$COMMAND_STATUS"
fi

if [[ -x "$ROOT/scripts/check-integration-runtime-readiness.sh" ]]; then
  run_logged "./scripts/check-integration-runtime-readiness.sh" "$OUTDIR/checks/integration-runtime-readiness.txt" "$ROOT/scripts/check-integration-runtime-readiness.sh"
else
  echo "./scripts/check-integration-runtime-readiness.sh=missing" >> "$COMMAND_STATUS"
fi

if [[ -x "$ROOT/scripts/check-player-report-runtime-readiness.sh" ]]; then
  run_logged "./scripts/check-player-report-runtime-readiness.sh" "$OUTDIR/checks/player-report-runtime-readiness.txt" "$ROOT/scripts/check-player-report-runtime-readiness.sh"
else
  echo "./scripts/check-player-report-runtime-readiness.sh=missing" >> "$COMMAND_STATUS"
fi

if [[ -f "$ROOT/scripts/run-integration-runtime-smoke-helper.sh" ]]; then
  run_logged "bash -n scripts/run-integration-runtime-smoke-helper.sh" "$OUTDIR/checks/integration-runtime-smoke-helper-syntax.txt" bash -n "$ROOT/scripts/run-integration-runtime-smoke-helper.sh"
else
  echo "bash -n scripts/run-integration-runtime-smoke-helper.sh=missing" >> "$COMMAND_STATUS"
fi

LATEST_SMOKE="$(find "$ROOT/build/manual-smoke" -maxdepth 2 -path '*/paper-runtime-*/summary.txt' -type f 2>/dev/null | sort | tail -n 1 || true)"
if [[ -n "$LATEST_SMOKE" && -f "$LATEST_SMOKE" ]]; then
  cp "$LATEST_SMOKE" "$OUTDIR/checks/latest-paper-runtime-smoke-summary.txt"
else
  {
    echo "status=NOT_RUN"
    echo "message=No paper runtime smoke summary found."
    echo "nextStep=Run scripts/run-paper-runtime-smoke.sh or record manual results with scripts/record-paper-runtime-smoke-result.sh"
  } > "$OUTDIR/checks/latest-paper-runtime-smoke-summary.txt"
fi

LATEST_INTEGRATION_SMOKE="$(find "$ROOT/build/manual-smoke" -maxdepth 2 -path '*/integration-runtime-*/summary.txt' -type f 2>/dev/null | sort | tail -n 1 || true)"
if [[ -n "$LATEST_INTEGRATION_SMOKE" && -f "$LATEST_INTEGRATION_SMOKE" ]]; then
  cp "$LATEST_INTEGRATION_SMOKE" "$OUTDIR/checks/latest-integration-runtime-smoke-summary.txt"
else
  {
    echo "status=NOT_RUN"
    echo "message=No integration runtime smoke summary found."
    echo "nextStep=Run docs/INTEGRATION_RUNTIME_SMOKE_CHECKLIST.md and record results with scripts/record-integration-runtime-smoke-result.sh"
  } > "$OUTDIR/checks/latest-integration-runtime-smoke-summary.txt"
fi

LATEST_PLAYER_REPORT_SMOKE="$(find "$ROOT/build/manual-smoke" -maxdepth 2 -path '*/player-report-runtime-*/summary.txt' -type f 2>/dev/null | sort | tail -n 1 || true)"
if [[ -n "$LATEST_PLAYER_REPORT_SMOKE" && -f "$LATEST_PLAYER_REPORT_SMOKE" ]]; then
  cp "$LATEST_PLAYER_REPORT_SMOKE" "$OUTDIR/checks/latest-player-report-runtime-smoke-summary.txt"
else
  {
    echo "status=NOT_RUN"
    echo "result=NOT_RUN"
    echo "version=1.0.0"
    echo "jar=build/libs/ReputationBan-1.0.0.jar"
    echo "jarSha256=missing"
    echo "reporter="
    echo "target="
    echo "reportId="
    echo "note=No player report runtime smoke summary found. Do not mark PASS without two real players."
    echo "createdAt=$(date -Iseconds)"
  } > "$OUTDIR/checks/latest-player-report-runtime-smoke-summary.txt"
fi

if [[ -x "$ROOT/scripts/check-runtime-smoke-consistency.sh" ]]; then
  run_logged "./scripts/check-runtime-smoke-consistency.sh" "$OUTDIR/checks/runtime-smoke-consistency.txt" "$ROOT/scripts/check-runtime-smoke-consistency.sh" --checks-dir "$OUTDIR/checks"
else
  echo "./scripts/check-runtime-smoke-consistency.sh=missing" >> "$COMMAND_STATUS"
fi

if [[ -x "$ROOT/scripts/check-v1-release-gates.sh" ]]; then
  run_logged "./scripts/check-v1-release-gates.sh" "$OUTDIR/checks/v1-release-gates.txt" env REPUTATIONBAN_ALLOW_V1_TAG_BEHIND_HEAD=1 "$ROOT/scripts/check-v1-release-gates.sh"
else
  echo "./scripts/check-v1-release-gates.sh=missing" >> "$COMMAND_STATUS"
fi

if [[ -x "$ROOT/scripts/generate-v1-go-no-go-report.sh" ]]; then
  run_logged "./scripts/generate-v1-go-no-go-report.sh" "$OUTDIR/checks/generate-v1-go-no-go-report.txt" "$ROOT/scripts/generate-v1-go-no-go-report.sh"
else
  echo "./scripts/generate-v1-go-no-go-report.sh=missing" >> "$COMMAND_STATUS"
fi

if [[ -x "$ROOT/scripts/generate-v1-release-notes.sh" ]]; then
  run_logged "./scripts/generate-v1-release-notes.sh" "$OUTDIR/checks/generate-v1-release-notes.txt" "$ROOT/scripts/generate-v1-release-notes.sh"
else
  echo "./scripts/generate-v1-release-notes.sh=missing" >> "$COMMAND_STATUS"
fi

if [[ -x "$ROOT/scripts/generate-v1-release-notes-draft.sh" ]]; then
  run_logged "./scripts/generate-v1-release-notes-draft.sh" "$OUTDIR/checks/generate-v1-release-notes-draft.txt" "$ROOT/scripts/generate-v1-release-notes-draft.sh"
fi

{
  notes="$ROOT/build/release/ReputationBan-v1.0.0-release-notes.md"
  report="$ROOT/build/release/ReputationBan-v1-go-no-go-report.md"
  body_file="$OUTDIR/checks/github-release-body.txt"
  if command -v gh >/dev/null 2>&1; then
    gh release view v1.0.0 --json body --jq .body > "$body_file" 2>/dev/null || : > "$body_file"
  else
    : > "$body_file"
  fi

  if grep -E "DRAFT_TO_CREATE" "$notes" "$report" "$body_file" >/dev/null 2>&1; then
    echo "DRAFT_TO_CREATE=present"
  else
    echo "DRAFT_TO_CREATE=absent"
  fi

  if grep -E "公開はまだ|draft 作成まで|Phase 30 creates" "$notes" "$report" "$body_file" >/dev/null 2>&1; then
    echo "prePublishText=present"
  else
    echo "prePublishText=absent"
  fi

  if grep -q "GitHub Release status: PUBLISHED" "$notes" 2>/dev/null && grep -q "GitHub Release status: PUBLISHED" "$report" 2>/dev/null; then
    echo "releaseStatus=PUBLISHED"
  else
    echo "releaseStatus=unknown"
  fi
} > "$OUTDIR/checks/release-notes-body-check.txt"

{
  for file in \
    "docs/POST_RELEASE_MONITORING.md" \
    "docs/BUGFIX_INTAKE.md" \
    "docs/V1_0_1_CANDIDATES.md" \
    "docs/phase-32.md"; do
    if [[ -f "$ROOT/$file" ]]; then
      echo "$file=present"
    else
      echo "$file=missing"
    fi
  done
  if grep -q "POST_RELEASE_MONITORING.md" "$ROOT/README.md" \
    && grep -q "BUGFIX_INTAKE.md" "$ROOT/README.md" \
    && grep -q "V1_0_1_CANDIDATES.md" "$ROOT/README.md"; then
    echo "README-post-release-docs=present"
  else
    echo "README-post-release-docs=missing"
  fi
} > "$OUTDIR/checks/post-release-monitoring-docs.txt"

{
  local_tag="$(git tag --list "v1.0.0" || true)"
  head_commit="$(git rev-parse HEAD)"
  if [[ -n "$local_tag" ]]; then
    local_tag_commit="$(git rev-list -n 1 v1.0.0)"
  else
    local_tag_commit=""
  fi
  remote_line="$(git ls-remote --tags origin "refs/tags/v1.0.0^{}" 2>/dev/null | tail -n 1 || true)"
  if [[ -z "$remote_line" ]]; then
    remote_line="$(git ls-remote --tags origin "refs/tags/v1.0.0" 2>/dev/null | tail -n 1 || true)"
  fi
  remote_tag_commit="$(printf '%s\n' "$remote_line" | awk '{print $1}')"
  echo "headCommit=$head_commit"
  echo "localTag=${local_tag:-missing}"
  echo "localTagCommit=${local_tag_commit:-missing}"
  echo "remoteTag=$([[ -n "$remote_tag_commit" ]] && echo "v1.0.0" || echo "missing")"
  echo "remoteTagCommit=${remote_tag_commit:-missing}"
  if [[ -n "$local_tag_commit" && "$local_tag_commit" == "$head_commit" && -n "$remote_tag_commit" && "$remote_tag_commit" == "$head_commit" ]]; then
    echo "matchesHead=true"
  else
    echo "matchesHead=false"
  fi
  if [[ -n "$local_tag_commit" ]] && git merge-base --is-ancestor "$local_tag_commit" "$head_commit"; then
    echo "localTagIsAncestorOfHead=true"
  else
    echo "localTagIsAncestorOfHead=false"
  fi
  echo "phase30Commit=b422e72ec5a917cdc04dee902e96a0cef190026c"
  if [[ "$local_tag_commit" == "b422e72ec5a917cdc04dee902e96a0cef190026c" && "$remote_tag_commit" == "b422e72ec5a917cdc04dee902e96a0cef190026c" ]]; then
    echo "pointsToPhase30Commit=true"
  else
    echo "pointsToPhase30Commit=false"
  fi
} > "$OUTDIR/checks/v1-tag-status.txt"

{
  echo "ReputationBan-1.0.0.jar"
  echo "ReputationBan-1.0.0.jar.sha256"
  echo "ReputationBan-1.0.0-release.zip"
  echo "ReputationBan-1.0.0-release.zip.sha256"
} > "$OUTDIR/checks/release-draft-assets.txt"

{
  if command -v gh >/dev/null 2>&1; then
    echo "ghAvailable=true"
    set +e
    release_json="$(gh release view v1.0.0 --json tagName,name,isDraft,isPrerelease,url,assets 2>&1)"
    release_code=$?
    set -e
    if [[ "$release_code" == "0" ]]; then
      echo "releaseExists=true"
      echo "isDraft=$([[ "$release_json" == *'"isDraft":true'* ]] && echo true || echo false)"
      echo "isPrerelease=$([[ "$release_json" == *'"isPrerelease":true'* ]] && echo true || echo false)"
      url="$(printf '%s\n' "$release_json" | sed -n 's/.*"url":"\([^"]*\)".*/\1/p')"
      echo "url=${url:-unknown}"
      for asset in \
        "ReputationBan-1.0.0.jar" \
        "ReputationBan-1.0.0.jar.sha256" \
        "ReputationBan-1.0.0-release.zip" \
        "ReputationBan-1.0.0-release.zip.sha256"; do
        if [[ "$release_json" == *"\"name\":\"$asset\""* ]]; then
          echo "asset:${asset}=present"
        else
          echo "asset:${asset}=missing"
        fi
      done
    else
      echo "releaseExists=unknown"
      echo "isDraft=unknown"
      echo "isPrerelease=unknown"
      echo "url=unknown"
      echo "ghError=${release_json//$'\n'/ }"
      if [[ -f "$ROOT/docs/V1_GITHUB_RELEASE_DRAFT_MANUAL.md" ]]; then
        echo "manualDraftInstructions=docs/V1_GITHUB_RELEASE_DRAFT_MANUAL.md"
      fi
    fi
  else
    echo "ghAvailable=false"
    echo "releaseExists=unknown"
    if [[ -f "$ROOT/docs/V1_GITHUB_RELEASE_DRAFT_MANUAL.md" ]]; then
      echo "manualDraftInstructions=docs/V1_GITHUB_RELEASE_DRAFT_MANUAL.md"
    else
      echo "manualDraftInstructions=missing"
    fi
  fi
} > "$OUTDIR/checks/github-release-draft-status.txt"

{
  echo "ReputationBan-1.0.0.jar"
  echo "ReputationBan-1.0.0.jar.sha256"
  echo "ReputationBan-1.0.0-release.zip"
  echo "ReputationBan-1.0.0-release.zip.sha256"
} > "$OUTDIR/checks/release-assets-after-publish.txt"

{
  if command -v gh >/dev/null 2>&1; then
    echo "ghAvailable=true"
    set +e
    release_json="$(gh release view v1.0.0 --json tagName,name,isDraft,isPrerelease,url,assets 2>&1)"
    release_code=$?
    set -e
    if [[ "$release_code" == "0" ]]; then
      tag_name="$(printf '%s\n' "$release_json" | sed -n 's/.*"tagName":"\([^"]*\)".*/\1/p')"
      url="$(printf '%s\n' "$release_json" | sed -n 's/.*"url":"\([^"]*\)".*/\1/p')"
      is_draft="$([[ "$release_json" == *'"isDraft":true'* ]] && echo true || echo false)"
      is_prerelease="$([[ "$release_json" == *'"isPrerelease":true'* ]] && echo true || echo false)"
      echo "releaseExists=true"
      echo "releasePublished=$([[ "$is_draft" == "false" ]] && echo true || echo false)"
      echo "tagName=${tag_name:-unknown}"
      echo "isDraft=$is_draft"
      echo "isPrerelease=$is_prerelease"
      echo "url=${url:-unknown}"
      for asset in \
        "ReputationBan-1.0.0.jar" \
        "ReputationBan-1.0.0.jar.sha256" \
        "ReputationBan-1.0.0-release.zip" \
        "ReputationBan-1.0.0-release.zip.sha256"; do
        if [[ "$release_json" == *"\"name\":\"$asset\""* ]]; then
          echo "asset:${asset}=present"
        else
          echo "asset:${asset}=missing"
        fi
      done
    else
      echo "releaseExists=unknown"
      echo "releasePublished=unknown"
      echo "tagName=unknown"
      echo "isDraft=unknown"
      echo "isPrerelease=unknown"
      echo "url=unknown"
      echo "ghError=${release_json//$'\n'/ }"
    fi
  else
    echo "ghAvailable=false"
    echo "releaseExists=unknown"
    echo "releasePublished=unknown"
  fi
} > "$OUTDIR/checks/github-release-status-after-publish.txt"

cp "$OUTDIR/checks/github-release-status-after-publish.txt" "$OUTDIR/checks/v1-release-publish-status.txt"

if [[ -x "$ROOT/scripts/run-local-smoke-check.sh" ]]; then
  run_logged "local-smoke-check" "$OUTDIR/checks/local-smoke-check.txt" env REPUTATIONBAN_SKIP_REVIEW_CODE=1 REPUTATIONBAN_SKIP_BUILD=1 "$ROOT/scripts/run-local-smoke-check.sh"
else
  echo "local-smoke-check=missing" >> "$COMMAND_STATUS"
fi

if [[ -x "$ROOT/scripts/create-release-artifact.sh" ]]; then
  run_logged "create-release-artifact" "$OUTDIR/checks/create-release-artifact.txt" "$ROOT/scripts/create-release-artifact.sh"
else
  echo "create-release-artifact=missing" >> "$COMMAND_STATUS"
fi

if [[ -x "$ROOT/scripts/verify-release-artifact.sh" ]]; then
  run_logged "verify-release-artifact" "$OUTDIR/checks/verify-release-artifact.txt" "$ROOT/scripts/verify-release-artifact.sh"
else
  echo "verify-release-artifact=missing" >> "$COMMAND_STATUS"
fi

for release_prep_file in \
  "$ROOT/build/release/ReputationBan-v1-go-no-go-report.md" \
  "$ROOT/build/release/ReputationBan-v1.0.0-release-notes.md" \
  "$ROOT/build/release/ReputationBan-v1.0.0-release-notes-draft.md" \
  "$ROOT/docs/V1_RELEASE_EXECUTION_PLAN.md" \
  "$ROOT/docs/V1_GITHUB_RELEASE_DRAFT_MANUAL.md"; do
  if [[ -f "$release_prep_file" ]]; then
    cp "$release_prep_file" "$OUTDIR/release-prep/"
  fi
done

if compgen -G "$ROOT/build/test-results/test/*.xml" >/dev/null; then
  {
    echo "JUnit XML files:"
    find "$ROOT/build/test-results/test" -type f -name '*.xml' -print | sort
    echo
    echo "Suite summary:"
    grep -h "<testsuite " "$ROOT"/build/test-results/test/*.xml \
      | sed -E 's/.*name="([^"]+)".*tests="([^"]+)".*skipped="([^"]+)".*failures="([^"]+)".*errors="([^"]+)".*/\1 tests=\2 skipped=\3 failures=\4 errors=\5/' || true
  } > "$OUTDIR/checks/junit-summary.txt"
fi

if [[ -d "$ROOT/build/libs" ]]; then
  find "$ROOT/build/libs" -maxdepth 1 -type f -print | sort > "$OUTDIR/checks/built-jars.txt"
  if [[ -f "$ROOT/build/libs/ReputationBan-1.0.0.jar" ]]; then
    (cd "$ROOT" && sha256sum build/libs/ReputationBan-1.0.0.jar) > "$OUTDIR/checks/jar-sha256.txt"
  fi
fi

if [[ -d "$ROOT/build/release" ]]; then
  find "$ROOT/build/release" -maxdepth 1 -type f -print | sort > "$OUTDIR/checks/release-artifacts.txt"
else
  echo "No build/release directory found." > "$OUTDIR/checks/release-artifacts.txt"
fi

redact_runtime_tail() {
  sed -E \
    -e 's#https://(canary\.|ptb\.)?discord(app)?\.com/api/webhooks/[0-9]+/[A-Za-z0-9_-]+#[REDACTED_DISCORD_WEBHOOK]#g' \
    -e 's#(webhook-url|webhookUrl|token|secret|password|sessionId)([[:space:]]*[:=][[:space:]]*)[^[:space:]]+#\1\2[REDACTED]#Ig'
}

copy_runtime_smoke_latest() {
  local pattern="$1"
  local dest_name="$2"
  shift 2
  local latest_summary
  latest_summary="$(find "$ROOT/build/manual-smoke" -maxdepth 2 -path "*/${pattern}-*/summary.txt" -type f 2>/dev/null | sort | tail -n 1 || true)"
  local dest="$OUTDIR/runtime-smoke/$dest_name"
  mkdir -p "$dest"
  if [[ -z "$latest_summary" || ! -f "$latest_summary" ]]; then
    {
      echo "status=NOT_RUN"
      echo "result=NOT_RUN"
      if [[ "$dest_name" == "player-report-runtime-latest" ]]; then
        echo "version=1.0.0"
        echo "jar=build/libs/ReputationBan-1.0.0.jar"
        echo "jarSha256=missing"
        echo "reporter="
        echo "target="
        echo "reportId="
        echo "note=No player report runtime smoke summary found. Do not mark PASS without two real players."
        echo "createdAt=$(date -Iseconds)"
      fi
    } > "$dest/summary.txt"
    return 0
  fi

  local latest_dir
  latest_dir="$(dirname "$latest_summary")"
  local file
  for file in "$@"; do
    if [[ -f "$latest_dir/$file" ]]; then
      cp "$latest_dir/$file" "$dest/$file"
    fi
  done
  if [[ -f "$latest_dir/server.log" ]]; then
    tail -n 500 "$latest_dir/server.log" | redact_runtime_tail > "$dest/server.log.tail.txt"
  else
    echo "server.log not found" > "$dest/server.log.tail.txt"
  fi
}

copy_runtime_smoke_latest "paper-runtime" "paper-runtime-latest" \
  summary.txt commands.txt environment.txt
copy_runtime_smoke_latest "integration-runtime" "integration-runtime-latest" \
  summary.txt commands.txt environment.txt staged-plugins.txt plugin-restore.txt integration-status.txt
copy_runtime_smoke_latest "player-report-runtime" "player-report-runtime-latest" \
  summary.txt manual-checklist.txt

tar -czf "$ARCHIVE" -C "$(dirname "$OUTDIR")" "$(basename "$OUTDIR")"
cp "$ARCHIVE" "$LATEST"

echo "Created:"
echo "  $ARCHIVE"
echo "  $LATEST"
echo
echo "HEAD:"
echo "  $HEAD_SHA $HEAD_SUBJECT"
echo
echo "Changed files:"
cat "$OUTDIR/meta/changed-files.txt"
