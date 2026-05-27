#!/usr/bin/env bash
set -euo pipefail

STRICT=0

usage() {
  cat <<'USAGE'
Usage:
  ./scripts/check-paper-runtime-readiness.sh
  ./scripts/check-paper-runtime-readiness.sh --strict

Checks the latest:
  build/manual-smoke/paper-runtime-*/summary.txt

Normal mode reports HOLD/NOT_RUN as exit 0.
Strict mode requires result=PASS or status=PASS and exits non-zero otherwise.
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
  find build/manual-smoke -maxdepth 2 -path '*/paper-runtime-*/summary.txt' -type f 2>/dev/null \
    | sort \
    | tail -n 1
}

summary_value() {
  local key="$1"
  local file="$2"
  grep -E "^${key}=" "$file" | tail -n 1 | cut -d= -f2- || true
}

SUMMARY="$(latest_summary || true)"
RESULT="NOT_RUN"
STATUS="NOT_RUN"

if [[ -n "$SUMMARY" && -f "$SUMMARY" ]]; then
  RESULT="$(summary_value "result" "$SUMMARY")"
  STATUS="$(summary_value "status" "$SUMMARY")"
  RESULT="${RESULT:-$STATUS}"
  RESULT="${RESULT:-UNKNOWN}"
  STATUS="${STATUS:-$RESULT}"
fi

case "$RESULT:$STATUS" in
  PASS:*|*:PASS)
    echo "paper runtime smoke: PASS"
    echo "summary: $SUMMARY"
    echo "judgment: READY_FOR_PAPER_RUNTIME_RELEASE_REVIEW"
    exit 0
    ;;
  FAIL:*|*:FAIL)
    echo "paper runtime smoke: FAIL"
    echo "summary: $SUMMARY"
    echo "judgment: FAIL_PAPER_RUNTIME_SMOKE"
    [[ "$STRICT" == "1" ]] && exit 1
    exit 0
    ;;
  NOT_RUN:*|*:NOT_RUN)
    echo "paper runtime smoke: NOT_RUN"
    echo "summary: ${SUMMARY:--}"
    echo "judgment: HOLD_FOR_PAPER_RUNTIME_SMOKE"
    [[ "$STRICT" == "1" ]] && exit 1
    exit 0
    ;;
  *)
    echo "paper runtime smoke: $RESULT"
    echo "summary: ${SUMMARY:--}"
    echo "judgment: HOLD_FOR_PAPER_RUNTIME_SMOKE"
    [[ "$STRICT" == "1" ]] && exit 1
    exit 0
    ;;
esac
