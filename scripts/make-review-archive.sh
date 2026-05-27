#!/usr/bin/env bash
set -euo pipefail

# ReputationBan review archive generator.
# Usage:
#   bash scripts/make-review-archive.sh
#   bash scripts/make-review-archive.sh "Phase 10"
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

run_logged "git diff --check" "$OUTDIR/checks/git-diff-check.txt" git diff --check

run_logged "./gradlew clean test build --warning-mode all" \
  "$OUTDIR/checks/gradle-clean-test-build.txt" \
  ./gradlew clean test build --warning-mode all

if [[ -x "$ROOT/scripts/review_code.sh" ]]; then
  run_logged "./scripts/review_code.sh" "$OUTDIR/checks/review-code.txt" "$ROOT/scripts/review_code.sh"
else
  echo "./scripts/review_code.sh=missing" >> "$COMMAND_STATUS"
fi

if [[ -x "$ROOT/scripts/run-local-smoke-check.sh" ]]; then
  run_logged "./scripts/run-local-smoke-check.sh" "$OUTDIR/checks/local-smoke-check.txt" "$ROOT/scripts/run-local-smoke-check.sh"
else
  echo "./scripts/run-local-smoke-check.sh=missing" >> "$COMMAND_STATUS"
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
  if [[ -f "$ROOT/build/libs/ReputationBan-0.10.0.jar" ]]; then
    (cd "$ROOT" && sha256sum build/libs/ReputationBan-0.10.0.jar) > "$OUTDIR/checks/jar-sha256.txt"
  fi
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
