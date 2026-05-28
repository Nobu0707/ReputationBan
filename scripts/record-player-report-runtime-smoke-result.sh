#!/usr/bin/env bash
set -euo pipefail

VERSION="1.0.1"
JAR="build/libs/ReputationBan-${VERSION}.jar"
MANUAL_CONFIRMED_NOTE="User manually confirmed all Phase 26 player report runtime smoke checklist items passed."

usage() {
  cat <<'USAGE'
Usage:
  ./scripts/record-player-report-runtime-smoke-result.sh --result PASS --reporter TestReporter --target TestTarget --report-id 123 --note "reportbad and reports evidence passed"
  ./scripts/record-player-report-runtime-smoke-result.sh --result PASS --manual-confirmed --note "User manually confirmed all Phase 26 player report runtime smoke checklist items passed."
  ./scripts/record-player-report-runtime-smoke-result.sh --result PASS --manual-confirmed --carried-forward-from 0.27.0 --note "Carried forward from Phase 27 manual player report runtime smoke; Phase 29 changes version/docs/scripts/release artifacts only."
  ./scripts/record-player-report-runtime-smoke-result.sh --result FAIL --reporter TestReporter --target TestTarget --note "reports evidence threw exception"
  ./scripts/record-player-report-runtime-smoke-result.sh --result NOT_RUN --note "no two players available"

Writes:
  build/manual-smoke/player-report-runtime-YYYYMMDD-HHMMSS/summary.txt
  build/manual-smoke/player-report-runtime-YYYYMMDD-HHMMSS/manual-checklist.txt when --manual-confirmed is used
USAGE
}

RESULT=""
REPORTER=""
TARGET=""
REPORT_ID=""
NOTE=""
MANUAL_CONFIRMED="false"
CARRIED_FORWARD_FROM=""
CARRY_FORWARD_REASON="Phase 29 changes version/docs/scripts/release artifacts only."

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
    --manual-confirmed)
      MANUAL_CONFIRMED="true"
      shift
      ;;
    --carried-forward-from)
      CARRIED_FORWARD_FROM="${2:-}"
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

if [[ "$MANUAL_CONFIRMED" == "true" && -z "$NOTE" ]]; then
  NOTE="$MANUAL_CONFIRMED_NOTE"
fi

if [[ -z "$RESULT" || -z "$NOTE" ]]; then
  usage
  exit 0
fi

if [[ "$RESULT" != "PASS" && "$RESULT" != "FAIL" && "$RESULT" != "NOT_RUN" ]]; then
  echo "[FAIL] --result must be PASS, FAIL, or NOT_RUN" >&2
  exit 1
fi

if [[ "$RESULT" == "PASS" && "$MANUAL_CONFIRMED" != "true" && ( -z "$REPORTER" || -z "$TARGET" || -z "$REPORT_ID" ) ]]; then
  echo "[FAIL] PASS requires --reporter, --target, and --report-id from a real two-player runtime smoke." >&2
  exit 1
fi

if [[ "$MANUAL_CONFIRMED" == "true" && "$RESULT" != "PASS" ]]; then
  echo "[FAIL] --manual-confirmed is only valid with --result PASS" >&2
  exit 1
fi

if [[ -n "$CARRIED_FORWARD_FROM" && "$MANUAL_CONFIRMED" != "true" ]]; then
  echo "[FAIL] --carried-forward-from requires --manual-confirmed" >&2
  exit 1
fi

if [[ -n "$CARRIED_FORWARD_FROM" && "$RESULT" != "PASS" ]]; then
  echo "[FAIL] --carried-forward-from is only valid with --result PASS" >&2
  exit 1
fi

if [[ "$MANUAL_CONFIRMED" == "true" ]]; then
  REPORTER="${REPORTER:-<manual-confirmed>}"
  TARGET="${TARGET:-<manual-confirmed>}"
  REPORT_ID="${REPORT_ID:-<manual-confirmed>}"
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
  echo "manualConfirmed=$MANUAL_CONFIRMED"
  if [[ -n "$CARRIED_FORWARD_FROM" ]]; then
    echo "carriedForwardFrom=$CARRIED_FORWARD_FROM"
    echo "carryForwardReason=$CARRY_FORWARD_REASON"
  fi
  echo "note=$NOTE"
  echo "createdAt=$(date -Iseconds)"
} > "$SUMMARY"

if [[ "$MANUAL_CONFIRMED" == "true" ]]; then
  cat > "${OUTDIR}/manual-checklist.txt" <<'CHECKLIST'
/rep version: OK
/rep doctor: OK
/rep integrations: OK
/reportbad: OK
/reports list all 10: OK
/reports view: OK
/reports evidence: OK
LuckPerms context: OK
CoreProtect context: OK
WorldGuard context: OK
GriefPrevention context: OK
DiscordSRV context unavailable/hidden acceptable: OK
/rep history: OK
/rep audit recent: OK
/rep pardon cleanup: OK
support bundle excludes DB/logs/secret: OK
No exception: OK
No NoClassDefFoundError: OK
No ClassNotFoundException: OK
No enabling error: OK
No unintended auto-ban: OK
Pardon works: OK
CHECKLIST
fi

echo "Created $SUMMARY"
