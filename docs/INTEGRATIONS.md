# 外部連携

ReputationBan 0.17.0 では LuckPerms と CoreProtect を任意連携として扱います。どちらも `softdepend` であり、未導入、未ロード、API unavailable の場合でも ReputationBan 単体の通報、監査、BAN、backup、support bundle は継続します。

Phase 16a 以降、optional dependency class loading を安全化するため、LuckPerms / CoreProtect API 型を起動時に無条件ロードされるクラスから外し、reflection adapter 経由で必要な時だけ参照します。LuckPerms だけ、CoreProtect だけ、またはどちらも未導入の構成でも、外部 API 欠落による `NoClassDefFoundError` で ReputationBan 本体が起動不能にならない設計です。

## LuckPerms

LuckPerms 連携は、通報者の primary group を使って reporter weight を計算し、通報の補助情報として保存します。Phase 17 時点では `apply-weight-to-deduction` はデフォルト `false` です。`false` の場合、重みは `report_context` と audit metadata に記録、表示されるだけで減点量には影響しません。`true` にする場合は、サーバールール上の説明責任が必要です。

Phase 17 の実装では `apply-weight-to-deduction` は設定値と metadata 表示のために保持していますが、減点量への実反映はまだ行いません。実反映する場合は後続フェーズで、審査フローと通知文言を含めて明示的に実装します。

`bypass-groups` は `reputationban.bypass` 権限と OP 保護に加える補助保護です。対象プレイヤーの primary group が `admin` や `owner` などに一致した場合、通報、減点、自動 BAN の対象外として扱います。LuckPerms が無い場合、この判定はスキップされます。

Phase 17 では、LuckPerms のオフラインユーザー情報が未ロードの場合、primary group を取得せず `default-weight` 扱いになる場合があります。

## CoreProtect

CoreProtect 連携は、荒らし通報の審査補助として、通報者の現在位置周辺にある対象プレイヤーの block-break / block-place ログを短いサマリーとして保存します。これは rollback ではありません。Phase 17 では `performLookup` のみを使い、rollback、restore、purge は行いません。

CoreProtect の証拠サマリーは自動 BAN の唯一根拠にしないでください。あくまで `/reports view <id>`、`/reports evidence <id>`、audit log で審査者が状況を確認しやすくするための補助情報です。保存件数は `max-results` で制限し、大量ログや不要な個人情報を保存しない方針です。

`max-results: 0` の場合は個別行を保存せず、`resultCount`、lookup 条件、座標、API version だけを metadata に保存します。

## Commands

- `/rep integrations`: LuckPerms / CoreProtect の設定値、plugin presence、API availability、active、lookup 設定を表示します。
- `/rep integrations test`: LuckPerms / CoreProtect だけに絞った詳細診断です。破壊的操作や CoreProtect 実 lookup は行いません。
- `/rep doctor`: ReputationBan 全体診断です。database、tables、config、Discord、backup、連携の簡易状態をまとめて確認します。
- `/reports view <id>`: LuckPerms reporter weight と CoreProtect 証拠サマリーがある場合に概要を表示します。
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
```

## 採用依存

- LuckPerms API: `net.luckperms:api:5.5`
- CoreProtect API: `net.coreprotect:coreprotect:23.2`
- CoreProtect repository: `https://maven.playpro.com/`
- CoreProtect minimum API version: `11`
