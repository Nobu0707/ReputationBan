#!/usr/bin/env bash
set -euo pipefail

PROJECT_NAME="ReputationBan"
EXPECTED_VERSION="1.0.0"
EXPECTED_MAIN="dev.modplugin.reputationban.ReputationBanPlugin"
EXPECTED_API_VERSION="26.1.2"
ROOT="$(pwd)"
EXPECTED_JAR="build/libs/${PROJECT_NAME}-${EXPECTED_VERSION}.jar"
RELEASE_DIR="build/release"
RELEASE_JAR="${RELEASE_DIR}/${PROJECT_NAME}-${EXPECTED_VERSION}.jar"
RELEASE_ZIP="${RELEASE_DIR}/${PROJECT_NAME}-${EXPECTED_VERSION}-release.zip"

fail() { echo "[FAIL] $*" >&2; exit 1; }
pass() { echo "[PASS] $*"; }
info() { echo "[INFO] $*"; }
require_file() { [[ -f "$1" ]] || fail "Missing required file: $1"; pass "Found $1"; }
require_dir() { [[ -d "$1" ]] || fail "Missing required directory: $1"; pass "Found $1"; }
require_command() { command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"; pass "Command available: $1"; }
extract_yaml_value() { grep -E "^$2:" "$1" | head -n 1 | sed -E "s/^$2:[[:space:]]*//" | tr -d "'\""; }

preserve_manual_smoke() {
  local preserved
  local code
  preserved="$(mktemp -d)"
  if [[ -d build/manual-smoke ]]; then
    cp -R build/manual-smoke "$preserved/manual-smoke"
  fi
  set +e
  "$@"
  code=$?
  set -e
  if [[ -d "$preserved/manual-smoke" ]]; then
    mkdir -p build
    rm -rf build/manual-smoke
    cp -R "$preserved/manual-smoke" build/manual-smoke
  fi
  rm -rf "$preserved"
  return "$code"
}

require_command git
require_command grep
require_command sed
require_command awk
require_command find
require_command jar
require_command sha256sum
require_command gh

[[ -d .git ]] || fail "Not a Git repository"
git rev-parse --is-inside-work-tree >/dev/null || fail "Not inside a Git work tree"

for file in \
  settings.gradle.kts \
  build.gradle.kts \
  gradlew \
  README.md \
  CHANGELOG.md \
  reputationban_phase_plan.md \
  scripts/check-docs-localization.sh \
  scripts/check-optional-dependency-safety.sh \
  scripts/check-integration-runtime-readiness.sh \
  scripts/check-paper-runtime-readiness.sh \
  scripts/check-player-report-runtime-readiness.sh \
  scripts/check-runtime-smoke-consistency.sh \
  scripts/check-v1-release-gates.sh \
  scripts/run-local-smoke-check.sh \
  scripts/run-paper-runtime-smoke.sh \
  scripts/run-integration-runtime-smoke.sh \
  scripts/create-release-artifact.sh \
  scripts/verify-release-artifact.sh \
  scripts/record-paper-runtime-smoke-result.sh \
  scripts/record-integration-runtime-smoke-result.sh \
  scripts/record-player-report-runtime-smoke-result.sh \
  scripts/generate-v1-go-no-go-report.sh \
  scripts/generate-v1-release-notes.sh \
  scripts/generate-v1-release-notes-draft.sh \
  scripts/make-review-archive.sh \
  docs/POST_RELEASE_MONITORING.md \
  docs/BUGFIX_INTAKE.md \
  docs/V1_0_1_CANDIDATES.md \
  docs/phase-32.md \
  docs/INSTALLATION.md \
  docs/CONFIGURATION.md \
  docs/MIGRATION.md \
  docs/RELEASE_READINESS.md \
  docs/RELEASE_CANDIDATE_CHECKLIST.md \
  docs/V1_RELEASE_PLAN.md \
  docs/V1_RELEASE_EXECUTION_PLAN.md \
  docs/runtime-smoke-checklist.md \
  docs/phase-31a.md \
  docs/phase-30.md \
  docs/phase-29.md \
  docs/SECURITY_REDACTION.md \
  docs/SUPPORT_BUNDLE.md \
  docs/INTEGRATIONS.md \
  docs/PLAYER_REPORT_RUNTIME_SMOKE_CHECKLIST.md \
  docs/INTEGRATION_RUNTIME_SMOKE_CHECKLIST.md \
  src/main/resources/plugin.yml \
  src/main/resources/config.yml; do
  require_file "$file"
done

require_dir src/main/java/dev/modplugin/reputationban

for executable in \
  gradlew \
  scripts/check-docs-localization.sh \
  scripts/check-optional-dependency-safety.sh \
  scripts/check-integration-runtime-readiness.sh \
  scripts/check-paper-runtime-readiness.sh \
  scripts/check-player-report-runtime-readiness.sh \
  scripts/check-runtime-smoke-consistency.sh \
  scripts/check-v1-release-gates.sh \
  scripts/run-local-smoke-check.sh \
  scripts/run-paper-runtime-smoke.sh \
  scripts/run-integration-runtime-smoke.sh \
  scripts/create-release-artifact.sh \
  scripts/verify-release-artifact.sh \
  scripts/record-paper-runtime-smoke-result.sh \
  scripts/record-integration-runtime-smoke-result.sh \
  scripts/record-player-report-runtime-smoke-result.sh \
  scripts/generate-v1-go-no-go-report.sh \
  scripts/generate-v1-release-notes.sh \
  scripts/make-review-archive.sh; do
  [[ -x "$executable" ]] || fail "$executable is not executable"
done

YML=src/main/resources/plugin.yml
[[ "$(extract_yaml_value "$YML" name)" == "$PROJECT_NAME" ]] || fail "plugin.yml name is not ${PROJECT_NAME}"
[[ "$(extract_yaml_value "$YML" version)" == "$EXPECTED_VERSION" ]] || fail "plugin.yml version is not ${EXPECTED_VERSION}"
[[ "$(extract_yaml_value "$YML" main)" == "$EXPECTED_MAIN" ]] || fail "plugin.yml main is not ${EXPECTED_MAIN}"
[[ "$(extract_yaml_value "$YML" api-version)" == "$EXPECTED_API_VERSION" ]] || fail "plugin.yml api-version is not ${EXPECTED_API_VERSION}"

grep -q "version = \"${EXPECTED_VERSION}\"" build.gradle.kts || fail "build.gradle.kts version is not ${EXPECTED_VERSION}"
grep -q "JavaLanguageVersion.of(25)" build.gradle.kts || fail "Java 25 toolchain not found"
grep -q "options.release.set(25)" build.gradle.kts || fail "Java release 25 not found"
grep -q "io.papermc.paper:paper-api:26.1.2.build" build.gradle.kts || fail "Paper API 26.1.2 dependency not found"
grep -q "org.xerial:sqlite-jdbc" "$YML" || fail "SQLite library not found in plugin.yml libraries"

grep -q "1.0.0" README.md || fail "README.md does not mention 1.0.0"
grep -q "1.0.0" CHANGELOG.md || fail "CHANGELOG.md does not mention 1.0.0"
grep -q "POST_RELEASE_MONITORING.md" README.md || fail "README.md does not mention post-release monitoring docs"
grep -q "BUGFIX_INTAKE.md" README.md || fail "README.md does not mention bugfix intake docs"
grep -q "V1_0_1_CANDIDATES.md" README.md || fail "README.md does not mention v1.0.1 candidates docs"
grep -q "v1.0.0 tag" README.md docs/phase-30.md docs/phase-29.md docs/V1_RELEASE_EXECUTION_PLAN.md || fail "v1.0.0 tag status docs missing"
grep -q "GitHub Release" README.md docs/phase-30.md docs/phase-29.md docs/V1_RELEASE_EXECUTION_PLAN.md || fail "GitHub Release status docs missing"
grep -q "Tag 作成前チェック" docs/V1_RELEASE_EXECUTION_PLAN.md || fail "v1.0.0 tag preflight check docs missing"
grep -q "gh release create v1.0.0" docs/V1_RELEASE_EXECUTION_PLAN.md || fail "GitHub Release draft creation command docs missing"
grep -q -- "--draft" docs/V1_RELEASE_EXECUTION_PLAN.md docs/phase-30.md || fail "GitHub Release draft flag docs missing"
grep -q "draft=false" docs/V1_RELEASE_EXECUTION_PLAN.md docs/phase-30.md || fail "GitHub Release publish prohibition docs missing"
grep -q "ReputationBan-1.0.0.jar" docs/phase-30.md scripts/create-release-artifact.sh scripts/verify-release-artifact.sh || fail "Phase 30 release artifact target missing"
grep -q "READY_FOR_V1_RELEASE_WITH_DISCORDSRV_WARNING" scripts/check-v1-release-gates.sh docs/phase-29.md docs/RELEASE_READINESS.md docs/RELEASE_CANDIDATE_CHECKLIST.md || fail "v1 release judgment is not updated"
grep -q "VERSION=\"${EXPECTED_VERSION}\"" scripts/create-release-artifact.sh || fail "create-release-artifact.sh does not target ${EXPECTED_VERSION}"
grep -q "VERSION=\"${EXPECTED_VERSION}\"" scripts/verify-release-artifact.sh || fail "verify-release-artifact.sh does not target ${EXPECTED_VERSION}"
grep -q "EXPECTED_VERSION=\"${EXPECTED_VERSION}\"" scripts/run-local-smoke-check.sh || fail "run-local-smoke-check.sh does not check ${EXPECTED_VERSION}"
grep -q -- "--carried-forward-from" scripts/record-player-report-runtime-smoke-result.sh || fail "player report recorder lacks --carried-forward-from"
grep -q "carriedForwardFrom" scripts/record-player-report-runtime-smoke-result.sh docs/phase-29.md || fail "player report carry-forward summary/doc missing"
grep -q "generate-v1-release-notes.sh" scripts/make-review-archive.sh docs/RELEASE_READINESS.md docs/RELEASE_CANDIDATE_CHECKLIST.md || fail "release notes final script is not wired into docs/archive"
grep -q "V1_RELEASE_EXECUTION_PLAN.md" scripts/make-review-archive.sh docs/RELEASE_READINESS.md README.md || fail "release execution plan is not wired into docs/archive"

if [[ -n "$(git tag --list "v1.0.0")" ]]; then
  HEAD_COMMIT="$(git rev-parse HEAD)"
  TAG_COMMIT="$(git rev-list -n 1 v1.0.0)"
  [[ "$TAG_COMMIT" == "b422e72ec5a917cdc04dee902e96a0cef190026c" ]] || fail "v1.0.0 tag no longer points at the Phase 30 release commit"
  if [[ "$HEAD_COMMIT" != "$TAG_COMMIT" ]]; then
    if [[ -f docs/phase-31.md ]] && git merge-base --is-ancestor "$TAG_COMMIT" "$HEAD_COMMIT"; then
      pass "v1.0.0 tag points to an ancestor release commit after Phase 31/31a docs-only commits"
    else
      fail "v1.0.0 tag exists but does not point at HEAD"
    fi
  fi
fi

set +e
RELEASE_JSON="$(gh release view v1.0.0 --json tagName,isDraft,isPrerelease,url,assets,body 2>&1)"
RELEASE_CODE=$?
set -e
[[ "$RELEASE_CODE" == "0" ]] || fail "Unable to confirm GitHub Release v1.0.0 status: ${RELEASE_JSON//$'\n'/ }"
[[ "$RELEASE_JSON" == *'"tagName":"v1.0.0"'* ]] || fail "GitHub Release tagName is not v1.0.0"
[[ "$RELEASE_JSON" == *'"isDraft":false'* ]] || fail "GitHub Release v1.0.0 is not published"
[[ "$RELEASE_JSON" == *'"isPrerelease":false'* ]] || fail "GitHub Release v1.0.0 is prerelease"
for asset in \
  "ReputationBan-1.0.0.jar" \
  "ReputationBan-1.0.0.jar.sha256" \
  "ReputationBan-1.0.0-release.zip" \
  "ReputationBan-1.0.0-release.zip.sha256"; do
  [[ "$RELEASE_JSON" == *"\"name\":\"$asset\""* ]] || fail "GitHub Release v1.0.0 missing asset: $asset"
done
if printf '%s\n' "$RELEASE_JSON" | grep -E "DRAFT_TO_CREATE|公開はまだ|draft 作成まで" >/dev/null; then
  fail "Published GitHub Release notes still contain pre-publish wording"
fi
pass "GitHub Release v1.0.0 is published and release notes do not contain old draft wording"

SCAN_PATHS=(scripts)
if [[ -d .github ]]; then
  SCAN_PATHS+=(.github)
fi

if grep -R --exclude=review_code.sh "g[h] release create\\|g[h] release upload\\|g[h] release edit\\|g[h] release delete" "${SCAN_PATHS[@]}" >/dev/null 2>&1; then
  fail "GitHub Release creation/upload command found in executable automation"
fi
if grep -R --exclude=review_code.sh "g[h] release edit.*--draft=false\\|g[h] release edit.*--latest\\|g[h] release create.*--draft=false" "${SCAN_PATHS[@]}" >/dev/null 2>&1; then
  fail "GitHub Release publish command found in executable automation"
fi

if grep -R --exclude=review_code.sh "g[it] tag -a v1.0.0\\|g[it] push origin v1.0.0" "${SCAN_PATHS[@]}" >/dev/null 2>&1; then
  fail "v1.0.0 tag creation/push command found in executable automation"
fi

if grep -R "performRollback\\|performRestore\\|performPurge" src/main/java >/dev/null; then
  fail "CoreProtect rollback/restore/purge API usage detected"
fi
if grep -R "saveUser\\|setPermission\\|data()\\.add\\|data()\\.remove" src/main/java >/dev/null; then
  fail "LuckPerms write API usage detected"
fi
if grep -R "addRegion\\|removeRegion\\|saveChanges\\|setFlag\\|setPriority\\|setOwners\\|setMembers" src/main/java >/dev/null; then
  fail "WorldGuard region/flag mutation usage detected"
fi
if grep -R "createClaim\\|deleteClaim\\|resizeClaim\\|changeClaimOwner\\|setOwner\\|setManagers\\|setBuilders\\|setContainers\\|setAccessors" src/main/java >/dev/null; then
  fail "GriefPrevention claim/trust mutation usage detected"
fi
if grep -R "dispatchCommand\\|performCommand" src/main/java/dev/modplugin/reputationban/integration/discordsrv src/main/java/dev/modplugin/reputationban/notification 2>/dev/null; then
  fail "Discord integration appears to execute Minecraft commands"
fi
if grep -R "addRole\\|removeRole\\|modifyMemberRoles\\|RoleManager" src/main/java/dev/modplugin/reputationban/integration/discordsrv src/main/java/dev/modplugin/reputationban/notification 2>/dev/null; then
  fail "Discord role mutation usage detected"
fi
if grep -RE "https://(canary\\.|ptb\\.)?discord(app)?\\.com/api/webhooks/[0-9]+/[A-Za-z0-9_-]{20,}" src/main/java src/main/resources >/dev/null; then
  fail "Concrete Discord webhook URL detected in main sources/resources"
fi

for script in scripts/*.sh; do
  bash -n "$script" || fail "$script syntax check failed"
done

./scripts/check-docs-localization.sh
./scripts/check-optional-dependency-safety.sh

info "Running Gradle clean test build"
preserve_manual_smoke ./gradlew clean test build --warning-mode all

[[ -f "$EXPECTED_JAR" ]] || fail "Expected JAR not found: $EXPECTED_JAR"
jar tf "$EXPECTED_JAR" | grep -q "^plugin.yml$" || fail "plugin.yml missing from JAR"
jar tf "$EXPECTED_JAR" | grep -q "dev/modplugin/reputationban/ReputationBanPlugin.class" || fail "main plugin class missing from JAR"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
(cd "$TMP_DIR" && jar xf "$ROOT/$EXPECTED_JAR" plugin.yml)
grep -q "^version:[[:space:]]*${EXPECTED_VERSION}$" "$TMP_DIR/plugin.yml" || fail "JAR plugin.yml version is not ${EXPECTED_VERSION}"

./scripts/check-paper-runtime-readiness.sh
./scripts/check-integration-runtime-readiness.sh
./scripts/check-player-report-runtime-readiness.sh
./scripts/check-runtime-smoke-consistency.sh
REPUTATIONBAN_ALLOW_V1_TAG_BEHIND_HEAD=1 ./scripts/check-v1-release-gates.sh
./scripts/generate-v1-go-no-go-report.sh
./scripts/generate-v1-release-notes.sh

RELEASE_NOTES="${RELEASE_DIR}/ReputationBan-v1.0.0-release-notes.md"
GO_NO_GO_REPORT="${RELEASE_DIR}/ReputationBan-v1-go-no-go-report.md"
require_file "$RELEASE_NOTES"
require_file "$GO_NO_GO_REPORT"
if grep -E "DRAFT_TO_CREATE|公開はまだ|draft 作成まで|Phase 30 creates" "$RELEASE_NOTES" "$GO_NO_GO_REPORT" >/dev/null; then
  fail "Published release notes/report still contain pre-publish wording"
fi
grep -q "GitHub Release status: PUBLISHED" "$RELEASE_NOTES" || fail "Release notes do not show published GitHub Release status"
grep -q "GitHub Release status: PUBLISHED" "$GO_NO_GO_REPORT" || fail "Go/No-Go report does not show published GitHub Release status"
grep -q "Tag status: CREATED" "$RELEASE_NOTES" || fail "Release notes do not show created tag status"
grep -q "Tag status: CREATED" "$GO_NO_GO_REPORT" || fail "Go/No-Go report does not show created tag status"
grep -q "Judgment: RELEASED_WITH_DISCORDSRV_WARNING" "$GO_NO_GO_REPORT" || fail "Go/No-Go report does not show released judgment"

./scripts/create-release-artifact.sh
[[ -f "$RELEASE_JAR" ]] || fail "release jar not found: $RELEASE_JAR"
[[ -f "${RELEASE_JAR}.sha256" ]] || fail "release jar sha256 not found"
[[ -f "$RELEASE_ZIP" ]] || fail "release zip not found: $RELEASE_ZIP"
[[ -f "${RELEASE_ZIP}.sha256" ]] || fail "release zip sha256 not found"
[[ -f "${RELEASE_DIR}/ReputationBan-v1-go-no-go-report.md" ]] || fail "Go/No-Go report not found"
[[ -f "${RELEASE_DIR}/ReputationBan-v1.0.0-release-notes.md" ]] || fail "release notes final candidate not found"
./scripts/verify-release-artifact.sh

jar tf "$RELEASE_ZIP" | grep -q "^${PROJECT_NAME}-${EXPECTED_VERSION}.jar$" || fail "release zip missing JAR"
jar tf "$RELEASE_ZIP" | grep -q "^README.md$" || fail "release zip missing README.md"
jar tf "$RELEASE_ZIP" | grep -q "^CHANGELOG.md$" || fail "release zip missing CHANGELOG.md"
jar tf "$RELEASE_ZIP" | grep -q "^docs/V1_RELEASE_EXECUTION_PLAN.md$" || fail "release zip missing V1_RELEASE_EXECUTION_PLAN.md"
if jar tf "$RELEASE_ZIP" | grep -E '(^|/)(config\.yml|reputationban\.db|reputationban\.db-wal|reputationban\.db-shm|latest\.log|debug\.log)$|(^|/)logs/' >/dev/null; then
  fail "release zip contains forbidden config, DB, or logs"
fi

REPUTATIONBAN_SKIP_REVIEW_CODE=1 REPUTATIONBAN_SKIP_BUILD=1 ./scripts/run-local-smoke-check.sh

pass "Review checks completed for ${PROJECT_NAME} ${EXPECTED_VERSION}"
