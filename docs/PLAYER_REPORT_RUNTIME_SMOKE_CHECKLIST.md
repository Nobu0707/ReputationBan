# Player Report Runtime Smoke Checklist

Phase 26 の player report/evidence runtime smoke は、実プレイヤー操作が必要な `/reportbad`、`/reports view`、`/reports evidence`、`report_context` 表示を確認する手動ゲートです。Phase 27 では、ユーザーが全項目 OK と報告済みの手動確認結果を `manualConfirmed=true` として正式記録しました。Phase 29 では Java runtime behavior、reportbad、reports evidence、report_context 生成、DB schema を変更しないため、この PASS 状態を `carriedForwardFrom=0.27.0` として carry-forward 記録します。実プレイヤー2名以上で確認していない場合は PASS にせず、`NOT_RUN` / `HOLD_FOR_PLAYER_REPORT_RUNTIME_SMOKE` として記録します。

## 前提

- Paper runtime smoke が PASS 済みです。
- Integration runtime smoke が PASS 済みです。
- テスト用プレイヤー2名以上を使います。
- reporter と target を分けます。
- 本番データではなく disposable なテスト環境で行います。
- BAN やスコア減点が起きるため、必要なら事前 backup を取ります。
- CoreProtect / WorldGuard / GriefPrevention の context はテスト場所や事前ログに依存します。何もない場所では結果が空でも正常です。ただし `/reports evidence` がエラーなく表示できることは必須です。

## 手順

1. reporter と target がログインします。
2. `/rep version`
3. `/rep doctor`
4. `/rep integrations`
5. reporter で `/reportbad <target> griefing runtime-smoke-test`
6. `/reports list all 10`
7. `/reports view <id>`
8. `/reports evidence <id>`
9. LuckPerms context が表示されるか確認します。
10. CoreProtect context が表示されるか確認します。
11. WorldGuard context が表示されるか確認します。
12. GriefPrevention context が表示されるか確認します。
13. DiscordSRV context は未設定なら unavailable または hidden で問題ありません。
14. `/rep history <target> 10`
15. `/rep audit recent 20`
16. 必要に応じて `/rep pardon <target> runtime-smoke-cleanup`
17. support bundle に DB / logs / secret が含まれないことを確認します。

## PASS 条件

- `/reportbad` が成功します。
- `/reports view <id>` が表示されます。
- `/reports evidence <id>` が表示されます。
- ReputationBan 本体に Exception が出ません。
- `report_context` が少なくとも1 provider分表示される、または「連携情報なし」が明示されます。
- 外部連携未導入または未設定があっても ReputationBan 本体が落ちません。

## FAIL 条件

- `/reportbad` で例外が出ます。
- `/reports evidence` で例外が出ます。
- `NoClassDefFoundError`
- `ClassNotFoundException`
- `Error occurred while enabling ReputationBan`
- 不正な自動 BAN
- pardon 不能

## 記録

PASS は実プレイヤー2名以上で確認した場合だけ記録します。

```bash
./scripts/record-player-report-runtime-smoke-result.sh \
  --result PASS \
  --reporter TestReporter \
  --target TestTarget \
  --report-id 123 \
  --note "reportbad and reports evidence passed"
```

公開用レビューに reporter、target、report id を残したくない場合は、ユーザー手動確認済みの結果として次のように記録します。

```bash
./scripts/record-player-report-runtime-smoke-result.sh \
  --result PASS \
  --manual-confirmed \
  --carried-forward-from 0.27.0 \
  --note "Carried forward from Phase 27 manual player report runtime smoke; Phase 29 changes version/docs/scripts/release artifacts only."
```

この形式では `reporter=<manual-confirmed>`、`target=<manual-confirmed>`、`reportId=<manual-confirmed>`、`manualConfirmed=true`、`carriedForwardFrom=0.27.0`、`carryForwardReason=Phase 29 changes version/docs/scripts/release artifacts only.` を summary に残し、同じディレクトリに `manual-checklist.txt` を作成します。

FAIL:

```bash
./scripts/record-player-report-runtime-smoke-result.sh \
  --result FAIL \
  --reporter TestReporter \
  --target TestTarget \
  --note "reports evidence threw exception"
```

NOT_RUN:

```bash
./scripts/record-player-report-runtime-smoke-result.sh --result NOT_RUN --note "no two players available"
```

readiness:

```bash
./scripts/check-player-report-runtime-readiness.sh
./scripts/check-player-report-runtime-readiness.sh --strict
```

通常モードでは未実施でも exit 0 とし、`player report runtime smoke: NOT_RUN` と `judgment: HOLD_FOR_PLAYER_REPORT_RUNTIME_SMOKE` を表示します。`--strict` は PASS 以外を non-zero にします。
