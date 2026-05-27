#!/usr/bin/env bash
set -euo pipefail

VERSION="0.17.0"
PROJECT_NAME="ReputationBan"
JAR_NAME="${PROJECT_NAME}-${VERSION}.jar"
RELEASE_DIR="build/release"
JAR_PATH="${RELEASE_DIR}/${JAR_NAME}"
JAR_SHA="${JAR_PATH}.sha256"
RELEASE_ZIP="${RELEASE_DIR}/${PROJECT_NAME}-${VERSION}-release.zip"
RELEASE_ZIP_SHA="${RELEASE_ZIP}.sha256"
ROOT="$(pwd)"
VERIFY_DIR="build/tmp/release-verify"

fail() { echo "[FAIL] $*" >&2; exit 1; }
pass() { echo "[PASS] $*"; }
require_command() { command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"; }
require_file() { [[ -f "$1" ]] || fail "Missing required file: $1"; pass "Found $1"; }
require_japanese_text() { grep -Pq '[\p{Hiragana}\p{Katakana}\p{Han}]' "$1" || fail "$1 does not appear to contain Japanese text"; pass "$1 contains Japanese text"; }
zip_contains() { jar tf "$RELEASE_ZIP" | grep -q "$1" || fail "Release zip missing: $1"; }
zip_forbids() {
  if jar tf "$RELEASE_ZIP" | grep -E "$1" >/dev/null; then
    fail "Release zip contains forbidden entry matching: $1"
  fi
}

require_command jar
require_command grep
require_command sha256sum
require_command mktemp

require_file "$JAR_PATH"
require_file "$JAR_SHA"
require_file "$RELEASE_ZIP"
require_file "$RELEASE_ZIP_SHA"

sha256sum -c "$JAR_SHA"
sha256sum -c "$RELEASE_ZIP_SHA"

zip_contains "^${JAR_NAME}$"
zip_contains "^${JAR_NAME}.sha256$"
zip_contains "^README.md$"
zip_contains "^CHANGELOG.md$"
zip_contains "^docs/INSTALLATION.md$"
zip_contains "^docs/CONFIGURATION.md$"
zip_contains "^docs/INTEGRATIONS.md$"
zip_contains "^docs/INTEGRATION_RUNTIME_SMOKE_CHECKLIST.md$"
zip_contains "^docs/MIGRATION.md$"
zip_contains "^docs/RELEASE_READINESS.md$"
zip_contains "^docs/SUPPORT_BUNDLE.md$"
zip_contains "^docs/SECURITY_REDACTION.md$"
zip_contains "^docs/PAPER_RUNTIME_SMOKE_REPORT_TEMPLATE.md$"

zip_forbids '(^|/)(config\.yml|reputationban\.db|reputationban\.db-wal|reputationban\.db-shm|latest\.log|debug\.log)$|(^|/)logs/'

jar tf "$JAR_PATH" | grep -q "^plugin.yml$" || fail "plugin.yml missing from JAR"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
(cd "$TMP_DIR" && jar xf "$ROOT/$JAR_PATH" plugin.yml)
grep -q "^version:[[:space:]]*${VERSION}$" "$TMP_DIR/plugin.yml" \
  || fail "JAR plugin.yml version is not ${VERSION}"

rm -rf "$VERIFY_DIR"
mkdir -p "$VERIFY_DIR"
(cd "$VERIFY_DIR" && jar xf "$ROOT/$RELEASE_ZIP")

require_japanese_text "$VERIFY_DIR/README.md"
require_japanese_text "$VERIFY_DIR/docs/INSTALLATION.md"

if grep -RE "https://(canary\\.|ptb\\.)?discord(app)?\\.com/api/webhooks/[0-9]+/[A-Za-z0-9_-]{20,}" "$VERIFY_DIR" >/dev/null; then
  fail "Release zip contains concrete Discord webhook URL-like value"
fi

if find "$VERIFY_DIR" -type f \( -name 'reputationban.db' -o -name 'reputationban.db-wal' -o -name 'reputationban.db-shm' -o -name 'config.yml' \) | grep -q .; then
  fail "Release zip extracted contents include forbidden config or DB file"
fi
if find "$VERIFY_DIR" -type d -name logs | grep -q .; then
  fail "Release zip extracted contents include logs directory"
fi

pass "Release artifact verification completed"
