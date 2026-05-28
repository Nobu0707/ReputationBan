# Phase 37 / v1.0.1 hotfix candidate

Phase 37 は 100人規模本番運用向け production hardening です。新機能追加ではなく、v1.0.1 hotfix candidate artifact を作成するための安全性・一貫性・運用耐性の修正だけを行います。

## 実装内容

- version を `1.0.1` に更新。
- TargetProtectionService を追加し、OP、`reputationban.bypass`、LuckPerms bypass group の保護判定を一元化。
- LuckPerms bypass 判定に限り offline `loadUser(UUID)` を非同期 reflection で利用し、timeout と fail closed 設定を追加。
- PunishmentService の自動BAN直前に TargetProtectionService を再確認。
- DatabaseManager shutdown を `closed` flag、failed future、`awaitTermination`、`shutdownNow` fallback で hardening。
- SQLite `busy_timeout = 5000` と `synchronous = NORMAL` を追加。
- report reason、review note、audit reason、report_context summary の最大長を追加。
- ScoreService の score mutation で `players` 行がない場合に row を作成し、`score_history` との不整合を防止。
- Bukkit Profile BAN 成功後に DB record が失敗した場合、SEVERE log と staff notification で不整合を検出しやすくした。
- reports / players / bans / report_context 用の追加 index を作成。
- review_code.sh、runtime smoke scripts、release artifact scripts を v1.0.1 に更新。

## Release 方針

- v1.0.1 tag/releaseはまだ未実施。
- GitHub Release `v1.0.1` は作成しない。
- v1.0.0 tag と公開済み Release assets は変更しない。

## Runtime smoke

- Paper runtime smoke: PASS 必須。
- Integration runtime smoke: PASS 必須。
- Player report runtime smoke: 実プレイヤー2名以上が必要なため、Codex 環境で実施できない場合は `NOT_RUN` / `HOLD` として Phase 38 で手動再確認。
