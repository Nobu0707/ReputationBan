#!/usr/bin/env bash
set -euo pipefail

# ReputationBan code review / verification script
# Intended to be run from the repository root.
# Usage:
#   chmod +x scripts/review_code.sh
#   ./scripts/review_code.sh

PROJECT_NAME="ReputationBan"
EXPECTED_VERSION="0.1.0"
EXPECTED_MAIN="dev.modplugin.reputationban.ReputationBanPlugin"
EXPECTED_API_VERSION="26.1.2"
EXPECTED_PACKAGE_DIR="src/main/java/dev/modplugin/reputationban"
EXPECTED_JAR_PREFIX="ReputationBan"

fail() {
  echo "[FAIL] $*" >&2
  exit 1
}

warn() {
  echo "[WARN] $*" >&2
}

pass() {
  echo "[PASS] $*"
}

info() {
  echo "[INFO] $*"
}

require_file() {
  local path="$1"
  [[ -f "$path" ]] || fail "Missing required file: $path"
  pass "Found $path"
}

require_dir() {
  local path="$1"
  [[ -d "$path" ]] || fail "Missing required directory: $path"
  pass "Found $path"
}

require_command() {
  local cmd="$1"
  command -v "$cmd" >/dev/null 2>&1 || fail "Required command not found: $cmd"
  pass "Command available: $cmd"
}

extract_yaml_value() {
  local file="$1"
  local key="$2"
  grep -E "^${key}:" "$file" | head -n 1 | sed -E "s/^${key}:[[:space:]]*//" | tr -d "'\""
}

check_git_state() {
  info "Checking Git repository state"
  [[ -d .git ]] || fail "This directory is not a Git repository. Run git init first."
  git rev-parse --is-inside-work-tree >/dev/null || fail "Not inside a Git work tree"
  local branch
  branch="$(git branch --show-current || true)"
  [[ -n "$branch" ]] || fail "No current Git branch detected"
  pass "Git branch: $branch"

  if [[ -n "$(git status --porcelain)" ]]; then
    warn "Working tree has uncommitted changes. Review them before final approval."
    git status --short
  else
    pass "Working tree is clean"
  fi
}

check_project_layout() {
  info "Checking project layout"
  require_file "settings.gradle.kts"
  require_file "build.gradle.kts"
  require_file "gradlew"
  require_file "src/main/resources/plugin.yml"
  require_file "src/main/resources/config.yml"
  require_dir "$EXPECTED_PACKAGE_DIR"

  [[ -x ./gradlew ]] || fail "gradlew is not executable. Run: chmod +x gradlew"
  pass "gradlew is executable"
}

check_gradle_config() {
  info "Checking Gradle configuration"
  grep -q "io.papermc.paper:paper-api:26.1.2.build" build.gradle.kts \
    || fail "build.gradle.kts must depend on io.papermc.paper:paper-api:26.1.2.build.+ or a fixed 26.1.2 build"
  pass "Paper API 26.1.2 dependency found"

  grep -q "JavaLanguageVersion.of(25)" build.gradle.kts \
    || fail "build.gradle.kts must set Java toolchain to 25"
  pass "Java 25 toolchain found"

  grep -q "options.release.set(25)" build.gradle.kts \
    || warn "JavaCompile options.release.set(25) not found. Toolchain may still work, but explicit release is preferred."

  if grep -q "paperweight.userdev" build.gradle.kts; then
    warn "paperweight-userdev is present. Phase 1 should avoid NMS unless explicitly justified."
  else
    pass "paperweight-userdev not used"
  fi
}

check_plugin_yml() {
  info "Checking plugin.yml"
  local yml="src/main/resources/plugin.yml"
  local name version main api_version
  name="$(extract_yaml_value "$yml" "name")"
  version="$(extract_yaml_value "$yml" "version")"
  main="$(extract_yaml_value "$yml" "main")"
  api_version="$(extract_yaml_value "$yml" "api-version")"

  [[ "$name" == "$PROJECT_NAME" ]] || fail "plugin.yml name must be $PROJECT_NAME, got: $name"
  [[ "$version" == "$EXPECTED_VERSION" ]] || fail "plugin.yml version must be $EXPECTED_VERSION for Phase 1, got: $version"
  [[ "$main" == "$EXPECTED_MAIN" ]] || fail "plugin.yml main must be $EXPECTED_MAIN, got: $main"
  [[ "$api_version" == "$EXPECTED_API_VERSION" ]] || fail "plugin.yml api-version must be $EXPECTED_API_VERSION, got: $api_version"

  grep -q "reportbad:" "$yml" || fail "plugin.yml must define /reportbad command"
  grep -q "rep:" "$yml" || fail "plugin.yml must define /rep command"
  grep -q "reports:" "$yml" || fail "plugin.yml should define /reports command even if Phase 1 has minimal behavior"
  grep -q "reputationban.report:" "$yml" || fail "plugin.yml must define reputationban.report permission"
  grep -q "reputationban.bypass:" "$yml" || fail "plugin.yml must define reputationban.bypass permission"
  grep -q "org.xerial:sqlite-jdbc" "$yml" || fail "plugin.yml must declare sqlite-jdbc in libraries or build must shade it intentionally"

  pass "plugin.yml core fields, commands, permissions, and sqlite library are valid"
}

