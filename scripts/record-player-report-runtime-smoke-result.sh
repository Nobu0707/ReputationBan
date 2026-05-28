#!/usr/bin/env bash
set -euo pipefail

VERSION="0.26.0"
JAR="build/libs/ReputationBan-${VERSION}.jar"

usage() {
  cat <<'USAGE'
Usage:
  ./scripts/record-player-report-runtime-smoke-result.sh --result PASS --reporter TestReporter --target TestTarget --report-id 123 --note "reportbad and reports evidence passed"
  ./scripts/record-player-report-runtime-smoke-result.sh --result FAIL --reporter TestReporter --target TestTarget --note "reports evidence threw exception"
  ./scripts/record-player-report-runtime-smoke-result.sh --result NOT_RUN --note "no two players available"

Writes:
  build/manual-smoke/player-report-runtime-YYYYMMDD-HHMMSS/summary.txt
USAGE
}

RESULT=""
REPORTER=""
TARGET=""
REPORT_ID=""
NOTE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --result)
      RESULT="${2:-}"
      shift 2
      ;;
    --reporter)
      REPORTER="${2:-}"
      shift 2
      ;;
    --target)
      TARGET="${2:-}"
      shift 2
      ;;
    --report-id)
      REPORT_ID="${2:-}"
      shift 2
      ;;
    --note)
      NOTE="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      usage
      exit 0
      ;;
  esac
done

if [[ -z "$RESULT" || -z "$NOTE" ]]; then
  usage
  exit 0
fi

if [[ "$RESULT" != "PASS" && "$RESULT" != "FAIL" && "$RESULT" != "NOT_RUN" ]]; then
  echo "[FAIL] --result must be PASS, FAIL, or NOT_RUN" >&2
  exit 1
fi

if [[ "$RESULT" == "PASS" && ( -z "$REPORTER" || -z "$TARGET" || -z "$REPORT_ID" ) ]]; then
  echo "[FAIL] PASS requires --reporter, --target, and --report-id from a real two-player runtime smoke." >&2
  exit 1
fi

STAMP="$(date +%Y%m%d-%H%M%S)"
OUTDIR="build/manual-smoke/player-report-runtime-${STAMP}"
SUMMARY="${OUTDIR}/summary.txt"
mkdir -p "$OUTDIR"

if [[ -f "$JAR" ]]; then
  JAR_SHA="$(sha256sum "$JAR" | awk '{print $1}')"
else
  JAR_SHA="missing"
fi

{
  echo "status=$RESULT"
  echo "result=$RESULT"
  echo "version=$VERSION"
  echo "jar=$JAR"
  echo "jarSha256=$JAR_SHA"
  echo "reporter=$REPORTER"
  echo "target=$TARGET"
  echo "reportId=$REPORT_ID"
  echo "note=$NOTE"
  echo "createdAt=$(date -Iseconds)"
} > "$SUMMARY"

echo "Created $SUMMARY"
