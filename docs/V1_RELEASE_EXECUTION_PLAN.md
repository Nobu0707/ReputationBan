# v1.0.0 Release Execution Plan

この文書は Phase 29 で作成する実行計画です。Phase 29 では `v1.0.0` tag 作成と GitHub Release 公開は行いません。次Phaseでユーザーの明示承認後に実行します。

## Tag 作成前チェック

```bash
git status --short
git log --oneline -5
git tag --list "v1.0.0"
```

期待:

- working tree が clean です。
- HEAD が Phase 29 commit です。
- `v1.0.0` tag が存在しません。

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

Go/No-Go report の judgment は `READY_FOR_V1_RELEASE_WITH_DISCORDSRV_WARNING` を期待します。`Tag status: NOT_CREATED` と `GitHub Release status: NOT_CREATED` も確認します。

## Tag 作成コマンド

ユーザー承認後にだけ実行します。

```bash
git tag -a v1.0.0 -m "Release ReputationBan v1.0.0"
git push origin main
git push origin v1.0.0
```

## GitHub Release draft 作成/公開方針

- `build/release/ReputationBan-v1.0.0-release-notes.md` を release notes の本文候補にします。
- GitHub Release は最初に draft として作成します。
- 添付 artifact は JAR、JAR sha256、release zip、release zip sha256 です。
- draft 上で SHA256、DiscordSRV WARN、対応環境、インストール注意を確認します。
- ユーザー承認後に publish します。

## Rollback 方針

- GitHub Release 公開前に問題があれば draft を更新または削除し、tag は作成しません。
- tag push 後、Release 公開前に問題が見つかった場合は、原因を記録し、ユーザー承認を得てから tag 削除または修正リリースへ進みます。
- server 側 rollback は server を停止し、現在の `plugins/ReputationBan/reputationban.db` を退避してから直近 backup を復元します。
- 直前に利用していた ReputationBan JAR へ戻し、起動後に `/rep doctor`、`/rep audit recent`、主要 commands を確認します。
