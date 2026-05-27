# Changelog

## 0.18.0

- WorldGuard / WorldEdit を `softdepend` と `compileOnly` の任意連携として追加しました。
- WorldGuard 連携は reflection adapter に閉じ込め、`com.sk89q.*` の直接 import を避けました。
- `/rep integrations`、`/rep integrations test`、`/rep doctor` に WorldGuard 状態を追加しました。
- `/reportbad` 後に対象カテゴリの WorldGuard region context を `report_context` provider `worldguard` として保存するようにしました。
- `/reports view <id>` と `/reports evidence <id>` で WorldGuard 文脈を表示できるようにしました。
- `WORLDGUARD_CONTEXT_CAPTURED` audit event を追加しました。
- WorldGuard region/flag の作成、変更、削除、自動処罰の根拠化は行いません。

## 0.17.0

- `/reports evidence <id>` を追加し、report に保存された LuckPerms / CoreProtect の `report_context` を詳細表示できるようにしました。
- `/rep integrations test` を追加し、外部連携だけに絞った安全な診断を実行できるようにしました。
- `/rep integrations` の表示を、configuredEnabled、pluginPresent、apiAvailable、active、LuckPerms weight 設定、CoreProtect lookup 設定まで確認できる内容に拡張しました。
- LuckPerms metadata に `primaryGroup`、`reporterWeight`、`bypassGroup`、`applyWeightToDeduction` を保存し、説明責任を強化しました。
- CoreProtect metadata に `resultCount`、`lookupSeconds`、`radius`、`category`、`world`、`x`、`y`、`z`、`apiVersion` を保存するようにしました。
- CoreProtect summary を日本語運用向けに改善し、`max-results: 0` では個別行を保存せず resultCount のみ保存する方針を明確化しました。
- `scripts/check-optional-dependency-safety.sh` を追加し、review archive と `review_code.sh` に統合しました。
- `docs/INTEGRATION_RUNTIME_SMOKE_CHECKLIST.md` と `scripts/record-integration-runtime-smoke-result.sh` を追加しました。
- CoreProtect rollback、restore、purge と LuckPerms 書き込み操作は引き続き行いません。

## 0.15.0

- docs localization check script を追加しました。
- release candidate checklist を追加しました。
- Paper runtime smoke status reporting を改善しました。
- localized docs 向けの release artifact verification を強化しました。

## 0.14.0

- README.md を日本語トップページとして再整備しました。
- docs/*.md を日本語化し、command names、permission nodes、config keys、file names は維持しました。
- `docs/phase-14.md` を追加しました。
- release artifact と review archive の対象を `ReputationBan-0.14.0.jar` に更新しました。
- `scripts/review_code.sh` に日本語ドキュメント確認と v0.14.0 artifact 確認を追加しました。
- Discord Webhook URL を docs、support bundle、review archive に含めない方針を日本語で明確化しました。

## 0.13.0

- token/password/secret/session/cookie/webhook/url-like values の free-text redaction を強化しました。
- support bundle の `meta.txt` と `doctor.txt` で absolute path exposure を減らしました。
- release artifact verification と release ZIP SHA256 generation を追加しました。
- Paper runtime smoke result recording と report template を追加しました。
- security/redaction documentation と review archive collection を拡充しました。

## 0.12.0

- `/rep backup [reason]` を追加し、`DB_BACKUP_CREATED` audit events 付きの manual SQLite backups を作成できるようにしました。
- `/rep support bundle` を追加し、DB files と server logs を除外した secret-redacted diagnostic ZIPs を作成できるようにしました。
- webhook URLs、URL-like values、passwords、tokens、secrets、sessions、cookies 向けの config redaction utilities を追加しました。
- `scripts/create-release-artifact.sh` を追加し、`build/release` JAR、SHA256、release ZIP artifacts を作成できるようにしました。
- review archive、local smoke、runtime smoke、release readiness、support bundle documentation を Phase 12 向けに更新しました。

## 0.11.0

- installation、configuration、migration、v1.0.0 readiness 向けの release preparation と operation documents を追加しました。
- `/rep version` と TAB completion を追加しました。
- safe Paper runtime smoke helper script を追加しました。
- `/rep doctor` に plugin data folder、database file、audit export、Discord webhook state、backup directory checks を追加し、Webhook URL は出さないようにしました。
- review archive generation と local smoke checks の重複 build を減らしました。

## 0.10.0

- `/rep doctor` と `/rep diagnostics` を追加しました。
- safe booleans と counts のみを含む `DIAGNOSTICS_RUN` audit metadata を追加しました。
- `bans.unbanned_by` durable actor ID と `bans.unbanned_by_name` display name を分離しました。
- review archives に local smoke output を収集するようにしました。

## 0.9.0

- config validation と safe audit export path handling を追加しました。
- maintenance preview と confirmed cleanup flow を追加しました。
- retention cleanup 前の SQLite backup を追加しました。
- runtime smoke checklist と review archive secret scan を追加しました。

## 0.8.0

- `audit_events`、audit commands、CSV export、retention policy を追加しました。
- moderation、score、ban、recovery、reload、maintenance の audit events を記録するようにしました。
- webhook URLs とその他 secrets を audit metadata と CSV output から除外しました。

## 0.7.0

- reporter playtime と server account age gates を追加しました。
- `threshold_pending` を使う multi-report threshold flow を追加しました。
- score threshold notifications を追加しました。

## 0.6.0

- optional Discord webhook notifications を追加しました。
- event-level notification toggles、JSON escaping、content truncation、failure log rate limiting を追加しました。

## 0.5.0

- TAB completion と help commands を追加しました。
- `bans.unban_reason` migration と ban history output 改善を追加しました。
- command input validation を改善しました。

## 0.4.0

- ban review gates と bypass checks を安全化しました。
- ban history、ban info、unban、pardon commands を追加しました。

## 0.3.0

- false-report penalties と report suspension を追加しました。
- score recovery と richer report listing を追加しました。

## 0.2.0

- staff report review と score administration commands を追加しました。
- score history と manual recovery workflows を追加しました。

## 0.1.0

- PaperMC 26.1.2 / Java 25 plugin の初期版です。
- SQLite storage、player reputation scores、`/reportbad`、`/rep`、basic automatic profile bans を追加しました。
