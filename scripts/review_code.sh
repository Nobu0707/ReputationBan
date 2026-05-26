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
require_file scripts/make-review-archive.sh
require_file src/main/resources/plugin.yml
require_file src/main/resources/config.yml
require_dir "$EXPECTED_PACKAGE_DIR"
[[ -x ./gradlew ]] || fail "gradlew is not executable"
[[ -x ./scripts/review_code.sh ]] || fail "review_code.sh is not executable"
[[ -x ./scripts/make-review-archive.sh ]] || fail "make-review-archive.sh is not executable"

grep -q "io.papermc.paper:paper-api:26.1.2.build" build.gradle.kts || fail "Paper API 26.1.2 dependency not found"
grep -q "JavaLanguageVersion.of(25)" build.gradle.kts || fail "Java 25 toolchain not found"
grep -q "options.release.set(25)" build.gradle.kts || fail "Java release 25 not found"

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
grep -R "OfflinePlayer.*ban\\|\\.ban(reason" src/main/java >/dev/null || fail "Profile ban API usage not found"

if grep -R "net\.minecraft\|org\.bukkit\.craftbukkit\|CraftPlayer\|NMS" src/main/java >/dev/null; then
  fail "NMS/CraftBukkit usage detected"
fi
if grep -R "BanList.Type.NAME\|@SuppressWarnings(\"deprecation\")" src/main/java >/dev/null; then
  fail "Deprecated name ban usage detected"
fi

./gradlew clean test build --warning-mode all

JAR="$(find build/libs -maxdepth 1 -type f -name "${EXPECTED_JAR_PREFIX}-*.jar" | sort | tail -n 1 || true)"
[[ -n "$JAR" ]] || fail "Built jar not found"
require_command jar
jar tf "$JAR" | grep -q "plugin.yml" || fail "plugin.yml missing from jar"
jar tf "$JAR" | grep -q "dev/modplugin/reputationban/ReputationBanPlugin.class" || fail "Main class missing from jar"

git rev-list --count HEAD >/dev/null || fail "No commits found"

if git status --porcelain | grep -E '(^|/).*:Zone.Identifier$' >/dev/null; then
  fail "Zone.Identifier file is present in working tree"
fi

pass "Review checks completed"