check_config_yml() {
  info "Checking config.yml"
  local cfg="src/main/resources/config.yml"
  grep -q "^initial-score:[[:space:]]*100" "$cfg" || fail "config.yml must set initial-score: 100"
  grep -q "^max-score:[[:space:]]*100" "$cfg" || fail "config.yml must set max-score: 100"
  grep -q "^categories:" "$cfg" || fail "config.yml must define categories"
  grep -q "griefing:" "$cfg" || fail "config.yml must include category griefing"
  grep -q "abusive_chat:" "$cfg" || fail "config.yml must include category abusive_chat"
  grep -q "cheating:" "$cfg" || fail "config.yml must include category cheating"
  grep -q "same-target-cooldown-days" "$cfg" || fail "config.yml must include same-target-cooldown-days"
  grep -q "global-report-seconds" "$cfg" || fail "config.yml must include global-report-seconds"
  grep -q "threshold:[[:space:]]*0" "$cfg" || fail "config.yml must include a ban threshold of 0"
  pass "config.yml contains Phase 1 required settings"
}

check_source_patterns() {
  info "Checking Java source patterns"
  local java_files
  java_files="$(find src/main/java -name '*.java' -print)"
  [[ -n "$java_files" ]] || fail "No Java source files found"

  grep -R "extends JavaPlugin" src/main/java >/dev/null || fail "Main plugin class extending JavaPlugin not found"
  grep -R "PlayerJoinEvent" src/main/java >/dev/null || fail "PlayerJoinEvent handling not found"
  grep -R "CREATE TABLE" src/main/java >/dev/null || fail "Database table creation SQL not found"
  grep -R "players" src/main/java >/dev/null || fail "players table/service usage not found"
  grep -R "reports" src/main/java >/dev/null || fail "reports table/service usage not found"
  grep -R "score_history" src/main/java >/dev/null || fail "score_history table/service usage not found"
  grep -R "bans" src/main/java >/dev/null || fail "bans table/service usage not found"
  grep -R "getUniqueId" src/main/java >/dev/null || fail "UUID-based player handling not found"
  grep -R "reputationban.bypass" src/main/java >/dev/null || fail "bypass permission check not found"

  if grep -R "net\.minecraft\|org\.bukkit\.craftbukkit\|CraftPlayer\|NMS" src/main/java >/dev/null; then
    fail "NMS/CraftBukkit usage detected. Phase 1 must use Paper/Bukkit API only."
  fi
  pass "No NMS/CraftBukkit usage detected"

  if grep -R "Thread\.sleep\|System\.exit" src/main/java >/dev/null; then
    fail "Dangerous blocking/exit call detected in plugin source"
  fi
  pass "No Thread.sleep/System.exit detected"

  if grep -R "Bukkit\.getOnlinePlayers\|Bukkit\.getPlayer\|Player#" src/main/java >/dev/null; then
    warn "Bukkit/Player API usage detected. Ensure calls happen on the main server thread, not inside async DB tasks."
  fi
}

run_build() {
  info "Running Gradle verification"
  ./gradlew --version
  ./gradlew clean test build --warning-mode all
  pass "Gradle clean test build completed"
}

check_built_jar() {
  info "Checking built JAR contents"
  local jar
  jar="$(find build/libs -maxdepth 1 -type f -name "${EXPECTED_JAR_PREFIX}-*.jar" | sort | tail -n 1 || true)"
  [[ -n "$jar" ]] || fail "Built plugin jar not found under build/libs"
  pass "Built jar: $jar"

  require_command "jar"
  jar tf "$jar" | grep -q "plugin.yml" || fail "plugin.yml not found in built jar"
  jar tf "$jar" | grep -q "dev/modplugin/reputationban/ReputationBanPlugin.class" \
    || fail "Main plugin class not found in built jar"
  pass "Built jar contains plugin.yml and main class"
}

check_commit_metadata() {
  info "Checking commit metadata"
  local commit_count
  commit_count="$(git rev-list --count HEAD 2>/dev/null || echo 0)"
  [[ "$commit_count" -ge 1 ]] || fail "No commits found. Codex must create a Phase 1 commit."
  pass "Commit count: $commit_count"

  local last_msg
  last_msg="$(git log -1 --pretty=%B)"
  echo "$last_msg" | grep -qi "phase 1\|0.1.0\|mvp" \
    || warn "Latest commit message should mention Phase 1 / 0.1.0 / MVP. Current message: $last_msg"

  if git remote get-url origin >/dev/null 2>&1; then
    pass "Git remote origin: $(git remote get-url origin)"
  else
    warn "No git remote named origin configured. Push to GitHub cannot be verified."
  fi
}

main() {
  echo "== ReputationBan Phase 1 Review Script =="
  require_command "git"
  require_command "grep"
  require_command "sed"
  require_command "find"
  require_command "sort"
  require_command "tail"

  check_git_state
  check_project_layout
  check_gradle_config
  check_plugin_yml
  check_config_yml
  check_source_patterns
  run_build
  check_built_jar
  check_commit_metadata

  echo ""
  echo "== Review completed =="
  echo "If only WARN messages remain, inspect them manually before approving the PR/commit."
}

main "$@"
