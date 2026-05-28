#!/usr/bin/env bash
set -euo pipefail

VERSION="0.28.0"
PROJECT_NAME="ReputationBan"
RELEASE_DIR="build/release"
DRAFT="${RELEASE_DIR}/ReputationBan-v1.0.0-release-notes-draft.md"
JAR_PATH="${RELEASE_DIR}/${PROJECT_NAME}-${VERSION}.jar"
RELEASE_ZIP="${RELEASE_DIR}/${PROJECT_NAME}-${VERSION}-release.zip"

mkdir -p "$RELEASE_DIR"

if [[ ! -f "$JAR_PATH" || ! -f "$RELEASE_ZIP" ]]; then
  ./scripts/create-release-artifact.sh
fi

./scripts/check-v1-release-gates.sh >/dev/null

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

cat > "$DRAFT" <<DRAFT
# ReputationBan v1.0.0 Release Notes Draft

## 概要

評判スコア制BAN・通報・審査・監査・外部連携プラグインです。

## 対応環境

- PaperMC 26.1.2
- Java 25

## 主な機能

- 評判スコア
- /reportbad
- /reports approve/reject/view/evidence
- /rep history/add/remove/set/pardon/unban
- 期限付きBAN
- スコア回復
- 監査ログ
- CSV export
- support bundle
- backup
- Discord webhook通知
- LuckPerms連携
- CoreProtect連携
- WorldGuard連携
- GriefPrevention連携
- PlaceholderAPI連携
- DiscordSRV連携

## 重要な注意

- DiscordSRVはbot token設定済み環境で追加確認が必要です。
- 自動BAN運用前にconfig調整を推奨します。
- support bundle共有前に \`config-redacted.yml\` と同梱物を確認してください。
- Webhook URLはシークレットです。チケット、ログ、スクリーンショット、review archive に貼らないでください。

## 検証状況

- Build/Test PASS
- Paper runtime smoke PASS
- Integration runtime smoke PASS
- Player report runtime smoke PASS
- Runtime smoke consistency PASS
- Optional dependency safety PASS
- Docs localization PASS
- Release artifact verification PASS
- Secret scan PASS

## SHA256

- JAR: \`${JAR_SHA}\`
- Release zip: \`${ZIP_SHA}\`

## Draft Note

Phase 28では \`v1.0.0\` tag 作成と GitHub Release 公開はまだ行っていません。この下書きは、Phase 28レビュー後にユーザー承認を得てから次Phaseで確定します。
DRAFT

echo "Generated $DRAFT"
