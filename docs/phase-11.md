# Phase 11 / v0.11.0

Phase 11 prepares ReputationBan for easier distribution, installation, and real Paper server smoke checks before v1.0.0.

## Scope

- Version updated to `0.11.0`.
- Added `/rep version`.
- Added release and operation documents.
- Added `CHANGELOG.md`.
- Added Paper runtime smoke helper script.
- Extended `/rep doctor` with release-readiness state checks.
- Updated review scripts for Phase 11 artifacts and reduced repeated builds during archive generation.

## Not In Scope

- GUI
- LuckPerms, CoreProtect, WorldGuard, or protection plugin integration
- Trust score systems beyond current reputation score
- Appeal workflow implementation
- Folia support

## Artifacts

- JAR: `build/libs/ReputationBan-0.11.0.jar`
- Runtime helper: `scripts/run-paper-runtime-smoke-helper.sh`
- Readiness checklist: `docs/RELEASE_READINESS.md`
