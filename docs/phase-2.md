# Phase 2 / v0.2.0

Phase 2 では、staff review と score administration を追加しました。

## 主な変更

- pending report の list、view、approve、reject を追加しました。
- approve 時に category deduction、report status、score history を transaction で更新します。
- `/rep history`、`/rep add`、`/rep remove`、`/rep set` を追加しました。
- score mutation を `ScoreService` に集約しました。
- automatic ban completion は Paper Profile BAN call の成功または失敗後に future が完了します。
- review archive に remotes、tags、build outputs、test summaries、review script output を追加しました。
- plugin version を `0.2.0` に更新しました。

## 後続フェーズへ残したもの

Discord webhook delivery、GUI moderation menus、LuckPerms/CoreProtect/WorldGuard/GriefPrevention integration、trust weighting、contact checks、abuse clustering、appeal/pardon/unban workflow、Folia support。
