# 外部連携

ReputationBan 0.16.0 では LuckPerms と CoreProtect を任意連携として扱います。どちらも `softdepend` であり、未導入、未ロード、API unavailable の場合でも ReputationBan 単体の通報、監査、BAN、backup、support bundle は継続します。

Phase 16a では optional dependency class loading を安全化するため、LuckPerms / CoreProtect API 型を起動時に無条件ロードされるクラスから外し、reflection adapter 経由で必要な時だけ参照します。LuckPerms だけ、CoreProtect だけ、またはどちらも未導入の構成でも、外部 API 欠落による `NoClassDefFoundError` で ReputationBan 本体が起動不能にならない設計です。

## LuckPerms

LuckPerms 連携は、通報者の primary group を使って reporter weight を計算し、通報の補助情報として保存します。Phase 16 では安全のため、既定では減点量に反映しません。`apply-weight-to-deduction: false` のままなら、`report_context` と audit metadata に `primaryGroup` と `reporterWeight` を記録するだけです。

`bypass-groups` は `reputationban.bypass` 権限と OP 保護に加える補助保護です。対象プレイヤーの primary group が `admin` や `owner` などに一致した場合、通報、減点、自動 BAN の対象外として扱います。LuckPerms が無い場合、この判定はスキップされます。

Phase 16 では、LuckPerms のオフラインユーザー情報が未ロードの場合、primary group を取得せず `default-weight` 扱いになる場合があります。

## CoreProtect

CoreProtect 連携は、荒らし通報の審査補助として、通報者の現在位置周辺にある対象プレイヤーの block-break / block-place ログを短いサマリーとして保存します。これは rollback ではありません。Phase 16 では `performLookup` のみを使い、rollback、restore、purge は行いません。

CoreProtect の証拠サマリーは自動 BAN の唯一根拠にしないでください。あくまで `/reports view <id>` や audit log で審査者が状況を確認しやすくするための補助情報です。保存件数は `max-results` で制限し、大量ログや不要な個人情報を保存しない方針です。

## Commands

- `/rep integrations`: LuckPerms / CoreProtect の `active`、`pluginPresent`、`apiAvailable` を表示します。
- `/rep doctor`: 診断情報に `LuckPerms integration` と `CoreProtect integration` の簡易状態を表示します。
- `/reports view <id>`: LuckPerms reporter weight と CoreProtect 証拠サマリーがある場合に表示します。

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
```

## 採用依存

- LuckPerms API: `net.luckperms:api:5.5`
- CoreProtect API: `net.coreprotect:coreprotect:23.2`
- CoreProtect repository: `https://maven.playpro.com/`
- CoreProtect minimum API version: `11`
