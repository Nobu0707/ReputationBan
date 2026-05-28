#!/usr/bin/env bash
set -euo pipefail

VERSION="0.27.0"
PROJECT_NAME="ReputationBan"
PLUGIN_JAR="build/libs/${PROJECT_NAME}-${VERSION}.jar"

REPUTATIONBAN_PAPER_DIR="${REPUTATIONBAN_PAPER_DIR:-$HOME/servers/paper-26.1.2}"
REPUTATIONBAN_PAPER_START_SCRIPT="${REPUTATIONBAN_PAPER_START_SCRIPT:-$REPUTATIONBAN_PAPER_DIR/start.sh}"
REPUTATIONBAN_INTEGRATION_PLUGIN_DIR="${REPUTATIONBAN_INTEGRATION_PLUGIN_DIR:-$HOME/servers/PaperPlugins}"
REPUTATIONBAN_JAVA_BIN="${REPUTATIONBAN_JAVA_BIN:-java}"
REPUTATIONBAN_SMOKE_TIMEOUT_SECONDS="${REPUTATIONBAN_SMOKE_TIMEOUT_SECONDS:-240}"
REPUTATIONBAN_SMOKE_COMMAND_DELAY_SECONDS="${REPUTATIONBAN_SMOKE_COMMAND_DELAY_SECONDS:-5}"
REPUTATIONBAN_SMOKE_MUTATING="${REPUTATIONBAN_SMOKE_MUTATING:-0}"
REPUTATIONBAN_SMOKE_STOP_SERVER="${REPUTATIONBAN_SMOKE_STOP_SERVER:-0}"
REPUTATIONBAN_INTEGRATION_RESTORE_PLUGINS="${REPUTATIONBAN_INTEGRATION_RESTORE_PLUGINS:-1}"
REPUTATIONBAN_SCREEN_NAME="${REPUTATIONBAN_SCREEN_NAME:-}"

STAMP="$(date +%Y%m%d-%H%M%S)"
STARTED_AT="$(date -Iseconds)"
OUTDIR="build/manual-smoke/integration-runtime-${STAMP}"
SUMMARY="${OUTDIR}/summary.txt"
COMMANDS_FILE="${OUTDIR}/commands.txt"
SERVER_LOG="${OUTDIR}/server.log"
ENVIRONMENT_FILE="${OUTDIR}/environment.txt"
SCREEN_BEFORE_FILE="${OUTDIR}/screen-before.txt"
SCREEN_AFTER_FILE="${OUTDIR}/screen-after.txt"
STAGED_PLUGINS_FILE="${OUTDIR}/staged-plugins.txt"
PLUGIN_RESTORE_FILE="${OUTDIR}/plugin-restore.txt"
INTEGRATION_STATUS_FILE="${OUTDIR}/integration-status.txt"
mkdir -p "$OUTDIR"
: > "$COMMANDS_FILE"
: > "$STAGED_PLUGINS_FILE"
: > "$PLUGIN_RESTORE_FILE"
: > "$INTEGRATION_STATUS_FILE"

PLUGINS_DIR="$REPUTATIONBAN_PAPER_DIR/plugins"
BACKUP_DIR="$PLUGINS_DIR/backups/reputationban-integration-smoke-${STAMP}"
STAGED_PLUGIN_BASENAMES=()
STAGED_PLUGIN_PATHS=()
BACKED_UP_PLUGIN_BASENAMES=()
RESTORED_PLUGINS=false
ACTIVE_INTEGRATIONS=""
UNAVAILABLE_INTEGRATIONS=""
DISCORDSRV_UNAVAILABLE_REASON=""

JAR_SHA="missing"
if [[ -f "$PLUGIN_JAR" ]]; then
  JAR_SHA="$(sha256sum "$PLUGIN_JAR" | awk '{print $1}')"
fi

join_by_comma() {
  local IFS=,
  echo "$*"
}

write_not_run_integration_status() {
  local reason="$1"
  {
    echo "LuckPerms=not_run"
    echo "CoreProtect=not_run"
    echo "WorldGuard=not_run"
    echo "GriefPrevention=not_run"
    echo "PlaceholderAPI=not_run"
    echo "DiscordSRV=not_run"
    echo "note=$reason"
  } > "$INTEGRATION_STATUS_FILE"
  ACTIVE_INTEGRATIONS=""
  UNAVAILABLE_INTEGRATIONS=""
  DISCORDSRV_UNAVAILABLE_REASON=""
}

