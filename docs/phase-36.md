# Phase 36

Phase 36 は v1.0.0 公開後の post-release maintenance baseline / issue intake dry-run phase です。version bump、`v1.0.1` tag 作成、GitHub Release asset 差し替え、GitHub Release 削除、`v1.0.0` tag 移動は行いません。

## 目的

- GitHub Release v1.0.0 公開状態を再確認する。
- GitHub issue templates、PR template、`SECURITY.md`、`SUPPORT.md`、`CONTRIBUTING.md` が見える状態を再確認する。
- GitHub Issues / PRs の現在状態を確認する。
- open issue / open PR があれば一覧化する。
- v1.0.1 へ入れるべき confirmed bug があるか判断できる baseline を作る。
- bugfix intake dry-run を行い、template や docs の不足があれば docs/scripts だけ修正する。
- release gate / runtime smoke / DiscordSRV WARN 状態を保守向けに再整理する。
- review archive に Phase 36 の確認結果を含める。

## 公開状態

- GitHub Release URL: <https://github.com/Nobu0707/ReputationBan/releases/tag/v1.0.0>
- Release status: `PUBLISHED`
- `tagName=v1.0.0`
- `isDraft=false`
- `isPrerelease=false`
- Release assets: 4件
- v1.0.0 tag commit: `b422e72ec5a917cdc04dee902e96a0cef190026c`

`git rev-parse v1.0.0` は annotated tag object `e2a0b89897db12b50716b5ea11fe49ada687a5c8` を返します。release commit の確認には `git rev-list -n 1 v1.0.0` を使い、Phase 30 commit を指していることを確認します。

## Intake Dry-Run

- Open issues: none
- Open PRs: none
- Confirmed bug candidates: none
- v1.0.1 candidates: none selected
- DiscordSRV configured smoke: `NOT_RUN` / `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE`

DiscordSRV configured smoke は本番で DiscordSRV 通知や account link context を使う前の operational verification candidate です。現時点では confirmed bug ではありません。

## 追加/更新

- `docs/MAINTENANCE_BASELINE.md`
- `docs/ISSUE_TRIAGE_GUIDE.md`
- `docs/phase-36.md`
- `scripts/check-maintenance-baseline.sh`
- `scripts/review_code.sh`
- `scripts/make-review-archive.sh`
- README、post-release monitoring、bugfix intake、v1.0.1 candidates、release readiness、phase plan、CHANGELOG の Phase 36 反映

## 変更しないもの

- version は `1.0.0` のままです。
- `v1.0.0` tag は移動しません。
- GitHub Release v1.0.0 は公開済みのままです。
- GitHub Release assets は差し替えません。
- 新機能、DB schema、runtime behavior は変更しません。
- Discord bot token は要求、表示、保存しません。
