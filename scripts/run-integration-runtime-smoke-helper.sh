#!/usr/bin/env bash
set -euo pipefail

EXPECTED_VERSION="0.28.0"
PLUGIN_JAR="build/libs/ReputationBan-${EXPECTED_VERSION}.jar"

usage() {
  cat <<'USAGE'
ReputationBan integration runtime smoke helper

Set these environment variables to copy the built JAR into a Paper test server:
  REPUTATIONBAN_PAPER_DIR="$HOME/servers/paper-26.1.2-test"
  REPUTATIONBAN_PAPER_JAR="paper-26.1.2-65.jar"
  REPUTATIONBAN_JAVA_BIN="java"

Then run:
  ./scripts/run-integration-runtime-smoke-helper.sh

If those variables are not set, this helper prints the manual integration smoke plan and exits 0.
It never deletes server directories, databases, logs, or optional plugin data.
USAGE
}

print_plan() {
  cat <<'STEPS'

JAR copy steps:
  1. Build with ./gradlew clean test build --warning-mode all
  2. Copy build/libs/ReputationBan-0.28.0.jar into the Paper server plugins directory
  3. Start PaperMC 26.1.2 with Java 25

Optional plugin combinations:
  - no optional plugins
  - LuckPerms only
  - CoreProtect only
  - WorldEdit + WorldGuard
  - GriefPrevention
  - PlaceholderAPI
  - DiscordSRV
  - all integrations

Confirmation commands:
  /rep integrations
  /rep integrations test
  /rep doctor
  /reportbad <player> griefing <reason>
  /reports evidence <id>
  /rep placeholders
  PlaceholderAPI installed: /papi parse <player> %reputationban_score%

DiscordSRV installed:
  - Confirm account link context appears for linked and unlinked players.
  - Confirm integrations.discordsrv.notifications.enabled is false by default.
  - Confirm Discord does not execute Minecraft commands.
  - Enable DiscordSRV notifications only for an intentional smoke check, then disable them again.

Safety boundaries:
  - Do not run CoreProtect rollback, restore, or purge.
  - Do not change LuckPerms groups or permissions.
  - Do not create, edit, or delete WorldGuard regions or flags.
  - Do not create, edit, or delete GriefPrevention claims or trust.
  - Do not configure Discord role changes, buttons, or Minecraft command execution from Discord.

Result recording:
  ./scripts/record-integration-runtime-smoke-result.sh --result PASS --scenario "All integrations" --note "manual smoke passed"
  ./scripts/record-integration-runtime-smoke-result.sh --result FAIL --scenario "DiscordSRV" --note "describe failure"
STEPS
}

if [[ -z "${REPUTATIONBAN_PAPER_DIR:-}" || -z "${REPUTATIONBAN_PAPER_JAR:-}" || -z "${REPUTATIONBAN_JAVA_BIN:-}" ]]; then
  usage
  print_plan
  exit 0
fi

fail() { echo "[FAIL] $*" >&2; exit 1; }
info() { echo "[INFO] $*"; }

[[ -f "$PLUGIN_JAR" ]] || fail "Missing plugin JAR: $PLUGIN_JAR"
[[ -d "$REPUTATIONBAN_PAPER_DIR" ]] || fail "Paper directory does not exist: $REPUTATIONBAN_PAPER_DIR"
[[ -f "$REPUTATIONBAN_PAPER_DIR/$REPUTATIONBAN_PAPER_JAR" ]] || fail "Paper JAR not found: $REPUTATIONBAN_PAPER_DIR/$REPUTATIONBAN_PAPER_JAR"

PLUGINS_DIR="$REPUTATIONBAN_PAPER_DIR/plugins"
BACKUP_DIR="$REPUTATIONBAN_PAPER_DIR/reputationban-plugin-backups"
STAMP="$(date +%Y%m%d-%H%M%S)"

mkdir -p "$PLUGINS_DIR" "$BACKUP_DIR"

shopt -s nullglob
for existing in "$PLUGINS_DIR"/ReputationBan-*.jar "$PLUGINS_DIR"/ReputationBan.jar; do
  base="$(basename "$existing")"
  info "Backing up existing plugin JAR: plugins/$base"
  mv "$existing" "$BACKUP_DIR/${STAMP}-$base"
done
shopt -u nullglob

cp "$PLUGIN_JAR" "$PLUGINS_DIR/$(basename "$PLUGIN_JAR")"

info "Copied $(basename "$PLUGIN_JAR") to $PLUGINS_DIR"
cat <<STEPS

Start command example:
  cd "$REPUTATIONBAN_PAPER_DIR"
  "$REPUTATIONBAN_JAVA_BIN" -Xms1G -Xmx2G -jar "$REPUTATIONBAN_PAPER_JAR" nogui
STEPS

print_plan
