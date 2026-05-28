# v1.0.0 Release Execution Plan

この文書は Phase 30 の v1.0.0 tag / GitHub Release draft preparation 実行計画です。Phase 30 では `v1.0.0` annotated tag を作成して GitHub へ push し、GitHub Release は draft まで作成します。GitHub Release 公開、`draft=false` への変更、v1.0.1 以降への version bump はまだ行いません。

## Tag 作成前チェック

```bash
git status --short
git log --oneline -5
git tag --list "v1.0.0"
git ls-remote --tags origin "refs/tags/v1.0.0"
```

期待:

- working tree が clean です。
- HEAD が Phase 30 commit です。
- `v1.0.0` tag が存在しません。
- remote `v1.0.0` tag が存在しません。

## Release artifact 確認

```bash
ls -lh build/release/
sha256sum -c build/release/ReputationBan-1.0.0.jar.sha256
sha256sum -c build/release/ReputationBan-1.0.0-release.zip.sha256
cat build/release/ReputationBan-v1-go-no-go-report.md
cat build/release/ReputationBan-v1.0.0-release-notes.md
```

確認対象:

- `build/release/ReputationBan-1.0.0.jar`
- `build/release/ReputationBan-1.0.0.jar.sha256`
- `build/release/ReputationBan-1.0.0-release.zip`
- `build/release/ReputationBan-1.0.0-release.zip.sha256`
- `build/release/ReputationBan-v1-go-no-go-report.md`
- `build/release/ReputationBan-v1.0.0-release-notes.md`

Go/No-Go report の judgment は `READY_FOR_V1_RELEASE_WITH_DISCORDSRV_WARNING` を期待します。Tag 作成前は `Tag status: NOT_CREATED`、GitHub Release draft 作成前は `GitHub Release status: NOT_CREATED` を確認します。

## Tag 作成コマンド

Phase 30 commit を main へ push した後に実行します。既存 tag がある場合は上書きしません。

```bash
git push origin main
git tag -a v1.0.0 -m "Release ReputationBan v1.0.0"
git push origin v1.0.0
```

## GitHub Release draft 作成方針

- `build/release/ReputationBan-v1.0.0-release-notes.md` を release notes の本文候補にします。
- GitHub Release は draft として作成します。
- 添付 artifact は JAR、JAR sha256、release zip、release zip sha256 です。
- draft 上で SHA256、DiscordSRV WARN、対応環境、インストール注意を確認します。
- Phase 30 では publish しません。
- `gh release edit --draft=false` は実行しません。

```bash
gh release create v1.0.0 \
  --draft \
  --title "ReputationBan v1.0.0" \
  --notes-file build/release/ReputationBan-v1.0.0-release-notes.md \
  build/release/ReputationBan-1.0.0.jar \
  build/release/ReputationBan-1.0.0.jar.sha256 \
  build/release/ReputationBan-1.0.0-release.zip \
  build/release/ReputationBan-1.0.0-release.zip.sha256
```

確認:

```bash
gh release view v1.0.0 --json tagName,name,isDraft,isPrerelease,url
```

期待:

- `isDraft=true`
- `isPrerelease=false`

## Rollback 方針

- GitHub Release draft 作成前に問題があれば原因を記録し、tag 作成や draft 作成へ進みません。
- tag push 後、Release 公開前に問題が見つかった場合は、原因を記録し、ユーザー承認を得てから draft 更新、tag 削除、または修正リリースへ進みます。
- server 側 rollback は server を停止し、現在の `plugins/ReputationBan/reputationban.db` を退避してから直近 backup を復元します。
- 直前に利用していた ReputationBan JAR へ戻し、起動後に `/rep doctor`、`/rep audit recent`、主要 commands を確認します。
