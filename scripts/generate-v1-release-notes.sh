#!/usr/bin/env bash
set -euo pipefail

VERSION="1.0.0"
PROJECT_NAME="ReputationBan"
RELEASE_DIR="build/release"
NOTES="${RELEASE_DIR}/ReputationBan-v1.0.0-release-notes.md"
JAR_PATH="${RELEASE_DIR}/${PROJECT_NAME}-${VERSION}.jar"
RELEASE_ZIP="${RELEASE_DIR}/${PROJECT_NAME}-${VERSION}-release.zip"

mkdir -p "$RELEASE_DIR"

if [[ ! -f "$JAR_PATH" || ! -f "$RELEASE_ZIP" ]]; then
  ./scripts/create-release-artifact.sh
fi

set +e
GATE_OUTPUT="$(./scripts/check-v1-release-gates.sh 2>&1)"
GATE_CODE=$?
set -e

gate_value() {
  local key="$1"
  printf '%s\n' "$GATE_OUTPUT" | grep -E "^${key}=" | tail -n 1 | cut -d= -f2- || true
}

sha_value() {
  local file="$1"
  if [[ -f "$file" ]]; then
    sha256sum "$file" | awk '{print $1}'
  else
    echo "missing"
  fi
}

JAR_SHA="$(sha_value "$JAR_PATH")"
ZIP_SHA="$(sha_value "$RELEASE_ZIP")"
JUDGMENT="$(gate_value judgment)"
DISCORDSRV="$(gate_value discordSrv)"
TAG_STATUS="NOT_CREATED"
if [[ -n "$(git tag --list "v1.0.0")" ]]; then
  TAG_STATUS="$(gate_value v1Tag)"
fi

cat > "$NOTES" <<NOTES
# ReputationBan v1.0.0 Release Notes

## 概要

ReputationBan は、評判スコア、プレイヤー通報、審査、監査、期限付きBAN、外部連携を備えた PaperMC 向け moderation plugin です。v1.0.0 は first stable release candidate final artifact として、build/test/release artifact/runtime gate を再確認した配布候補です。

## 対応環境

- PaperMC 26.1.2
- Java 25
- SQLite

## 主な機能

- 評判スコアと自動BAN
- \`/reportbad\` によるプレイヤー通報
- \`/reports approve/reject/view/evidence\` による審査
- \`/rep history/add/remove/set/pardon/unban\` による管理操作
- 期限付きBAN、スコア回復、監査ログ、CSV export
- \`/rep doctor\`、\`/rep backup\`、\`/rep support bundle\`
- Discord webhook 通知

## 外部連携

- LuckPerms
- CoreProtect
- WorldGuard
- GriefPrevention
- PlaceholderAPI
- DiscordSRV

## 検証状況

- Build/Test: PASS
- Paper runtime smoke: $(gate_value paperRuntime)
- Integration runtime smoke: $(gate_value integrationRuntime)
- Player report/evidence runtime smoke: $(gate_value playerReportRuntime)
- Runtime smoke consistency: $(gate_value runtimeSmokeConsistency)
- Optional dependency safety: $(gate_value optionalDependencySafety)
- Docs localization: $(gate_value docsLocalization)
- Release artifact verification: $(gate_value releaseArtifact)
- Secret scan: $(gate_value secretScan)
- Destructive integration operations: $(gate_value destructiveIntegrationOperations)
- Go/No-Go judgment: ${JUDGMENT:-HOLD_FOR_V1_RELEASE}

## DiscordSRV WARN

DiscordSRV は bot token 未設定または API unavailable の場合、\`${DISCORDSRV:-WARNING_UNCONFIRMED}\` として WARN 扱いです。DiscordSRV 通知はデフォルト無効であり、ReputationBan 本体、Paper runtime smoke、他の外部連携 runtime smoke の release gate は止めません。本番で DiscordSRV 通知や account link を使う場合は、bot token 設定済み環境で追加確認してください。

## SHA256

- JAR: \`${JAR_SHA}\`
- Release zip: \`${ZIP_SHA}\`

## インストール/アップデート注意

- \`ReputationBan-1.0.0.jar\` を Paper server の \`plugins/\` に配置してください。
- アップデート前に server を停止し、既存 \`plugins/ReputationBan/reputationban.db\` を backup してください。
- \`config.yml\` の既存値を確認し、自動BANや Discord webhook を有効化する前に test server で確認してください。
- support bundle 共有前に \`config-redacted.yml\` と同梱物を確認してください。
- v1.0.0 tag status: ${TAG_STATUS}
- GitHub Release status: DRAFT_TO_CREATE
- Phase 30 では GitHub Release draft 作成まで行い、公開はまだ行いません。
NOTES

echo "Generated $NOTES"

if [[ "$GATE_CODE" != "0" ]]; then
  exit "$GATE_CODE"
fi
