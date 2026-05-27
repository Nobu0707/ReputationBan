# 外部連携

ReputationBan 0.20.0 では LuckPerms、CoreProtect、WorldGuard、GriefPrevention、PlaceholderAPI を任意連携として扱います。いずれも `softdepend` であり、未導入、未ロード、API unavailable の場合でも ReputationBan 単体の通報、監査、BAN、backup、support bundle は継続します。

Phase 16a 以降、optional dependency class loading を安全化するため、LuckPerms / CoreProtect / WorldGuard / GriefPrevention API 型を起動時に無条件ロードされるクラスから外し、reflection adapter 経由で必要な時だけ参照します。PlaceholderAPI は `PlaceholderExpansion` 継承が必要なため、直接 import を `ReputationBanPlaceholderExpansion` だけに隔離し、PlaceholderAPI が存在する場合だけ reflection でロードします。外部プラグインが一部またはすべて未導入の構成でも、外部 API 欠落による `NoClassDefFoundError` で ReputationBan 本体が起動不能にならない設計です。

## LuckPerms

LuckPerms 連携は、通報者の primary group を使って reporter weight を計算し、通報の補助情報として保存します。Phase 17 時点では `apply-weight-to-deduction` はデフォルト `false` です。`false` の場合、重みは `report_context` と audit metadata に記録、表示されるだけで減点量には影響しません。`true` にする場合は、サーバールール上の説明責任が必要です。

Phase 17 の実装では `apply-weight-to-deduction` は設定値と metadata 表示のために保持していますが、減点量への実反映はまだ行いません。実反映する場合は後続フェーズで、審査フローと通知文言を含めて明示的に実装します。

`bypass-groups` は `reputationban.bypass` 権限と OP 保護に加える補助保護です。対象プレイヤーの primary group が `admin` や `owner` などに一致した場合、通報、減点、自動 BAN の対象外として扱います。LuckPerms が無い場合、この判定はスキップされます。

Phase 17 では、LuckPerms のオフラインユーザー情報が未ロードの場合、primary group を取得せず `default-weight` 扱いになる場合があります。

## CoreProtect

CoreProtect 連携は、荒らし通報の審査補助として、通報者の現在位置周辺にある対象プレイヤーの block-break / block-place ログを短いサマリーとして保存します。これは rollback ではありません。Phase 17 では `performLookup` のみを使い、rollback、restore、purge は行いません。

CoreProtect の証拠サマリーは自動 BAN の唯一根拠にしないでください。あくまで `/reports view <id>`、`/reports evidence <id>`、audit log で審査者が状況を確認しやすくするための補助情報です。保存件数は `max-results` で制限し、大量ログや不要な個人情報を保存しない方針です。

`max-results: 0` の場合は個別行を保存せず、`resultCount`、lookup 条件、座標、API version だけを metadata に保存します。

## WorldGuard

WorldGuard 連携は、荒らしや嫌がらせ通報の審査補助として、通報者の現在地に適用される region context を保存します。WorldGuard は WorldEdit を必要とするため、`plugin.yml` では `WorldEdit` と `WorldGuard` の両方を `softdepend` にしています。

WorldGuard / WorldEdit が未導入でも ReputationBan 本体は起動します。Java ソースでは `com.sk89q.*` を直接 import せず、`WorldGuardReflectionAdapter` の reflection lookup に閉じ込めています。

保存される provider は `worldguard` です。summary には region 件数、world、block 座標、region id、priority、owner/member の hidden または count、取得できる範囲の flag が入ります。owner/member は privacy のためデフォルトでは `hidden` です。

WorldGuard の region context は自動処罰の唯一根拠にしないでください。ReputationBan は WorldGuard region の作成、変更、削除、flag 変更を行いません。Phase 18 では protection 設定を読み取り、審査者が `/reports view <id>` と `/reports evidence <id>` で確認できる補助情報として扱います。

## GriefPrevention

GriefPrevention 連携は、荒らし、嫌がらせ、詐欺などの通報で、通報者の現在地が claim 内かどうかを審査補助として保存します。GriefPrevention が未導入でも ReputationBan 本体は起動し、通報、監査、BAN 処理は継続します。

Java ソースでは GriefPrevention API 型を直接 import せず、`GriefPreventionReflectionAdapter` が `Class.forName` と reflection で `getClaimAt` を呼びます。外へ出す値は ReputationBan 独自の claim summary だけです。

保存される provider は `griefprevention` です。summary には claim 有無、world、block 座標、claim id、admin claim かどうか、設定に応じた owner、trust count、境界座標が入ります。privacy のため `include-claim-owner` と `include-trust-counts` はデフォルト `false` です。

