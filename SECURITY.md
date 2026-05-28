# Security Policy

ReputationBan は PaperMC 26.1.2 / Java 25 向け moderation plugin です。security report では、secret を公開Issueに貼らず、再現に必要な最小情報だけを共有してください。

## サポート対象version

| Version | Status |
| --- | --- |
| `1.0.0` | Supported |

v1.0.x は原則 bugfix と安全な docs/support 改善を中心に扱います。新機能は v1.1.0 以降の候補として整理します。

## 脆弱性報告方法

- まず GitHub issue で、secret を伏せて概要、影響範囲、再現条件、ReputationBan version、Paper version、Java version を報告してください。
- 公開Issueに実際の secret を貼る必要はありません。必要な場合も `<redacted>`、`configured=true`、`configured=false` のような安全な値で説明してください。
- support bundle を使う場合は `/rep support bundle` で作成し、添付前に ZIP 内の `config-redacted.yml` と `README-SHARING.txt` を確認してください。
- secret 漏えいが疑われる場合は、ReputationBan 側の修正相談とは別に、該当 token や Webhook URL を即時 rotate / revoke してください。

## 公開Issueに貼ってはいけない情報

- Discord bot token
- Discord Webhook URL
- password
- session / cookie
- secret key
- private database
- 公開不要な server private paths
- 個人情報を含む log 全体

## support bundleの使い方

1. server console または管理者権限を持つ player で `/rep support bundle` を実行します。
2. 作成された ZIP に DB files、WAL/SHM、server logs、Webhook URL が含まれていないことを確認します。
3. `config-redacted.yml` で Webhook URL、token、password、secret、session、cookie が `<redacted>` になっていることを確認します。
4. 必要な場合だけ issue に添付します。

## 緊急度

- `critical`: secret 漏えい、任意コマンド実行、data loss、server 起動不能。
- `high`: BAN / unban / report approval の重大な誤動作、権限 bypass。
- `medium`: optional integration 経由で限定的に再現する不具合。
- `low`: 表示、docs、診断情報の不足。

## 対応方針

- `v1.0.0` tag は移動しません。
- 公開済み GitHub Release assets は差し替えません。
- 修正が必要な場合は別 commit、別 tag、別 Release として扱います。
- DB schema 変更は v1.0.x では慎重に扱い、必要性と migration 方針を明記します。
- CoreProtect rollback / restore / purge、LuckPerms 書き込み、WorldGuard region/flag 変更、GriefPrevention claim/trust 変更、Discord role 変更、Discord から Minecraft command 実行は追加しません。
