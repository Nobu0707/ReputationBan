#!/usr/bin/env bash
set -euo pipefail

CHECKS_DIR=""

usage() {
  cat <<'USAGE'
Usage:
  ./scripts/check-runtime-smoke-consistency.sh
  ./scripts/check-runtime-smoke-consistency.sh --checks-dir build/review-temp/checks

Checks that the latest Paper and integration runtime smoke summaries match
their readiness outputs. PASS summaries must have READY/PASS readiness, and
NOT_RUN summaries must have HOLD/NOT_RUN readiness.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --checks-dir)
      CHECKS_DIR="${2:-}"
      shift 2
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

summary_value() {
  local file="$1"
  local result status
  if [[ -z "$file" || ! -f "$file" ]]; then
    echo "NOT_RUN"
    return 0
  fi
  result="$(grep -E '^result=' "$file" | tail -n 1 | cut -d= -f2- || true)"
  status="$(grep -E '^status=' "$file" | tail -n 1 | cut -d= -f2- || true)"
  if [[ -n "$result" ]]; then
    echo "$result"
  elif [[ -n "$status" ]]; then
    echo "$status"
  else
    echo "UNKNOWN"
  fi
}

latest_summary() {
  local kind="$1"
  find build/manual-smoke -maxdepth 2 -path "*/${kind}-*/summary.txt" -type f 2>/dev/null \
    | sort \
    | tail -n 1
}

checks_summary_file() {
  local name="$1"
  if [[ -n "$CHECKS_DIR" && -f "$CHECKS_DIR/$name" ]]; then
    echo "$CHECKS_DIR/$name"
  fi
}

readiness_output() {
  local name="$1"
  local script="$2"
  if [[ -n "$CHECKS_DIR" && -f "$CHECKS_DIR/$name" ]]; then
    cat "$CHECKS_DIR/$name"
    return 0
  fi
  if [[ -x "$script" ]]; then
    "$script" || true
  else
    echo "runtime smoke: NOT_RUN"
    echo "judgment: HOLD"
  fi
}

readiness_state() {
  local text="$1"
  if grep -Eiq 'READY|runtime smoke:[[:space:]]*PASS|judgment:.*PASS' <<<"$text"; then
    echo "READY"
  elif grep -Eiq 'FAIL' <<<"$text"; then
    echo "FAIL"
  elif grep -Eiq 'HOLD|NOT_RUN' <<<"$text"; then
    echo "HOLD"
  else
    echo "UNKNOWN"
  fi
}

check_pair() {
  local label="$1"
  local summary="$2"
  local readiness="$3"
  case "$summary" in
    PASS)
      [[ "$readiness" == "READY" ]]
      ;;
    NOT_RUN)
      [[ "$readiness" == "HOLD" ]]
      ;;
    FAIL)
      [[ "$readiness" == "FAIL" ]]
      ;;
    *)
      [[ "$readiness" == "HOLD" || "$readiness" == "UNKNOWN" ]]
      ;;
  esac
}

paper_summary_file="$(checks_summary_file latest-paper-runtime-smoke-summary.txt)"
paper_summary_file="${paper_summary_file:-$(latest_summary paper-runtime || true)}"
integration_summary_file="$(checks_summary_file latest-integration-runtime-smoke-summary.txt)"
integration_summary_file="${integration_summary_file:-$(latest_summary integration-runtime || true)}"

paper_summary="$(summary_value "$paper_summary_file")"
integration_summary="$(summary_value "$integration_summary_file")"

paper_readiness_text="$(readiness_output paper-runtime-readiness.txt ./scripts/check-paper-runtime-readiness.sh)"
integration_readiness_text="$(readiness_output integration-runtime-readiness.txt ./scripts/check-integration-runtime-readiness.sh)"

paper_readiness="$(readiness_state "$paper_readiness_text")"
integration_readiness="$(readiness_state "$integration_readiness_text")"

echo "paper summary: $paper_summary"
echo "paper readiness: $paper_readiness"
echo "integration summary: $integration_summary"
echo "integration readiness: $integration_readiness"

FAILED=0
if ! check_pair "paper" "$paper_summary" "$paper_readiness"; then
  echo "paper consistency: FAIL"
  FAILED=1
fi
if ! check_pair "integration" "$integration_summary" "$integration_readiness"; then
  echo "integration consistency: FAIL"
  FAILED=1
fi

if [[ "$FAILED" == "0" ]]; then
  echo "result: PASS"
else
  echo "result: FAIL"
fi

exit "$FAILED"
