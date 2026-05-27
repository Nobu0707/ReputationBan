#!/usr/bin/env bash
set -euo pipefail

VERSION="0.24.0"
PROJECT_NAME="ReputationBan"
JAR="build/libs/${PROJECT_NAME}-${VERSION}.jar"

usage() {
  cat <<'USAGE'
Usage:
  ./scripts/record-integration-runtime-smoke-result.sh --result PASS --scenario "WorldGuard" --note "manual smoke passed"
  ./scripts/record-integration-runtime-smoke-result.sh --result PASS --scenario "GriefPrevention" --note "manual smoke passed"
  ./scripts/record-integration-runtime-smoke-result.sh --result PASS --scenario "PlaceholderAPI" --note "manual smoke passed"
  ./scripts/record-integration-runtime-smoke-result.sh --result PASS --scenario "DiscordSRV" --note "manual smoke passed"
  ./scripts/record-integration-runtime-smoke-result.sh --result PASS --scenario "All integrations" --note "manual smoke passed"

Writes:
  build/manual-smoke/integration-runtime-YYYYMMDD-HHMMSS/summary.txt
USAGE
}

RESULT=""
SCENARIO=""
NOTE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --result)
      RESULT="${2:-}"
      shift 2
      ;;
    --scenario)
      SCENARIO="${2:-}"
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
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$RESULT" || -z "$SCENARIO" ]]; then
  usage
  exit 0
fi

STAMP="$(date +%Y%m%d-%H%M%S)"
OUTDIR="build/manual-smoke/integration-runtime-${STAMP}"
SUMMARY="${OUTDIR}/summary.txt"
mkdir -p "$OUTDIR"

if [[ -f "$JAR" ]]; then
  JAR_SHA="$(sha256sum "$JAR" | awk '{print $1}')"
else
  JAR_SHA="MISSING"
fi

{
  echo "result=$RESULT"
  echo "scenario=$SCENARIO"
  echo "note=$NOTE"
  echo "version=$VERSION"
  echo "jar=$JAR"
  echo "jarSha256=$JAR_SHA"
  echo "createdAt=$(date -Iseconds)"
} > "$SUMMARY"

echo "Recorded integration runtime smoke result:"
echo "  $SUMMARY"
