# Phase 13 / v0.13.0

Phase 13 は、v1.0.0 release candidate へ進む前の最終 hardening pass です。

## 主な変更

- project と plugin metadata を `0.13.0` に更新しました。
- webhook、URL、password、token、secret、session/sessionId、cookie-like values の free-text redaction を強化しました。
- support bundle の `meta.txt` と `doctor.txt` で `<plugin-data-folder>` を使い、absolute path exposure を減らしました。
- forbidden entry names、Webhook URLs、URLs、absolute paths に対する support bundle safety checks を追加しました。
- `scripts/verify-release-artifact.sh` を追加しました。
- JAR と release ZIP の両方に SHA256 files を作成するよう `scripts/create-release-artifact.sh` を更新しました。
- `docs/SECURITY_REDACTION.md` を追加しました。
- `docs/PAPER_RUNTIME_SMOKE_REPORT_TEMPLATE.md` を追加しました。
- `scripts/record-paper-runtime-smoke-result.sh` を追加しました。
- review archive に release verification output と最新の Paper runtime smoke summary を含めるよう更新しました。

## Deferred

GUI、LuckPerms integration、CoreProtect integration、WorldGuard や他 protection integrations、trust scoring、contact detection、full appeal workflows、Folia support は後続に残しています。

## Validation

- `./gradlew clean test build --warning-mode all`
- `./scripts/review_code.sh`
- `./scripts/run-local-smoke-check.sh`
- `./scripts/create-release-artifact.sh`
- `./scripts/verify-release-artifact.sh`
- `./scripts/make-review-archive.sh "Phase 13"`
