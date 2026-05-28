#!/usr/bin/env bash
set -euo pipefail

EXPECTED_VERSION="1.0.0"
EXPECTED_TAG="v1.0.0"
EXPECTED_TAG_COMMIT="b422e72ec5a917cdc04dee902e96a0cef190026c"
RELEASE_URL="https://github.com/Nobu0707/ReputationBan/releases/tag/v1.0.0"

fail() { echo "[FAIL] $*" >&2; exit 1; }
pass() { echo "[PASS] $*"; }
warn() { echo "[WARN] $*" >&2; }
require_file() { [[ -f "$1" ]] || fail "Missing required file: $1"; pass "Found $1"; }

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

for file in \
  docs/MAINTENANCE_BASELINE.md \
  docs/ISSUE_TRIAGE_GUIDE.md \
  docs/phase-36.md \
  SECURITY.md \
  SUPPORT.md \
  CONTRIBUTING.md \
  .github/ISSUE_TEMPLATE/bug_report.yml \
  .github/ISSUE_TEMPLATE/integration_issue.yml \
  .github/ISSUE_TEMPLATE/support_request.yml \
  .github/ISSUE_TEMPLATE/feature_request.yml \
  .github/ISSUE_TEMPLATE/config.yml \
  .github/pull_request_template.md; do
  require_file "$file"
done

grep -q "version = \"${EXPECTED_VERSION}\"" build.gradle.kts || fail "build.gradle.kts version is not ${EXPECTED_VERSION}"
grep -q "^version:[[:space:]]*${EXPECTED_VERSION}$" src/main/resources/plugin.yml || fail "plugin.yml version is not ${EXPECTED_VERSION}"
pass "Version remains ${EXPECTED_VERSION}"

[[ -n "$(git tag --list "$EXPECTED_TAG")" ]] || fail "${EXPECTED_TAG} tag is missing"
TAG_COMMIT="$(git rev-list -n 1 "$EXPECTED_TAG")"
[[ "$TAG_COMMIT" == "$EXPECTED_TAG_COMMIT" ]] || fail "${EXPECTED_TAG} points to ${TAG_COMMIT}, expected ${EXPECTED_TAG_COMMIT}"
pass "${EXPECTED_TAG} tag points to the Phase 30 release commit"

if git ls-remote --tags origin "refs/tags/${EXPECTED_TAG}^{}" >/tmp/reputationban-maintenance-remote-tag.txt 2>/tmp/reputationban-maintenance-remote-tag.err; then
  REMOTE_TAG_COMMIT="$(awk '{print $1}' /tmp/reputationban-maintenance-remote-tag.txt | tail -n 1)"
  if [[ -z "$REMOTE_TAG_COMMIT" ]]; then
    git ls-remote --tags origin "refs/tags/${EXPECTED_TAG}" >/tmp/reputationban-maintenance-remote-tag.txt 2>/tmp/reputationban-maintenance-remote-tag.err || true
    REMOTE_TAG_COMMIT="$(awk '{print $1}' /tmp/reputationban-maintenance-remote-tag.txt | tail -n 1)"
  fi
  if [[ -n "$REMOTE_TAG_COMMIT" ]]; then
    pass "Remote ${EXPECTED_TAG} tag exists"
  else
    warn "Remote ${EXPECTED_TAG} tag could not be confirmed"
  fi
else
  warn "Remote ${EXPECTED_TAG} tag check skipped: $(tr '\n' ' ' </tmp/reputationban-maintenance-remote-tag.err)"
fi
rm -f /tmp/reputationban-maintenance-remote-tag.txt /tmp/reputationban-maintenance-remote-tag.err

if ! command -v gh >/dev/null 2>&1; then
  warn "gh unavailable; GitHub Release published status not checked"
  echo "ghAvailable=false"
  echo "releaseStatus=not_checked"
  exit 0
fi

set +e
RELEASE_JSON="$(gh release view "$EXPECTED_TAG" --json tagName,name,isDraft,isPrerelease,url,assets,body 2>&1)"
RELEASE_CODE=$?
set -e

if [[ "$RELEASE_CODE" != "0" ]]; then
  warn "GitHub Release check skipped: ${RELEASE_JSON//$'\n'/ }"
  echo "ghAvailable=true"
  echo "releaseStatus=not_checked"
  exit 0
fi

[[ "$RELEASE_JSON" == *'"tagName":"v1.0.0"'* ]] || fail "GitHub Release tagName is not v1.0.0"
[[ "$RELEASE_JSON" == *'"isDraft":false'* ]] || fail "GitHub Release v1.0.0 is not published"
[[ "$RELEASE_JSON" == *'"isPrerelease":false'* ]] || fail "GitHub Release v1.0.0 is prerelease"
[[ "$RELEASE_JSON" == *"\"url\":\"$RELEASE_URL\""* ]] || fail "GitHub Release URL mismatch"

for asset in \
  "ReputationBan-1.0.0.jar" \
  "ReputationBan-1.0.0.jar.sha256" \
  "ReputationBan-1.0.0-release.zip" \
  "ReputationBan-1.0.0-release.zip.sha256"; do
  [[ "$RELEASE_JSON" == *"\"name\":\"$asset\""* ]] || fail "GitHub Release v1.0.0 missing asset: $asset"
done

if printf '%s\n' "$RELEASE_JSON" | grep -E "DRAFT_TO_CREATE|公開はまだ|draft 作成まで" >/dev/null; then
  fail "Published GitHub Release notes still contain pre-publish wording"
fi

pass "GitHub Release v1.0.0 is published with expected assets"
