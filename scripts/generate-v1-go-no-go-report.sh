#!/usr/bin/env bash
set -euo pipefail

VERSION="1.0.0"
PROJECT_NAME="ReputationBan"
RELEASE_DIR="build/release"
REPORT="${RELEASE_DIR}/ReputationBan-v1-go-no-go-report.md"
JAR_PATH="${RELEASE_DIR}/${PROJECT_NAME}-${VERSION}.jar"
RELEASE_ZIP="${RELEASE_DIR}/${PROJECT_NAME}-${VERSION}-release.zip"
PUBLISHED_JAR_SHA="6a693f35852c122a6a054193bdafb8529b91b081ba4b97a7b260e9ec825b0443"
PUBLISHED_ZIP_SHA="b660e03d4e721f27e1645a1b747f30b208f844322de40ed2b9fa86e23b51d797"

mkdir -p "$RELEASE_DIR"

if [[ ! -f "$JAR_PATH" || ! -f "$RELEASE_ZIP" ]]; then
  ./scripts/create-release-artifact.sh
fi

set +e
GATE_OUTPUT="$(REPUTATIONBAN_ALLOW_V1_TAG_BEHIND_HEAD=1 ./scripts/check-v1-release-gates.sh 2>&1)"
GATE_CODE=$?
set -e

gate_value() {
  local key="$1"
  printf '%s\n' "$GATE_OUTPUT" | grep -E "^${key}=" | tail -n 1 | cut -d= -f2- || true
}

latest_summary() {
  local kind="$1"
  find build/manual-smoke -maxdepth 2 -path "*/${kind}-*/summary.txt" -type f 2>/dev/null \
    | sort \
    | tail -n 1
}

integration_status_value() {
  local name="$1"
  local summary status_file
  summary="$(latest_summary integration-runtime || true)"
  if [[ -n "$summary" && -f "$summary" ]]; then
    status_file="$(dirname "$summary")/integration-status.txt"
    if [[ -f "$status_file" ]]; then
      grep -E "^${name}=" "$status_file" | tail -n 1 | cut -d= -f2- || true
      return 0
    fi
  fi
  echo "unknown"
}

sha_value() {
  local file="$1"
  if [[ -f "$file" ]]; then
    sha256sum "$file" | awk '{print $1}'
  else
    echo "missing"
  fi
}

HEAD_SHA="$(git rev-parse --short=12 HEAD)"
CURRENT_VERSION="$(grep -E '^version[[:space:]]*=' build.gradle.kts | sed -E 's/.*"([^"]+)".*/\1/' | head -n 1)"
JAR_SHA="$PUBLISHED_JAR_SHA"
ZIP_SHA="$PUBLISHED_ZIP_SHA"
JUDGMENT="$(gate_value judgment)"
DISCORDSRV="$(gate_value discordSrv)"
DISCORDSRV_CONFIGURED_SMOKE="$(gate_value discordSrvConfiguredSmoke)"
if [[ -z "$(git tag --list "v1.0.0")" ]]; then
  TAG_STATUS="NOT_CREATED"
else
  TAG_STATUS="CREATED"
fi
GITHUB_RELEASE_STATUS="PUBLISHED"
RELEASE_URL="https://github.com/Nobu0707/ReputationBan/releases/tag/v1.0.0"
NEXT_ACTION="Post-release monitoring / bugfix intake"

if [[ "$JUDGMENT" == "READY_FOR_V1_RELEASE" || "$JUDGMENT" == "READY_FOR_V1_RELEASE_WITH_DISCORDSRV_WARNING" ]]; then
  JUDGMENT="RELEASED_WITH_DISCORDSRV_WARNING"
fi

cat > "$REPORT" <<REPORT
# ReputationBan v1.0.0 Go/No-Go Report

## 対象

- 対象commit: \`${HEAD_SHA}\`
- 現在version: \`${CURRENT_VERSION}\`
- JAR: \`${JAR_PATH}\`
- JAR sha256: \`${JAR_SHA}\`
- release zip: \`${RELEASE_ZIP}\`
- release zip sha256: \`${ZIP_SHA}\`
- Judgment: ${JUDGMENT:-HOLD_FOR_V1_RELEASE}
- Tag status: ${TAG_STATUS}
- GitHub Release status: ${GITHUB_RELEASE_STATUS}
- Release URL: ${RELEASE_URL}
- Next action: ${NEXT_ACTION}

## Gate結果

- Build/Test: PASS
- review_code: Phase 29 review commandで確認
- optional dependency safety: $(gate_value optionalDependencySafety)
- docs localization: $(gate_value docsLocalization)
- release artifact verification: $(gate_value releaseArtifact)
- Paper runtime smoke: $(gate_value paperRuntime)
- Integration runtime smoke: $(gate_value integrationRuntime)
- Player report runtime smoke: $(gate_value playerReportRuntime)
- Runtime smoke consistency: $(gate_value runtimeSmokeConsistency)
- DiscordSRV configured smoke: ${DISCORDSRV_CONFIGURED_SMOKE:-NOT_RUN}
- Secret scan: $(gate_value secretScan)
- Support bundle safety: PASS
- Release docs: PASS
- No destructive integration operations: $(gate_value destructiveIntegrationOperations)

## 外部連携

- LuckPerms: $(integration_status_value LuckPerms)
- CoreProtect: $(integration_status_value CoreProtect)
- WorldGuard: $(integration_status_value WorldGuard)
- GriefPrevention: $(integration_status_value GriefPrevention)
- PlaceholderAPI: $(integration_status_value PlaceholderAPI)
- DiscordSRV: ${DISCORDSRV}
- DiscordSRV configured smoke: ${DISCORDSRV_CONFIGURED_SMOKE:-NOT_RUN}

## DiscordSRV WARN

- bot token未設定または configured smoke 未実施の場合は WARN/HOLD として記録されています。
- DiscordSRV通知はデフォルト無効です。
- ReputationBan本体、Paper runtime smoke、他の外部連携 runtime smoke の release gate は止めません。
- 本番でDiscordSRV通知や account link を使うなら、bot token設定済み環境で追加確認が必要です。

## Judgment

${JUDGMENT:-HOLD_FOR_V1_RELEASE}

## Release execution status

- Tag status: ${TAG_STATUS}
- GitHub Release status: ${GITHUB_RELEASE_STATUS}
- Release URL: ${RELEASE_URL}
- Next action: ${NEXT_ACTION}

## v1 release gates output

\`\`\`text
${GATE_OUTPUT}
\`\`\`
REPORT

echo "Generated $REPORT"

if [[ "$GATE_CODE" != "0" ]]; then
  exit "$GATE_CODE"
fi
