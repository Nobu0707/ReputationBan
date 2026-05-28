# Phase 26 / v0.26.0

Phase 26 は player report/evidence runtime smoke gate フェーズです。新しい外部プラグイン連携や GUI は追加せず、実プレイヤー操作が必要な `/reportbad`、`/reports view`、`/reports evidence`、外部連携 `report_context` 表示を記録・判定しやすくしました。

## 目的

- version を 0.26.0 に更新します。
- `docs/PLAYER_REPORT_RUNTIME_SMOKE_CHECKLIST.md` を追加します。
- `scripts/record-player-report-runtime-smoke-result.sh` を追加します。
- `scripts/check-player-report-runtime-readiness.sh` を追加します。
- `scripts/check-runtime-smoke-consistency.sh` が Paper / Integration / Player report runtime smoke の整合性を確認します。
- `scripts/make-review-archive.sh` が player report readiness と latest summary を収集します。

## Player report/evidence runtime smoke

`/reportbad` はプレイヤー実行が前提です。console だけの自動 runtime smoke では `report_context` の実生成を完全確認できないため、実プレイヤー2名以上の reporter / target で確認します。

未実施の場合は `NOT_RUN` とし、`HOLD_FOR_PLAYER_REPORT_RUNTIME_SMOKE` を表示します。実プレイヤー2名以上で `/reportbad`、`/reports view <id>`、`/reports evidence <id>` を確認していない場合、PASS summary は作りません。

## PASS を偽装しない方針

- `make-review-archive.sh` は `/reportbad` を自動実行しません。
- `make-review-archive.sh` は player report smoke summary がない場合、NOT_RUN summary 相当を収集します。
- `record-player-report-runtime-smoke-result.sh --result PASS` は reporter、target、report id を必須にします。
- `check-player-report-runtime-readiness.sh --strict` は PASS 以外で non-zero になります。

## v0.26.0 の検証結果

- Paper runtime smoke: 既存 summary に基づき readiness を判定します。
- Integration runtime smoke: 既存 summary に基づき readiness を判定します。
- Player report/evidence runtime smoke: この作業では実プレイヤー2名を操作していないため PASS にはしません。未実施の場合は `NOT_RUN` / `HOLD_FOR_PLAYER_REPORT_RUNTIME_SMOKE` として記録します。
- v1.0.0 前には Paper runtime smoke PASS、Integration runtime smoke PASS、Player report/evidence runtime smoke PASS を推奨ゲートとして確認します。
