#!/usr/bin/env bash
set -euo pipefail

EXPECTED_VERSION="0.13.0"
JAR="build/libs/ReputationBan-${EXPECTED_VERSION}.jar"

fail() { echo "[FAIL] $*" >&2; exit 1; }
pass() { echo "[PASS] $*"; }

command -v jar >/dev/null 2>&1 || fail "jar command not found"
command -v sha256sum >/dev/null 2>&1 || fail "sha256sum command not found"

if [[ "${REPUTATIONBAN_SKIP_BUILD:-0}" != "1" ]]; then
  ./gradlew clean test build --warning-mode all
fi
if [[ "${REPUTATIONBAN_SKIP_REVIEW_CODE:-0}" != "1" ]]; then
  ./scripts/review_code.sh
fi

[[ -f "$JAR" ]] || fail "Missing JAR: $JAR"
grep -q "version: ${EXPECTED_VERSION}" src/main/resources/plugin.yml || fail "plugin.yml version is not ${EXPECTED_VERSION}"
jar tf "$JAR" | grep -q "plugin.yml" || fail "plugin.yml missing from JAR"
jar tf "$JAR" | grep -q "dev/modplugin/reputationban/ReputationBanPlugin.class" || fail "main plugin class missing from JAR"
[[ -f docs/runtime-smoke-checklist.md ]] || fail "Missing docs/runtime-smoke-checklist.md"

pass "Built $JAR"
sha256sum "$JAR"

cat <<'STEPS'

Manual runtime smoke:
Copy build/libs/ReputationBan-0.13.0.jar to your Paper 26.1.2 plugins directory.
Start Paper with Java 25.
Verify /plugins, /rep version, /rep help, /rep doctor, /rep support bundle, /reportbad tab completion, /rep audit recent.
Verify /rep audit export recent, /rep backup before-smoke, /rep support bundle, /rep maintenance preview, and /rep maintenance run confirm.
Confirm Discord webhook remains disabled by default and webhook URLs are not printed in logs.
Record results with ./scripts/record-paper-runtime-smoke-result.sh --result PASS --note "local Paper smoke passed".
STEPS
