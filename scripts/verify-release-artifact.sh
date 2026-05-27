#!/usr/bin/env bash
set -euo pipefail

VERSION="0.13.0"
PROJECT_NAME="ReputationBan"
JAR_NAME="${PROJECT_NAME}-${VERSION}.jar"
RELEASE_DIR="build/release"
JAR_PATH="${RELEASE_DIR}/${JAR_NAME}"
JAR_SHA="${JAR_PATH}.sha256"
RELEASE_ZIP="${RELEASE_DIR}/${PROJECT_NAME}-${VERSION}-release.zip"
RELEASE_ZIP_SHA="${RELEASE_ZIP}.sha256"
ROOT="$(pwd)"

fail() { echo "[FAIL] $*" >&2; exit 1; }
pass() { echo "[PASS] $*"; }
require_command() { command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"; }
require_file() { [[ -f "$1" ]] || fail "Missing required file: $1"; pass "Found $1"; }
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
zip_contains "^docs/"

zip_forbids '(^|/)(config\.yml|reputationban\.db|reputationban\.db-wal|reputationban\.db-shm|latest\.log|debug\.log)$|(^|/)logs/'

jar tf "$JAR_PATH" | grep -q "^plugin.yml$" || fail "plugin.yml missing from JAR"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
(cd "$TMP_DIR" && jar xf "$ROOT/$JAR_PATH" plugin.yml)
grep -q "^version:[[:space:]]*${VERSION}$" "$TMP_DIR/plugin.yml" \
  || fail "JAR plugin.yml version is not ${VERSION}"

pass "Release artifact verification completed"
