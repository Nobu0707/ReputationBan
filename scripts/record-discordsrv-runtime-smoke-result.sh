#!/usr/bin/env bash
set -euo pipefail

VERSION="1.0.0"
PROJECT_NAME="ReputationBan"
JAR="build/libs/${PROJECT_NAME}-${VERSION}.jar"

usage() {
  cat <<'USAGE'
Usage:
  ./scripts/record-discordsrv-runtime-smoke-result.sh --result PASS --scenario "DiscordSRV token configured" --note "DiscordSRV active=true, account link context and notifications verified."
  ./scripts/record-discordsrv-runtime-smoke-result.sh --result FAIL --scenario "DiscordSRV token configured" --note "DiscordSRV notification failed."
  ./scripts/record-discordsrv-runtime-smoke-result.sh --result NOT_RUN --scenario "DiscordSRV token not configured" --note "Bot token is not configured in this environment."

Writes:
  build/manual-smoke/discordsrv-runtime-YYYYMMDD-HHMMSS/summary.txt

Secret safety:
  Concrete Discord webhook URLs and token/password/secret/session/cookie assignments in --note are replaced with <redacted>.
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

case "$RESULT" in
  PASS|FAIL|NOT_RUN)
    ;;
  *)
    echo "Invalid --result: $RESULT" >&2
    echo "Expected PASS, FAIL, or NOT_RUN." >&2
    exit 1
    ;;
esac

contains_secret_like_note() {
  local value="$1"
  if printf '%s\n' "$value" | grep -Eiq 'https://(canary\.|ptb\.)?discord(app)?\.com/api/webhooks/[0-9]+/[A-Za-z0-9_-]+'; then
    return 0
  fi
  if printf '%s\n' "$value" | grep -Eiq '(webhook[-_ ]?url|webhookUrl|bot[-_ ]?token|token|secret|password|session|cookie)[[:space:]]*[:=][[:space:]]*[^[:space:]]+'; then
    return 0
  fi
  if printf '%s\n' "$value" | grep -Eq 'mfa\.[A-Za-z0-9_-]{20,}|[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{6,}\.[A-Za-z0-9_-]{20,}'; then
    return 0
  fi
  return 1
}

SAFE_NOTE="$NOTE"
if contains_secret_like_note "$NOTE"; then
  echo "warning: --note contained secret-like data and was redacted." >&2
  SAFE_NOTE="<redacted>"
fi

STAMP="$(date +%Y%m%d-%H%M%S)"
OUTDIR="build/manual-smoke/discordsrv-runtime-${STAMP}"
SUMMARY="${OUTDIR}/summary.txt"
mkdir -p "$OUTDIR"

if [[ -f "$JAR" ]]; then
  JAR_SHA="$(sha256sum "$JAR" | awk '{print $1}')"
else
  JAR_SHA="MISSING"
fi

{
  echo "status=$RESULT"
  echo "result=$RESULT"
  echo "scenario=$SCENARIO"
  echo "note=$SAFE_NOTE"
  echo "version=$VERSION"
  echo "jar=$JAR"
  echo "jarSha256=$JAR_SHA"
  echo "createdAt=$(date -Iseconds)"
} > "$SUMMARY"

echo "Recorded DiscordSRV runtime smoke result:"
echo "  $SUMMARY"