write_integration_status_from_log() {
  local parsed="${OUTDIR}/integration-status.parsed"
  local names=(
    "LuckPerms"
    "CoreProtect"
    "WorldGuard"
    "GriefPrevention"
    "PlaceholderAPI"
    "DiscordSRV"
  )
  local active=()
  local unavailable=()
  local name configured plugin_present api_available is_active state

  if [[ ! -f "$SERVER_LOG" ]]; then
    write_not_run_integration_status "server log not found"
    return 0
  fi

  awk '
    BEGIN {
      split("LuckPerms CoreProtect WorldGuard GriefPrevention PlaceholderAPI DiscordSRV", ordered, " ")
      for (idx in ordered) {
        name = ordered[idx]
        names[name] = 1
        order[idx] = name
      }
    }
    {
      line = $0
      gsub(/\033\[[0-9;]*m/, "", line)
      sub(/\r$/, "", line)
      if (line ~ /\[ReputationBan\]/) {
        sub(/^.*\[ReputationBan\][[:space:]]*/, "", line)
      }
      if (line ~ /^(LuckPerms|CoreProtect|WorldGuard|GriefPrevention|PlaceholderAPI|DiscordSRV):$/) {
        current = line
        sub(/:$/, "", current)
        seen[current] = 1
        next
      }
      if (current != "" && line ~ /^[[:space:]]*(configuredEnabled|pluginPresent|apiAvailable|active)=/) {
        gsub(/^[[:space:]]*/, "", line)
        split(line, parts, "=")
        values[current, parts[1]] = parts[2]
      }
    }
    END {
      for (i = 1; i <= 6; i++) {
        name = order[i]
        print name "|" values[name, "configuredEnabled"] "|" values[name, "pluginPresent"] "|" values[name, "apiAvailable"] "|" values[name, "active"]
      }
    }
  ' "$SERVER_LOG" > "$parsed"

  : > "$INTEGRATION_STATUS_FILE"
  while IFS='|' read -r name configured plugin_present api_available is_active; do
    if [[ "$is_active" == "true" ]]; then
      state="active"
      active+=("$name")
    else
      state="unavailable"
      unavailable+=("$name")
    fi
    {
      echo "$name=$state"
      echo "$name.configuredEnabled=${configured:-unknown}"
      echo "$name.pluginPresent=${plugin_present:-unknown}"
      echo "$name.apiAvailable=${api_available:-unknown}"
      echo "$name.active=${is_active:-unknown}"
    } >> "$INTEGRATION_STATUS_FILE"
    if [[ "$name" == "DiscordSRV" && "$state" == "unavailable" ]]; then
      if [[ "$plugin_present" == "true" && "$api_available" != "true" ]]; then
        DISCORDSRV_UNAVAILABLE_REASON="Bot token not configured or API unavailable"
      else
        DISCORDSRV_UNAVAILABLE_REASON="DiscordSRV optional integration unavailable"
      fi
      echo "DiscordSRV.note=$DISCORDSRV_UNAVAILABLE_REASON" >> "$INTEGRATION_STATUS_FILE"
    fi
  done < "$parsed"

  ACTIVE_INTEGRATIONS="$(join_by_comma "${active[@]}")"
  UNAVAILABLE_INTEGRATIONS="$(join_by_comma "${unavailable[@]}")"
}

write_environment() {
  {
    echo "version=$VERSION"
    echo "jar=$PLUGIN_JAR"
    echo "jarSha256=$JAR_SHA"
    echo "paperDir=$REPUTATIONBAN_PAPER_DIR"
    echo "startScript=$REPUTATIONBAN_PAPER_START_SCRIPT"
    echo "integrationPluginDir=$REPUTATIONBAN_INTEGRATION_PLUGIN_DIR"
    echo "javaBin=$REPUTATIONBAN_JAVA_BIN"
    echo "timeoutSeconds=$REPUTATIONBAN_SMOKE_TIMEOUT_SECONDS"
    echo "commandDelaySeconds=$REPUTATIONBAN_SMOKE_COMMAND_DELAY_SECONDS"
    echo "mutating=$REPUTATIONBAN_SMOKE_MUTATING"
    echo "stopServer=$REPUTATIONBAN_SMOKE_STOP_SERVER"
    echo "restorePlugins=$REPUTATIONBAN_INTEGRATION_RESTORE_PLUGINS"
    echo "screenName=$REPUTATIONBAN_SCREEN_NAME"
    echo "createdAt=$STARTED_AT"
    if command -v "$REPUTATIONBAN_JAVA_BIN" >/dev/null 2>&1; then
      "$REPUTATIONBAN_JAVA_BIN" -version 2>&1 | sed 's/^/javaVersionLine=/'
    else
      echo "javaVersionLine=java command not found"
    fi
  } > "$ENVIRONMENT_FILE"
}

write_summary() {
  local status="$1"
  local result="$2"
  local reason="$3"
  local screen_session="${4:-}"
  local started_by_smoke="${5:-false}"
  local ended_at
  if [[ ! -s "$INTEGRATION_STATUS_FILE" ]]; then
    if [[ "$status" != "NOT_RUN" && -f "$SERVER_LOG" ]]; then
      write_integration_status_from_log
    else
      write_not_run_integration_status "${reason:-integration runtime smoke not run}"
    fi
  fi
  ended_at="$(date -Iseconds)"
  {
    echo "status=$status"
    echo "result=$result"
    echo "scenario=PaperPlugins"
    if [[ -n "$reason" ]]; then
      echo "reason=$reason"
    fi
    echo "version=$VERSION"
    echo "jar=$PLUGIN_JAR"
    echo "jarSha256=$JAR_SHA"
    echo "paperDir=$REPUTATIONBAN_PAPER_DIR"
    echo "startScript=$REPUTATIONBAN_PAPER_START_SCRIPT"
    echo "integrationPluginDir=$REPUTATIONBAN_INTEGRATION_PLUGIN_DIR"
    echo "stagedPlugins=$(join_by_comma "${STAGED_PLUGIN_BASENAMES[@]}")"
    echo "backedUpPlugins=$(join_by_comma "${BACKED_UP_PLUGIN_BASENAMES[@]}")"
    echo "activeIntegrations=$ACTIVE_INTEGRATIONS"
    echo "unavailableIntegrations=$UNAVAILABLE_INTEGRATIONS"
    if [[ -n "$DISCORDSRV_UNAVAILABLE_REASON" ]]; then
      echo "discordSrvUnavailableReason=$DISCORDSRV_UNAVAILABLE_REASON"
    fi
    echo "screenSession=$screen_session"
    echo "startedBySmoke=$started_by_smoke"
    echo "restoredPlugins=$RESTORED_PLUGINS"
    echo "reputationBanJarLeftInPlugins=true"
    echo "startedAt=$STARTED_AT"
    echo "endedAt=$ended_at"
    echo "manualPlayerNote=report_context generation for /reportbad and /reports evidence requires at least two real players."
  } > "$SUMMARY"
  cat "$SUMMARY"
}

capture_log() {
  local latest_log="$REPUTATIONBAN_PAPER_DIR/logs/latest.log"
  if [[ -f "$latest_log" ]]; then
    cp "$latest_log" "$SERVER_LOG"
  else
    echo "latest.log not found: $latest_log" > "$SERVER_LOG"
  fi
}

list_screen_sessions() {
  screen -ls 2>/dev/null \
    | awk '/[0-9]+\./ && $0 !~ /Dead/ {gsub(/^[ \t]+/, "", $0); split($1, parts, " "); print parts[1]}' \
    | sort -u
}

list_screen_names_from_file() {
  awk '/[0-9]+\./ && $0 !~ /Dead/ {gsub(/^[ \t]+/, "", $0); split($1, parts, " "); print parts[1]}' "$1" | sort -u
}

find_session_after_start() {
  local before_file="$1"
  local after_file="$2"
  local diff_file="${OUTDIR}/screen-new-sessions.txt"
  comm -13 <(list_screen_names_from_file "$before_file") <(list_screen_names_from_file "$after_file") > "$diff_file" || true
  if [[ "$(wc -l < "$diff_file" | tr -d ' ')" == "1" ]]; then
    cat "$diff_file"
    return 0
  fi
  return 1
}

find_candidate_session() {
  local candidates_file="${OUTDIR}/screen-candidates.txt"
  list_screen_sessions | grep -Ei '(paper|minecraft|(^|[._-])mc([._-]|$)|server)' > "$candidates_file" || true
  if [[ "$(wc -l < "$candidates_file" | tr -d ' ')" == "1" ]]; then
    cat "$candidates_file"
    return 0
  fi
  return 1
}

session_exists_in_file() {
  local session="$1"
  local file="$2"
  list_screen_names_from_file "$file" | grep -Fx "$session" >/dev/null
}

send_command() {
  local session="$1"
  local command="$2"
  echo "$command" >> "$COMMANDS_FILE"
  screen -S "$session" -p 0 -X stuff "$command$(printf '\r')"
}

stop_started_session_if_needed() {
  local session="$1"
  local started_by_smoke="$2"
  if [[ "$started_by_smoke" == "true" ]]; then
    screen -S "$session" -p 0 -X stuff "stop$(printf '\r')" >/dev/null 2>&1 || true
  fi
}

wait_for_startup() {
  local latest_log="$REPUTATIONBAN_PAPER_DIR/logs/latest.log"
  local require_new_startup="${1:-true}"
  local deadline=$((SECONDS + REPUTATIONBAN_SMOKE_TIMEOUT_SECONDS))
  local current_line_count
  local effective_start_line
  while (( SECONDS < deadline )); do
    if [[ -f "$latest_log" ]]; then
      if [[ "$require_new_startup" == "true" ]]; then
        current_line_count="$(wc -l < "$latest_log" | tr -d ' ')"
        effective_start_line="$LOG_START_LINE"
        if (( current_line_count < LOG_START_LINE )); then
          effective_start_line=1
        fi
        if tail -n +"$effective_start_line" "$latest_log" | grep -E 'Done \(|For help, type|Timings Reset' >/dev/null; then
          return 0
        fi
      elif grep -E 'Done \(|For help, type|Timings Reset' "$latest_log" >/dev/null; then
        return 0
      fi
    fi
    sleep 2
  done
  return 1
}

wait_for_stop() {
  local session="$1"
  local deadline=$((SECONDS + 60))
  while (( SECONDS < deadline )); do
    if ! screen -S "$session" -Q select . >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  return 1
}

backup_matching_plugins() {
  local patterns=(
    "ReputationBan*.jar"
    "LuckPerms*.jar"
    "CoreProtect*.jar"
    "WorldEdit*.jar"
    "WorldGuard*.jar"
    "GriefPrevention*.jar"
    "PlaceholderAPI*.jar"
    "DiscordSRV*.jar"
  )
  mkdir -p "$BACKUP_DIR"
  shopt -s nullglob
  for pattern in "${patterns[@]}"; do
    for existing in "$PLUGINS_DIR"/$pattern; do
      [[ -f "$existing" ]] || continue
      local base
      base="$(basename "$existing")"
      if [[ -f "$BACKUP_DIR/$base" ]]; then
        base="${base}.${STAMP}.bak"
      fi
      mv "$existing" "$BACKUP_DIR/$base"
      BACKED_UP_PLUGIN_BASENAMES+=("$base")
      echo "backedUp=$base" >> "$PLUGIN_RESTORE_FILE"
    done
  done
  shopt -u nullglob
}

stage_plugins() {
  mkdir -p "$PLUGINS_DIR"
  backup_matching_plugins

  local rb_target="$PLUGINS_DIR/$(basename "$PLUGIN_JAR")"
  cp "$PLUGIN_JAR" "$rb_target"
  echo "staged=$(basename "$PLUGIN_JAR")" >> "$STAGED_PLUGINS_FILE"

  local plugin_jar
  for plugin_jar in "${INTEGRATION_JARS[@]}"; do
    local base target
    base="$(basename "$plugin_jar")"
    target="$PLUGINS_DIR/$base"
    cp "$plugin_jar" "$target"
    STAGED_PLUGIN_BASENAMES+=("$base")
    STAGED_PLUGIN_PATHS+=("$target")
    echo "staged=$base" >> "$STAGED_PLUGINS_FILE"
  done
}

restore_plugins() {
  if [[ "$REPUTATIONBAN_INTEGRATION_RESTORE_PLUGINS" != "1" ]]; then
    echo "restoreSkipped=true" >> "$PLUGIN_RESTORE_FILE"
    RESTORED_PLUGINS=false
    return 0
  fi

  local staged_path
  for staged_path in "${STAGED_PLUGIN_PATHS[@]}"; do
    if [[ -f "$staged_path" ]]; then
      rm -f "$staged_path"
      echo "removedStaged=$(basename "$staged_path")" >> "$PLUGIN_RESTORE_FILE"
    fi
  done

  shopt -s nullglob
  for backed_up in "$BACKUP_DIR"/*.jar "$BACKUP_DIR"/*.bak; do
    [[ -f "$backed_up" ]] || continue
    mv "$backed_up" "$PLUGINS_DIR/$(basename "$backed_up" .bak)"
    echo "restored=$(basename "$backed_up")" >> "$PLUGIN_RESTORE_FILE"
  done
  shopt -u nullglob
  RESTORED_PLUGINS=true
}

inspect_log_result() {
  if grep -E 'NoClassDefFoundError|ClassNotFoundException|ExceptionInInitializerError|Could not load .*plugins/ReputationBan|Error occurred while enabling ReputationBan' "$SERVER_LOG" >/dev/null; then
    echo "runtime failure pattern found in server log"
    return 1
  fi
  if ! grep -q "ReputationBan" "$SERVER_LOG"; then
    echo "server log does not mention ReputationBan"
    return 1
  fi
  if ! grep -E "version:[[:space:]]*${VERSION}|${PROJECT_NAME}.*${VERSION}" "$SERVER_LOG" >/dev/null; then
    echo "server log does not show ReputationBan version ${VERSION}"
    return 1
  fi
  if ! grep -E "Doctor|databaseFileExists|overall:" "$SERVER_LOG" >/dev/null; then
    echo "server log does not show /rep doctor output"
    return 1
  fi
  if ! grep -E "LuckPerms|CoreProtect|WorldGuard|GriefPrevention|PlaceholderAPI|DiscordSRV|integrations" "$SERVER_LOG" >/dev/null; then
    echo "server log does not show integration command output"
    return 1
  fi
  return 0
}

write_environment

if [[ ! -f "$PLUGIN_JAR" ]]; then
  write_summary "NOT_RUN" "NOT_RUN" "built jar not found" "" "false"
  exit 0
fi

if [[ ! -d "$REPUTATIONBAN_INTEGRATION_PLUGIN_DIR" ]]; then
  write_summary "NOT_RUN" "NOT_RUN" "integration plugin directory not found" "" "false"
  exit 0
fi

shopt -s nullglob
INTEGRATION_JARS=("$REPUTATIONBAN_INTEGRATION_PLUGIN_DIR"/*.jar)
shopt -u nullglob
if [[ "${#INTEGRATION_JARS[@]}" == "0" ]]; then
  write_summary "NOT_RUN" "NOT_RUN" "integration plugin jar not found" "" "false"
  exit 0
fi

if [[ ! -d "$REPUTATIONBAN_PAPER_DIR" ]]; then
  write_summary "NOT_RUN" "NOT_RUN" "paper directory not found" "" "false"
  exit 0
fi

if [[ ! -f "$REPUTATIONBAN_PAPER_START_SCRIPT" ]]; then
  write_summary "NOT_RUN" "NOT_RUN" "start.sh not found" "" "false"
  exit 0
fi

if ! command -v screen >/dev/null 2>&1; then
  echo "screen command not found" > "$SCREEN_BEFORE_FILE"
  echo "screen command not found" > "$SCREEN_AFTER_FILE"
  write_summary "NOT_RUN" "NOT_RUN" "screen command not found" "" "false"
  exit 0
fi

screen -ls > "$SCREEN_BEFORE_FILE" 2>&1 || true

if ! stage_plugins; then
  write_summary "FAIL" "FAIL" "plugins directory is not writable" "" "false"
  exit 1
fi

STARTED_BY_SMOKE=false
START_EXIT=0
LATEST_LOG="$REPUTATIONBAN_PAPER_DIR/logs/latest.log"
if [[ -f "$LATEST_LOG" ]]; then
  LOG_START_LINE="$(($(wc -l < "$LATEST_LOG" | tr -d ' ') + 1))"
else
  LOG_START_LINE=1
fi
(
  cd "$REPUTATIONBAN_PAPER_DIR"
  if [[ -x "$REPUTATIONBAN_PAPER_START_SCRIPT" ]]; then
    "$REPUTATIONBAN_PAPER_START_SCRIPT"
  else
    bash "$REPUTATIONBAN_PAPER_START_SCRIPT"
  fi
) > "${OUTDIR}/start-script.out" 2>&1 || START_EXIT=$?

sleep 1
screen -ls > "$SCREEN_AFTER_FILE" 2>&1 || true

if [[ "$START_EXIT" != "0" ]]; then
  capture_log
  restore_plugins
  write_summary "FAIL" "FAIL" "start script exited with code ${START_EXIT}" "" "false"
  exit 1
fi

SCREEN_SESSION=""
if [[ -n "$REPUTATIONBAN_SCREEN_NAME" ]]; then
  SCREEN_SESSION="$REPUTATIONBAN_SCREEN_NAME"
elif SCREEN_SESSION="$(find_session_after_start "$SCREEN_BEFORE_FILE" "$SCREEN_AFTER_FILE")"; then
  STARTED_BY_SMOKE=true
elif SCREEN_SESSION="$(find_candidate_session)"; then
  if session_exists_in_file "$SCREEN_SESSION" "$SCREEN_BEFORE_FILE"; then
    STARTED_BY_SMOKE=false
  else
    STARTED_BY_SMOKE=true
  fi
else
  capture_log
  restore_plugins
  write_summary "FAIL" "FAIL" "screen session could not be identified" "" "false"
  exit 1
fi

if ! screen -S "$SCREEN_SESSION" -Q select . >/dev/null 2>&1; then
  capture_log
  restore_plugins
  write_summary "FAIL" "FAIL" "screen session is not reachable" "$SCREEN_SESSION" "$STARTED_BY_SMOKE"
  exit 1
fi

REQUIRE_NEW_STARTUP="$STARTED_BY_SMOKE"
if ! wait_for_startup "$REQUIRE_NEW_STARTUP"; then
  capture_log
  stop_started_session_if_needed "$SCREEN_SESSION" "$STARTED_BY_SMOKE"
  wait_for_stop "$SCREEN_SESSION" || true
  restore_plugins
  write_summary "FAIL" "FAIL" "server startup timed out" "$SCREEN_SESSION" "$STARTED_BY_SMOKE"
  exit 1
fi

if [[ "$STARTED_BY_SMOKE" == "true" ]]; then
  sleep 10
fi
sleep "$REPUTATIONBAN_SMOKE_COMMAND_DELAY_SECONDS"

commands=(
  "version"
  "plugins"
  "rep version"
  "rep doctor"
  "rep integrations"
  "rep integrations test"
  "rep placeholders"
  "reports help"
  "rep audit recent 5"
  "rep maintenance preview"
)

if [[ "$REPUTATIONBAN_SMOKE_MUTATING" == "1" ]]; then
  commands+=(
    "rep backup integration-runtime-smoke"
    "rep support bundle"
  )
fi

for command in "${commands[@]}"; do
  if ! send_command "$SCREEN_SESSION" "$command"; then
    capture_log
    stop_started_session_if_needed "$SCREEN_SESSION" "$STARTED_BY_SMOKE"
    wait_for_stop "$SCREEN_SESSION" || true
    restore_plugins
    write_summary "FAIL" "FAIL" "screen command injection failed: $command" "$SCREEN_SESSION" "$STARTED_BY_SMOKE"
    exit 1
  fi
  sleep 1
done

sleep 5

SHOULD_STOP=false
if [[ "$STARTED_BY_SMOKE" == "true" || "$REPUTATIONBAN_SMOKE_STOP_SERVER" == "1" ]]; then
  SHOULD_STOP=true
fi

if [[ "$SHOULD_STOP" == "true" ]]; then
  send_command "$SCREEN_SESSION" "stop"
  wait_for_stop "$SCREEN_SESSION" || true
fi

capture_log
restore_plugins

if INSPECTION_REASON="$(inspect_log_result 2>&1)"; then
  write_summary "PASS" "PASS" "" "$SCREEN_SESSION" "$STARTED_BY_SMOKE"
  exit 0
else
  write_summary "FAIL" "FAIL" "$INSPECTION_REASON" "$SCREEN_SESSION" "$STARTED_BY_SMOKE"
  exit 1
fi
