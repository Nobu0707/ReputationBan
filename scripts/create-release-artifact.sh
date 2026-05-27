#!/usr/bin/env bash
set -euo pipefail

VERSION="0.25.0"
PROJECT_NAME="ReputationBan"
JAR_NAME="${PROJECT_NAME}-${VERSION}.jar"
SOURCE_JAR="build/libs/${JAR_NAME}"
RELEASE_DIR="build/release"
RELEASE_ZIP="${RELEASE_DIR}/${PROJECT_NAME}-${VERSION}-release.zip"
RELEASE_ZIP_SHA="${RELEASE_ZIP}.sha256"

fail() { echo "[FAIL] $*" >&2; exit 1; }
pass() { echo "[PASS] $*"; }

command -v jar >/dev/null 2>&1 || fail "jar command not found"
command -v sha256sum >/dev/null 2>&1 || fail "sha256sum command not found"

if [[ ! -f "$SOURCE_JAR" ]]; then
  ./gradlew clean test build --warning-mode all
fi

[[ -f "$SOURCE_JAR" ]] || fail "Missing built JAR: $SOURCE_JAR"
mkdir -p "$RELEASE_DIR"
rm -f "${RELEASE_DIR}/${JAR_NAME}" "${RELEASE_DIR}/${JAR_NAME}.sha256" "$RELEASE_ZIP" "$RELEASE_ZIP_SHA"

cp "$SOURCE_JAR" "${RELEASE_DIR}/${JAR_NAME}"
sha256sum "${RELEASE_DIR}/${JAR_NAME}" > "${RELEASE_DIR}/${JAR_NAME}.sha256"

jar --create --file "$RELEASE_ZIP" \
  -C "$RELEASE_DIR" "$JAR_NAME" \
  -C "$RELEASE_DIR" "${JAR_NAME}.sha256" \
  -C . README.md \
  -C . CHANGELOG.md \
  -C . docs

if jar tf "$RELEASE_ZIP" | grep -E '(^|/)(config\.yml|reputationban\.db|latest\.log|debug\.log)$|(^|/)logs/' >/dev/null; then
  fail "Release zip contains a forbidden config, DB, or log file"
fi

sha256sum "$RELEASE_ZIP" > "$RELEASE_ZIP_SHA"

pass "Created ${RELEASE_DIR}/${JAR_NAME}"
pass "Created ${RELEASE_DIR}/${JAR_NAME}.sha256"
pass "Created $RELEASE_ZIP"
pass "Created $RELEASE_ZIP_SHA"
