# Phase 23 / v0.23.0

Phase 23 は、新機能追加ではなく、v1.0.0 前の重要 gate である Paper runtime smoke を自動化し、記録しやすくするフェーズです。

## 目的

- version を 0.23.0 に更新します。
- `~/servers/paper-26.1.2/start.sh` が `screen` で PaperMC 26.1.2 を起動する環境を前提に、Paper runtime smoke を自動実行できるようにします。
- 実 Paper server を起動していない場合、または screen session / console command 投入を確認できない場合は PASS を記録しません。
- 環境がない場合は `NOT_RUN` として `build/manual-smoke/paper-runtime-*` に記録します。

## Paper runtime smoke automation

`scripts/run-paper-runtime-smoke.sh` は次を行います。

- `build/libs/ReputationBan-0.23.0.jar` の存在確認
- `REPUTATIONBAN_PAPER_DIR` の既定値 `~/servers/paper-26.1.2` と `REPUTATIONBAN_PAPER_START_SCRIPT` の既定値 `~/servers/paper-26.1.2/start.sh` の確認
- `screen -ls` の起動前後記録
- 既存 `ReputationBan*.jar` の `plugins/backups/reputationban-smoke-*` 退避
- JAR の `plugins/` 配置
- `start.sh` 実行
- `REPUTATIONBAN_SCREEN_NAME`、screen 差分、`paper` / `minecraft` / `mc` / `server` 候補の順で session 特定
- `screen -S <session> -p 0 -X stuff "<command>\r"` による console command 投入
- `version`、`plugins`、`rep version`、`rep doctor`、`rep integrations`、`rep integrations test`、`rep placeholders`、`rep audit recent 5`、`rep maintenance preview` の確認
- smoke が新規起動した server のみ `stop` 投入。既存 session は `REPUTATIONBAN_SMOKE_STOP_SERVER=1` の場合だけ停止

## Readiness gate

`scripts/check-paper-runtime-readiness.sh` は最新の `build/manual-smoke/paper-runtime-*/summary.txt` を読みます。

- 通常モードは `NOT_RUN` でも exit 0 とし、`judgment: HOLD_FOR_PAPER_RUNTIME_SMOKE` を表示します。
- `--strict` は `result=PASS` または `status=PASS` 以外で non-zero になります。
- 未実施は PASS ではありません。

## v0.23.0 検証結果

- build / JUnit / review / release artifact verification は Phase 23 実装後に実行します。
- Paper runtime smoke は `scripts/run-paper-runtime-smoke.sh` の実行結果を `build/manual-smoke/paper-runtime-*` に記録します。
- 実機環境がない場合は `status=NOT_RUN` / `result=NOT_RUN` として残します。
- Integration runtime smoke は既存の `scripts/check-integration-runtime-readiness.sh` と `docs/INTEGRATION_RUNTIME_SMOKE_CHECKLIST.md` の gate を維持します。

## 実装しないもの

- 新しい外部プラグイン連携
- Discord から Minecraft コマンドを実行する機能
- Discord role 変更
- GUI
- Folia 対応
- v1.0.0 リリース化
