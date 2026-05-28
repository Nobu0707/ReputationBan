#!/usr/bin/env bash
set -euo pipefail

VERSION="1.0.0"
PROJECT_NAME="ReputationBan"
RELEASE_DIR="build/release"
JAR_PATH="${RELEASE_DIR}/${PROJECT_NAME}-${VERSION}.jar"
RELEASE_ZIP="${RELEASE_DIR}/${PROJECT_NAME}-${VERSION}-release.zip"
STRICT=0
REQUIRE_DISCORDSRV=0

usage() {
  cat <<'USAGE'
Usage:
  ./scripts/check-v1-release-gates.sh
  ./scripts/check-v1-release-gates.sh --strict
  ./scripts/check-v1-release-gates.sh --strict --require-discordsrv

Checks the v1.0.0 release review gates without creating a v1.0.0 tag or GitHub Release.
If v1.0.0 already exists, it must point at HEAD.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --strict)
      STRICT=1
      shift
      ;;
    --require-discordsrv)
      REQUIRE_DISCORDSRV=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

run_capture() {
  local output="$1"
  shift
  set +e
  "$@" > "$output" 2>&1
  local code=$?
  set -e
  return "$code"
}

latest_summary() {
  local kind="$1"
  find build/manual-smoke -maxdepth 2 -path "*/${kind}-*/summary.txt" -type f 2>/dev/null \
    | sort \
    | tail -n 1
}

mark_gate() {
  local key="$1"
  local output="$2"
  shift 2
  if run_capture "$output" "$@"; then
    echo "${key}=PASS"
    return 0
  fi
  echo "${key}=FAIL"
  return 1
}

secret_scan() {
  ! grep -RE "https://(canary\\.|ptb\\.)?discord(app)?\\.com/api/webhooks/[0-9]+/[A-Za-z0-9_-]{20,}" \
    src/main/java src/main/resources docs scripts README.md CHANGELOG.md reputationban_phase_plan.md >/dev/null
}

destructive_integration_scan() {
  if grep -R "performRollback\\|performRestore\\|performPurge" src/main/java >/dev/null; then
    return 1
  fi
  if grep -R "saveUser\\|setPermission\\|data()\\.add\\|data()\\.remove" src/main/java >/dev/null; then
    return 1
  fi
  if grep -R "addRegion\\|removeRegion\\|saveChanges\\|setFlag\\|setPriority\\|setOwners\\|setMembers" src/main/java >/dev/null; then
    return 1
  fi
  if grep -R "createClaim\\|deleteClaim\\|resizeClaim\\|changeClaimOwner\\|setOwner\\|setManagers\\|setBuilders\\|setContainers\\|setAccessors" src/main/java >/dev/null; then
    return 1
  fi
  if grep -R "dispatchCommand\\|performCommand" src/main/java/dev/modplugin/reputationban/integration/discordsrv src/main/java/dev/modplugin/reputationban/notification 2>/dev/null; then
    return 1
  fi
  if grep -R "addRole\\|removeRole\\|modifyMemberRoles\\|RoleManager" src/main/java/dev/modplugin/reputationban/integration/discordsrv src/main/java/dev/modplugin/reputationban/notification 2>/dev/null; then
    return 1
  fi
}

v1_tag_is_safe() {
  local head tag_commit
  if [[ -z "$(git tag --list "v1.0.0")" ]]; then
    echo "v1Tag=NOT_CREATED"
    return 0
  fi
  head="$(git rev-parse HEAD)"
  tag_commit="$(git rev-list -n 1 v1.0.0)"
  if [[ "$head" == "$tag_commit" ]]; then
    echo "v1Tag=CREATED_MATCHES_HEAD"
    return 0
  fi
  echo "v1Tag=FAIL_POINTS_TO_${tag_commit}"
  return 1
}

discordsrv_state() {
  local summary status_file
  summary="$(latest_summary integration-runtime || true)"
  if [[ -n "$summary" && -f "$summary" ]]; then
    status_file="$(dirname "$summary")/integration-status.txt"
    if [[ -f "$status_file" ]]; then
      if grep -q '^DiscordSRV=active$' "$status_file"; then
        echo "PASS"
        return 0
      fi
      if grep -q '^DiscordSRV=unavailable$' "$status_file"; then
        echo "WARNING_UNAVAILABLE"
        return 0
      fi
    fi
    if grep -q '^unavailableIntegrations=.*DiscordSRV' "$summary"; then
      echo "WARNING_UNAVAILABLE"
      return 0
    fi
  fi
  echo "WARNING_UNCONFIRMED"
}

ensure_release_artifact() {
  ./scripts/create-release-artifact.sh
  ./scripts/verify-release-artifact.sh
}

FAILED=0
echo "v1 release gates:"
echo "version=$VERSION"

if ! v1_tag_is_safe; then
  FAILED=1
fi

mark_gate "paperRuntime" "$TMP_DIR/paper-runtime.txt" ./scripts/check-paper-runtime-readiness.sh --strict || FAILED=1
mark_gate "integrationRuntime" "$TMP_DIR/integration-runtime.txt" ./scripts/check-integration-runtime-readiness.sh --strict || FAILED=1
mark_gate "playerReportRuntime" "$TMP_DIR/player-report-runtime.txt" ./scripts/check-player-report-runtime-readiness.sh --strict || FAILED=1
mark_gate "runtimeSmokeConsistency" "$TMP_DIR/runtime-smoke-consistency.txt" ./scripts/check-runtime-smoke-consistency.sh || FAILED=1
mark_gate "optionalDependencySafety" "$TMP_DIR/optional-dependency-safety.txt" ./scripts/check-optional-dependency-safety.sh || FAILED=1
mark_gate "docsLocalization" "$TMP_DIR/docs-localization.txt" ./scripts/check-docs-localization.sh || FAILED=1
mark_gate "releaseArtifact" "$TMP_DIR/release-artifact.txt" ensure_release_artifact || FAILED=1

if secret_scan; then
  echo "secretScan=PASS"
else
  echo "secretScan=FAIL"
  FAILED=1
fi

if destructive_integration_scan; then
  echo "destructiveIntegrationOperations=PASS"
else
  echo "destructiveIntegrationOperations=FAIL"
  FAILED=1
fi

DISCORDSRV="$(discordsrv_state)"
echo "discordSrv=$DISCORDSRV"

if [[ "$REQUIRE_DISCORDSRV" == "1" && "$DISCORDSRV" != "PASS" ]]; then
  echo "judgment=HOLD_FOR_DISCORDSRV_RUNTIME_SMOKE"
  exit 1
fi

if [[ "$FAILED" != "0" ]]; then
  echo "judgment=HOLD_FOR_V1_RELEASE"
  exit 1
fi

if [[ "$DISCORDSRV" == WARNING_* ]]; then
  echo "judgment=READY_FOR_V1_RELEASE_WITH_DISCORDSRV_WARNING"
else
  echo "judgment=READY_FOR_V1_RELEASE"
fi

exit 0
