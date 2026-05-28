# Phase 27 / v0.27.0

Phase 27 は player report/evidence runtime smoke result recording / release gate close フェーズです。新しい外部連携や GUI は追加せず、Phase 26 でユーザーが手動確認済みと報告した player report/evidence runtime smoke PASS を正式な runtime smoke 記録として保存します。

## 目的

- Phase 26 手動 player report runtime smoke 結果を正式記録します。
- `scripts/record-player-report-runtime-smoke-result.sh --manual-confirmed` で、reporter、target、report id を公開せずに PASS を記録できるようにします。
- `build/manual-smoke/player-report-runtime-*` に `summary.txt` と `manual-checklist.txt` を保存します。
- Player report runtime smoke、readiness、runtime smoke consistency を PASS/READY/PASS で揃えます。
- Paper runtime smoke PASS、Integration runtime smoke PASS、Player report runtime smoke PASS が review archive に揃う状態にします。

## Manual Confirmed

`--manual-confirmed` は、公開用レビューに実プレイヤー名や report id を残したくない場合の記録用オプションです。PASS summary には次を残します。

```text
reporter=<manual-confirmed>
target=<manual-confirmed>
reportId=<manual-confirmed>
manualConfirmed=true
note=User manually confirmed all Phase 26 player report runtime smoke checklist items passed.
```

同じディレクトリの `manual-checklist.txt` には、`/rep version`、`/rep doctor`、`/rep integrations`、`/reportbad`、`/reports view`、`/reports evidence`、各 optional integration context、support bundle redaction、例外なし、意図しない auto-ban なし、pardon cleanup 成功を要約して保存します。

## Runtime Smoke Gate

- Paper runtime smoke: PASS
- Integration runtime smoke: PASS
- Player report/evidence runtime smoke: PASS
- Player report manualConfirmed: true
- Runtime smoke consistency: PASS

DiscordSRV は bot token 未設定時に WARN として扱います。ReputationBan 本体と他連携が PASS なら release gate を止めませんが、本番で DiscordSRV 通知や account link を使う場合は bot token 設定済み環境で追加確認が必要です。

## v0.27.0 の検証結果

- `./gradlew clean test build --warning-mode all`
- `./scripts/check-docs-localization.sh`
- `./scripts/check-optional-dependency-safety.sh`
- `./scripts/record-player-report-runtime-smoke-result.sh --result PASS --manual-confirmed --note "User manually confirmed all Phase 26 player report runtime smoke checklist items passed."`
- `./scripts/check-player-report-runtime-readiness.sh`
- `./scripts/check-runtime-smoke-consistency.sh`
- `./scripts/review_code.sh`
- `./scripts/run-local-smoke-check.sh`
- `./scripts/create-release-artifact.sh`
- `./scripts/verify-release-artifact.sh`
- `./scripts/make-review-archive.sh "Phase 27"`

