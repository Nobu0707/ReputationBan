# Phase 11 / v0.11.0

Phase 11 では、v1.0.0 前に distribution、installation、real Paper server smoke checks を行いやすくしました。

## 範囲

- version を `0.11.0` に更新しました。
- `/rep version` を追加しました。
- release と operation documents を追加しました。
- `CHANGELOG.md` を追加しました。
- Paper runtime smoke helper script を追加しました。
- `/rep doctor` に release-readiness state checks を追加しました。
- review scripts を Phase 11 artifacts に対応し、archive generation 中の重複 build を減らしました。

## 範囲外

- GUI
- LuckPerms、CoreProtect、WorldGuard、protection plugin integration
- 現在の reputation score を超える trust score systems
- appeal workflow implementation
- Folia support

## Artifacts

- JAR: `build/libs/ReputationBan-0.11.0.jar`
- Runtime helper: `scripts/run-paper-runtime-smoke-helper.sh`
- Readiness checklist: `docs/RELEASE_READINESS.md`
