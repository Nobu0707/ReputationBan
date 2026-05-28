# Phase 38 / v1.0.1 user documentation

Phase 38 は Player / Operator documentation phase です。v1.0.1 production hardening candidate の状態を前提に、リポジトリ利用者が導入、運用、通報対応、外部連携、トラブル対応を追いやすくするための利用者向けドキュメントを追加しました。

## 目的

- プレイヤー向けガイドを追加する。
- 運営向けガイドを追加する。
- コマンドリファレンスを追加する。
- README と support 系ドキュメントから利用者向けドキュメントへリンクする。
- review archive に user docs の存在確認を含める。

## 追加ドキュメント

- `docs/PLAYER_GUIDE.md`: 一般プレイヤー向けに、ReputationBan、評判スコア、`/reportbad`、`/rep`、BAN、虚偽通報、FAQ を説明。
- `docs/OPERATOR_GUIDE.md`: サーバー運営向けに、初期確認、通報対応、スコア管理、BAN管理、監査、メンテナンス、バックアップ、support bundle、外部連携、100人規模運用、トラブル対応を説明。
- `docs/COMMAND_REFERENCE.md`: プレイヤーコマンド、管理コマンド、通報管理、監査、メンテナンス、バックアップ、外部連携確認、権限ノードを整理。

## 変更しないもの

- Javaコード変更なし。
- DBスキーマ変更なし。
- config key変更なし。
- version変更なし。version は `1.0.1` のまま。
- `v1.0.1` tag 作成なし。
- GitHub Release 作成なし。
- GitHub Release asset 差し替えなし。