GriefPrevention の claim context は自動処罰の唯一根拠にしないでください。ReputationBan は claim の作成、変更、削除、owner 変更、trust/permission 変更を行いません。`/rep integrations test` でも現在地の claim 取得だけを行います。

## PlaceholderAPI

PlaceholderAPI 連携は、他プラグイン、scoreboard、TAB、chat 表示から ReputationBan の状態を参照できるようにする読み取り専用連携です。PlaceholderAPI が未導入でも ReputationBan 本体は起動し、`/rep placeholders` では利用可能な placeholder 一覧を表示できます。

提供する placeholder は以下です。

- `%reputationban_score%`
- `%reputationban_max_score%`
- `%reputationban_score_percent%`
- `%reputationban_status%`
- `%reputationban_ban_count%`
- `%reputationban_false_report_count%`
- `%reputationban_report_banned%`
- `%reputationban_report_banned_until%`
- `%reputationban_last_seen%`
- `%reputationban_version%`

`status` は `normal`、`warning`、`watch`、`restricted`、`final-warning`、`banned-threshold` の英字 stable value です。`identifier` の既定値は `reputationban` で、`%reputationban_score%` の prefix になります。

placeholder 呼び出し中に SQLite へ同期問い合わせは行いません。値は online player summary cache から返します。cache は join 後、定期 refresh、主要な score 変更後に更新しますが、Phase 20 では最大 `cache-refresh-seconds` 程度の遅延があり得ます。cache 未取得、offline、player なしの場合は `show-unknown-as` を返します。ただし `version` は player なしでも返します。

## Commands

- `/rep placeholders`: PlaceholderAPI の連携状態、identifier、利用可能な placeholder 一覧を表示します。
- `/rep integrations`: LuckPerms / CoreProtect / WorldGuard / GriefPrevention / PlaceholderAPI の設定値、plugin presence、API availability、active、lookup 設定を表示します。
- `/rep integrations test`: LuckPerms / CoreProtect / WorldGuard / GriefPrevention / PlaceholderAPI だけに絞った詳細診断です。破壊的操作、CoreProtect 実 lookup、WorldGuard region/flag 変更、GriefPrevention claim/trust 変更は行いません。
- `/rep doctor`: ReputationBan 全体診断です。database、tables、config、Discord、backup、連携の簡易状態をまとめて確認します。
- `/reports view <id>`: LuckPerms reporter weight、CoreProtect 証拠サマリー、WorldGuard region context、GriefPrevention claim context がある場合に概要を表示します。
- `/reports evidence <id>`: `report_context` の詳細を provider ごとに表示します。

## 設定例

```yaml
integrations:
  luckperms:
    enabled: true
    use-group-weight: true
    apply-weight-to-deduction: false
    default-weight: 1.0
    group-weights:
      default: 1.0
      trusted: 1.2
      moderator: 2.0
    bypass-groups:
      - admin
      - owner

  coreprotect:
    enabled: true
    minimum-api-version: 11
    report-context:
      enabled: true
      categories:
        - griefing
      lookup-seconds: 3600
      radius: 20
      max-results: 10
      include-actions:
        - block-break
        - block-place

  worldguard:
    enabled: true
    report-context:
      enabled: true
      categories:
        - griefing
        - harassment
      max-regions: 10
      include-region-owners: false
      include-region-members: false
      include-flags:
        - build
        - block-break
        - block-place

  griefprevention:
    enabled: true
    report-context:
      enabled: true
      categories:
        - griefing
        - harassment
        - scam
      include-claim-owner: false
      include-trust-counts: false
      include-boundaries: true

  placeholderapi:
    enabled: true
    identifier: "reputationban"
    cache-refresh-seconds: 60
    show-unknown-as: "-"
```

## 採用依存

- LuckPerms API: `net.luckperms:api:5.5`
- CoreProtect API: `net.coreprotect:coreprotect:23.2`
- CoreProtect repository: `https://maven.playpro.com/`
- CoreProtect minimum API version: `11`
- WorldGuard API: `com.sk89q.worldguard:worldguard-bukkit:7.0.13` (`compileOnly`, transitive dependencies disabled)
- WorldEdit API: `com.sk89q.worldedit:worldedit-bukkit:7.3.0` (`compileOnly`, transitive dependencies disabled)
- EngineHub repository: `https://maven.enginehub.org/repo/`
- GriefPrevention API: compile dependency は追加せず、reflection only で扱います。
- PlaceholderAPI API: `me.clip:placeholderapi:2.12.2` (`compileOnly`)
- PlaceholderAPI repository: `https://repo.helpch.at/releases/`
