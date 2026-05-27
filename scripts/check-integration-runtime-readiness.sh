#!/usr/bin/env bash
set -euo pipefail

STRICT=0

usage() {
  cat <<'USAGE'
Usage:
  ./scripts/check-integration-runtime-readiness.sh
  ./scripts/check-integration-runtime-readiness.sh --strict

Checks the latest:
  build/manual-smoke/integration-runtime-*/summary.txt

Normal mode reports HOLD/NOT_RUN as exit 0.
Strict mode requires result=PASS and exits non-zero otherwise.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --strict)
      STRICT=1
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

latest_summary() {
  find build/manual-smoke -maxdepth 2 -path '*/integration-runtime-*/summary.txt' -type f 2>/dev/null \
    | sort \
    | tail -n 1
}

SUMMARY="$(latest_summary || true)"
RESULT="NOT_RUN"
STATUS="NOT_RUN"

if [[ -n "$SUMMARY" && -f "$SUMMARY" ]]; then
  RESULT="$(grep -E '^result=' "$SUMMARY" | tail -n 1 | cut -d= -f2- || true)"
  STATUS="$(grep -E '^status=' "$SUMMARY" | tail -n 1 | cut -d= -f2- || true)"
  RESULT="${RESULT:-UNKNOWN}"
  STATUS="${STATUS:-UNKNOWN}"
fi

discord_srv_warning() {
  local status_file
  if [[ -z "$SUMMARY" || ! -f "$SUMMARY" ]]; then
    return 0
  fi
  status_file="$(dirname "$SUMMARY")/integration-status.txt"
  if [[ -f "$status_file" ]] && grep -q '^DiscordSRV=unavailable$' "$status_file"; then
    echo "warning: DiscordSRV unavailable is treated as WARN for this release gate."
    if grep -q '^DiscordSRV.note=' "$status_file"; then
      grep '^DiscordSRV.note=' "$status_file" | tail -n 1 | sed 's/^DiscordSRV.note=/warningDetail: /'
    else
      echo "warningDetail: Bot token not configured or API unavailable"
    fi
  fi
}

if [[ "$RESULT" == "PASS" || "$STATUS" == "PASS" ]]; then
  echo "integration runtime smoke: PASS"
  echo "summary: $SUMMARY"
  discord_srv_warning
  echo "judgment: READY_FOR_INTEGRATION_RUNTIME_RELEASE_REVIEW"
  exit 0
fi

case "$RESULT" in
  PASS)
    echo "integration runtime smoke: PASS"
    echo "summary: $SUMMARY"
    discord_srv_warning
    echo "judgment: READY_FOR_INTEGRATION_RUNTIME_RELEASE_REVIEW"
    exit 0
    ;;
  FAIL)
    echo "integration runtime smoke: FAIL"
    echo "summary: $SUMMARY"
    echo "judgment: FAIL_INTEGRATION_RUNTIME_SMOKE"
    [[ "$STRICT" == "1" ]] && exit 1
    exit 0
    ;;
  NOT_RUN)
    echo "integration runtime smoke: NOT_RUN"
    echo "summary: -"
    echo "judgment: HOLD_FOR_INTEGRATION_RUNTIME_SMOKE"
    [[ "$STRICT" == "1" ]] && exit 1
    exit 0
    ;;
  *)
    echo "integration runtime smoke: $RESULT"
    echo "summary: $SUMMARY"
    echo "judgment: HOLD_FOR_INTEGRATION_RUNTIME_SMOKE"
    [[ "$STRICT" == "1" ]] && exit 1
    exit 0
    ;;
esac
