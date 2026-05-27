#!/usr/bin/env bash
set -euo pipefail

VERSION="0.16.0"
JAR="build/libs/ReputationBan-${VERSION}.jar"

usage() {
  cat <<'USAGE'
Usage:
  ./scripts/record-paper-runtime-smoke-result.sh --result PASS --note "local Paper smoke passed"
  ./scripts/record-paper-runtime-smoke-result.sh --result FAIL --note "doctor failed"
USAGE
}

RESULT=""
NOTE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --result)
      RESULT="${2:-}"
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

if [[ "$RESULT" != "PASS" && "$RESULT" != "FAIL" ]]; then
  echo "[FAIL] --result must be PASS or FAIL" >&2
  exit 1
fi

STAMP="$(date +%Y%m%d-%H%M%S)"
OUTDIR="build/manual-smoke/paper-runtime-${STAMP}"
SUMMARY="${OUTDIR}/summary.txt"
mkdir -p "$OUTDIR"

if [[ -f "$JAR" ]]; then
  JAR_SHA="$(sha256sum "$JAR" | awk '{print $1}')"
else
  JAR_SHA="missing"
fi

{
  echo "result=$RESULT"
  echo "note=$NOTE"
  echo "version=$VERSION"
  echo "jar=$JAR"
  echo "jarSha256=$JAR_SHA"
  echo "createdAt=$(date -Iseconds)"
} > "$SUMMARY"

echo "Created $SUMMARY"
