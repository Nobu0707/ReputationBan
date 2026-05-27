#!/usr/bin/env bash
set -euo pipefail

EXPECTED_VERSION="0.11.0"
PLUGIN_JAR="build/libs/ReputationBan-${EXPECTED_VERSION}.jar"

usage() {
  cat <<'USAGE'
ReputationBan Paper runtime smoke helper

Set these environment variables to prepare a Paper test server:
  REPUTATIONBAN_PAPER_DIR="$HOME/servers/paper-26.1.2-test"
  REPUTATIONBAN_PAPER_JAR="paper-26.1.2-65.jar"
  REPUTATIONBAN_JAVA_BIN="java"

Then run:
  ./scripts/run-paper-runtime-smoke-helper.sh

The helper copies the built ReputationBan JAR into the server plugins directory,
backs up existing ReputationBan plugin JARs, and prints manual smoke commands.
It does not delete server directories or databases.
USAGE
}

if [[ -z "${REPUTATIONBAN_PAPER_DIR:-}" || -z "${REPUTATIONBAN_PAPER_JAR:-}" || -z "${REPUTATIONBAN_JAVA_BIN:-}" ]]; then
  usage
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

Manual runtime smoke:
  /plugins
  /rep version
  /rep help
  /rep doctor
  /reportbad <test-player> spam smoke-test
  /reports list all 10
  /rep audit recent 10
  /rep maintenance preview

Safety checks:
  Confirm plugins/ReputationBan/reputationban.db still exists or is created on first boot.
  Confirm Discord webhook remains disabled unless intentionally configured.
  Confirm no Discord webhook URL is printed in console or logs.
  Use only test users for BAN-related commands.
STEPS
