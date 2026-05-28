#!/usr/bin/env bash
set -euo pipefail

PROJECT_NAME="ReputationBan"
EXPECTED_VERSION="1.0.1"
EXPECTED_API_VERSION="26.1.2"
EXPECTED_MAIN="dev.modplugin.reputationban.ReputationBanPlugin"
EXPECTED_JAR="build/libs/${PROJECT_NAME}-${EXPECTED_VERSION}.jar"
RELEASE_DIR="build/release"
RELEASE_JAR="${RELEASE_DIR}/${PROJECT_NAME}-${EXPECTED_VERSION}.jar"
RELEASE_ZIP="${RELEASE_DIR}/${PROJECT_NAME}-${EXPECTED_VERSION}-release.zip"
ROOT="$(pwd)"

fail() { echo "[FAIL] $*" >&2; exit 1; }
pass() { echo "[PASS] $*"; }
require_command() { command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"; pass "Command available: $1"; }
require_file() { [[ -f "$1" ]] || fail "Missing required file: $1"; pass "Found $1"; }
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
require_command jar
require_command sha256sum

for file in \
  build.gradle.kts \
  src/main/resources/plugin.yml \
  src/main/resources/config.yml \
  README.md \
  CHANGELOG.md \
  reputationban_phase_plan.md \
  docs/CONFIGURATION.md \
  docs/INTEGRATIONS.md \
  docs/PLAYER_GUIDE.md \
  docs/OPERATOR_GUIDE.md \
  docs/phase-38.md \
  docs/RELEASE_READINESS.md \
  docs/V1_0_1_CANDIDATES.md \
  docs/BUGFIX_INTAKE.md \
  docs/phase-37.md \
  scripts/run-local-smoke-check.sh \
  scripts/create-release-artifact.sh \
  scripts/verify-release-artifact.sh \
  scripts/make-review-archive.sh; do
  require_file "$file"
done

YML=src/main/resources/plugin.yml
[[ "$(extract_yaml_value "$YML" name)" == "$PROJECT_NAME" ]] || fail "plugin.yml name is not ${PROJECT_NAME}"
[[ "$(extract_yaml_value "$YML" version)" == "$EXPECTED_VERSION" ]] || fail "plugin.yml version is not ${EXPECTED_VERSION}"
[[ "$(extract_yaml_value "$YML" main)" == "$EXPECTED_MAIN" ]] || fail "plugin.yml main is not ${EXPECTED_MAIN}"
[[ "$(extract_yaml_value "$YML" api-version)" == "$EXPECTED_API_VERSION" ]] || fail "plugin.yml api-version is not ${EXPECTED_API_VERSION}"
grep -q "version = \"${EXPECTED_VERSION}\"" build.gradle.kts || fail "build.gradle.kts version is not ${EXPECTED_VERSION}"
grep -q "JavaLanguageVersion.of(25)" build.gradle.kts || fail "Java 25 toolchain not found"
grep -q "io.papermc.paper:paper-api:26.1.2.build" build.gradle.kts || fail "Paper API 26.1.2 dependency not found"
grep -q "VERSION=\"${EXPECTED_VERSION}\"" scripts/create-release-artifact.sh || fail "create-release-artifact.sh does not target ${EXPECTED_VERSION}"
grep -q "VERSION=\"${EXPECTED_VERSION}\"" scripts/verify-release-artifact.sh || fail "verify-release-artifact.sh does not target ${EXPECTED_VERSION}"
grep -q "EXPECTED_VERSION=\"${EXPECTED_VERSION}\"" scripts/run-local-smoke-check.sh || fail "run-local-smoke-check.sh does not target ${EXPECTED_VERSION}"

require_file src/main/java/dev/modplugin/reputationban/service/TargetProtectionService.java
require_file src/main/java/dev/modplugin/reputationban/model/TargetProtectionResult.java
grep -q "targetProtectionService.check" src/main/java/dev/modplugin/reputationban/service/PunishmentService.java \
  || fail "PunishmentService does not check TargetProtectionService before auto BAN"
grep -q "loadUser" src/main/java/dev/modplugin/reputationban/integration/luckperms/LuckPermsReflectionAdapter.java \
  || fail "LuckPerms offline loadUser handling missing"
grep -q "CompletableFuture" src/main/java/dev/modplugin/reputationban/integration/luckperms/LuckPermsReflectionAdapter.java \
  || fail "LuckPerms offline loadUser is not handled as CompletableFuture"
grep -q "offline-lookup" src/main/resources/config.yml \
  || fail "LuckPerms offline-lookup config missing"
grep -q "fail-closed-for-bypass" src/main/resources/config.yml src/main/java/dev/modplugin/reputationban/config/PluginConfig.java \
  || fail "LuckPerms fail-closed-for-bypass config missing"

grep -q "PRAGMA busy_timeout = 5000" src/main/java/dev/modplugin/reputationban/database/DatabaseManager.java \
  || fail "DatabaseManager missing SQLite busy_timeout"
grep -q "PRAGMA synchronous = NORMAL" src/main/java/dev/modplugin/reputationban/database/DatabaseManager.java \
  || fail "DatabaseManager missing SQLite synchronous NORMAL"
grep -q "awaitTermination" src/main/java/dev/modplugin/reputationban/database/DatabaseManager.java \
  || fail "DatabaseManager.close missing awaitTermination"
grep -q "volatile boolean closed" src/main/java/dev/modplugin/reputationban/database/DatabaseManager.java \
  || fail "DatabaseManager missing closed flag"
grep -q "CompletableFuture.failedFuture" src/main/java/dev/modplugin/reputationban/database/DatabaseManager.java \
  || fail "DatabaseManager does not return failed future after close"

grep -q "limits:" src/main/resources/config.yml || fail "limits config missing"
grep -q "max-report-reason-length" src/main/resources/config.yml src/main/java/dev/modplugin/reputationban/command/ReportBadCommand.java \
  || fail "max-report-reason-length is not enforced by /reportbad"
grep -q "max-review-note-length" src/main/resources/config.yml src/main/java/dev/modplugin/reputationban/command/ReportsCommand.java \
  || fail "max-review-note-length is not enforced by /reports"
grep -q "max-audit-reason-length" src/main/resources/config.yml src/main/java/dev/modplugin/reputationban/service/AuditService.java \
  || fail "max-audit-reason-length handling missing"
grep -q "max-context-summary-length" src/main/resources/config.yml src/main/java/dev/modplugin/reputationban/service/ReportService.java \
  || fail "max-context-summary-length handling missing"

grep -q "ON CONFLICT(uuid) DO UPDATE SET" src/main/java/dev/modplugin/reputationban/service/ScoreService.java \
  || fail "ScoreService does not create/update missing players row"
grep -q "updatedRows == 0" src/main/java/dev/modplugin/reputationban/service/ScoreService.java \
  || fail "ScoreService does not check UPDATE row count"
grep -q "Bukkit Profile BAN was applied but ReputationBan DB record failed" src/main/java/dev/modplugin/reputationban/service/PunishmentService.java \
  || fail "BAN record failure SEVERE warning missing"
grep -q "notifyStaff" src/main/java/dev/modplugin/reputationban/service/PunishmentService.java \
  || fail "BAN record failure staff notification missing"

for index in \
  "idx_reports_status_created" \
  "idx_reports_target_category_status_created" \
  "idx_players_lower_name" \
  "idx_bans_target_active" \
  "idx_report_context_provider_created"; do
  grep -q "$index" src/main/java/dev/modplugin/reputationban/database/DatabaseManager.java \
    || fail "Missing SQLite index: $index"
done

grep -q "v1.0.1 hotfix candidate" docs/phase-37.md || fail "docs/phase-37.md missing hotfix candidate summary"
grep -q "v1.0.1 tag/releaseはまだ未実施" docs/phase-37.md || fail "docs/phase-37.md missing no tag/release note"
grep -q "1.0.1" README.md CHANGELOG.md reputationban_phase_plan.md || fail "v1.0.1 docs not updated"
grep -q "offline-lookup" docs/CONFIGURATION.md docs/INTEGRATIONS.md || fail "offline lookup docs missing"
grep -q "max-report-reason-length" docs/CONFIGURATION.md || fail "limits docs missing"
grep -q "docs/PLAYER_GUIDE.md" README.md || fail "README.md does not link docs/PLAYER_GUIDE.md"
grep -q "docs/OPERATOR_GUIDE.md" README.md || fail "README.md does not link docs/OPERATOR_GUIDE.md"
grep -q "docs/phase-38.md" README.md || fail "README.md does not link docs/phase-38.md"
grep -q "/reportbad" docs/PLAYER_GUIDE.md || fail "PLAYER_GUIDE.md missing /reportbad"
grep -q "/rep" docs/PLAYER_GUIDE.md || fail "PLAYER_GUIDE.md missing /rep"
grep -q "/rep doctor" docs/OPERATOR_GUIDE.md || fail "OPERATOR_GUIDE.md missing /rep doctor"
grep -q "/reports evidence" docs/OPERATOR_GUIDE.md || fail "OPERATOR_GUIDE.md missing /reports evidence"
grep -q "100人規模運用" docs/OPERATOR_GUIDE.md || fail "OPERATOR_GUIDE.md missing 100人規模運用の推奨設定"
grep -q "Javaコード変更なし" docs/phase-38.md || fail "docs/phase-38.md missing Java no-change note"
grep -q "version変更なし" docs/phase-38.md || fail "docs/phase-38.md missing version no-change note"

if grep -RE "https://(canary\\.|ptb\\.)?discord(app)?\\.com/api/webhooks/[0-9]+/[A-Za-z0-9_-]{20,}" docs README.md SUPPORT.md CHANGELOG.md >/dev/null; then
  fail "Concrete Discord webhook URL-like value found in docs"
fi
if grep -REi "(password|token|secret|session|cookie)[[:space:]]*[:=][[:space:]]*['\"]?[^[:space:]'\"<>]+['\"]?" docs README.md SUPPORT.md CHANGELOG.md \
  | grep -Ev "<redacted>|\\[REDACTED\\]|your-|example|例|貼らない|含めない|伏せ|未設定|configured|設定済み|確認|secret scan|Secret取り扱い|secret-like|secret が" >/dev/null; then
  fail "Secret-like concrete value found in docs"
fi

if [[ -n "$(git tag --list "v1.0.1")" ]]; then
  fail "v1.0.1 tag exists; Phase 37 must not create it"
fi
if grep -R --exclude=review_code.sh "g[it] tag -a v1.0.1\\|g[it] push origin v1.0.1\\|g[h] release create v1.0.1" scripts .github 2>/dev/null; then
  fail "v1.0.1 tag/release command found in automation"
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

for script in scripts/*.sh; do
  bash -n "$script" || fail "$script syntax check failed"
done

./scripts/check-docs-localization.sh
./scripts/check-optional-dependency-safety.sh
./scripts/check-paper-runtime-readiness.sh
./scripts/check-integration-runtime-readiness.sh
./scripts/check-discordsrv-runtime-readiness.sh
./scripts/check-player-report-runtime-readiness.sh
./scripts/check-runtime-smoke-consistency.sh

if [[ "${REPUTATIONBAN_SKIP_BUILD:-0}" != "1" ]]; then
  preserve_manual_smoke ./gradlew clean test build --warning-mode all
fi

[[ -f "$EXPECTED_JAR" ]] || fail "Expected JAR not found: $EXPECTED_JAR"
jar tf "$EXPECTED_JAR" | grep -q "^plugin.yml$" || fail "plugin.yml missing from JAR"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
(cd "$TMP_DIR" && jar xf "$ROOT/$EXPECTED_JAR" plugin.yml)
grep -q "^version:[[:space:]]*${EXPECTED_VERSION}$" "$TMP_DIR/plugin.yml" || fail "JAR plugin.yml version is not ${EXPECTED_VERSION}"

./scripts/create-release-artifact.sh
[[ -f "$RELEASE_JAR" ]] || fail "release jar not found: $RELEASE_JAR"
[[ -f "${RELEASE_JAR}.sha256" ]] || fail "release jar sha256 not found"
[[ -f "$RELEASE_ZIP" ]] || fail "release zip not found: $RELEASE_ZIP"
[[ -f "${RELEASE_ZIP}.sha256" ]] || fail "release zip sha256 not found"
./scripts/verify-release-artifact.sh

pass "Review checks completed for ${PROJECT_NAME} ${EXPECTED_VERSION}"
