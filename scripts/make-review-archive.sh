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
mkdir -p "$OUTDIR"/{meta,diff,file-diffs,files,checks}
COMMAND_STATUS="$OUTDIR/checks/command-status.txt"
: > "$COMMAND_STATUS"

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
  if [[ -f "$ROOT/build/libs/ReputationBan-0.21.0.jar" ]]; then
    (cd "$ROOT" && sha256sum build/libs/ReputationBan-0.21.0.jar) > "$OUTDIR/checks/jar-sha256.txt"
  fi
fi

if [[ -d "$ROOT/build/release" ]]; then
  find "$ROOT/build/release" -maxdepth 1 -type f -print | sort > "$OUTDIR/checks/release-artifacts.txt"
else
  echo "No build/release directory found." > "$OUTDIR/checks/release-artifacts.txt"
fi

LATEST_SMOKE="$(find "$ROOT/build/manual-smoke" -maxdepth 2 -path '*/paper-runtime-*/summary.txt' -type f 2>/dev/null | sort | tail -n 1 || true)"
if [[ -n "$LATEST_SMOKE" && -f "$LATEST_SMOKE" ]]; then
  cp "$LATEST_SMOKE" "$OUTDIR/checks/latest-paper-runtime-smoke-summary.txt"
else
  {
    echo "status=NOT_RUN"
    echo "message=No paper runtime smoke summary found."
    echo "nextStep=Run scripts/run-paper-runtime-smoke-helper.sh and record results with scripts/record-paper-runtime-smoke-result.sh"
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
