# ReputationBan v1.0.0 Maintenance Baseline

作成日時: `2026-05-28T13:56:04+09:00`

## Repository State

- Current branch: `main`
- Current main HEAD at intake: `1e1cd86016d9 Phase 35: add ReputationBan v1.0.0 support templates`
- v1.0.0 tag object: `e2a0b89897db12b50716b5ea11fe49ada687a5c8`
- v1.0.0 tag commit: `b422e72ec5a917cdc04dee902e96a0cef190026c`
- Version: `1.0.0`
- Version bump: none
- v1.0.0 tag movement: none

## GitHub Release

- GitHub Release URL: <https://github.com/Nobu0707/ReputationBan/releases/tag/v1.0.0>
- Release status: `PUBLISHED`
- `tagName=v1.0.0`
- `isDraft=false`
- `isPrerelease=false`
- Release notes body stale draft wording: none detected

## Release Assets

- `ReputationBan-1.0.0.jar`
- `ReputationBan-1.0.0.jar.sha256`
- `ReputationBan-1.0.0-release.zip`
- `ReputationBan-1.0.0-release.zip.sha256`

Release assets were checked for presence only. Phase 36 does not delete, replace, or re-upload v1.0.0 assets.

## Runtime Gate State

- Build/test: expected to remain PASS through Phase 36 verification.
- Paper runtime smoke: PASS from v1.0.0 release gate records.
- Integration runtime smoke: PASS from v1.0.0 release gate records.
- Player report/evidence runtime smoke: PASS, carried forward from the Phase 27 manual confirmation.
- Runtime smoke consistency: expected PASS.
- v1 release judgment after publication: `RELEASED_WITH_DISCORDSRV_WARNING`.

## DiscordSRV Configured Smoke

- Status: `NOT_RUN`
- Result: `NOT_RUN`
- Judgment: `HOLD_FOR_DISCORDSRV_CONFIGURED_SMOKE`
- Classification: operational verification candidate, not a confirmed v1.0.1 bug.
- Secret policy: Discord bot token, Webhook URL, secret, session, cookie, and password values are not requested or recorded.

## Open Issues

- none

## Open PRs

- none

## Confirmed Bug Candidates

- none

## v1.0.1 Candidates

- No confirmed bugfix candidate is currently selected for v1.0.1.
- DiscordSRV configured smoke remains a post-release operational verification candidate.
- Docs/support-only improvements remain allowed for v1.0.x when they reduce support risk without changing runtime behavior.

## Issue/PR Intake Dry-Run

- GitHub issue templates: present
- GitHub PR template: present
- `SECURITY.md`: present
- `SUPPORT.md`: present
- `CONTRIBUTING.md`: present
- Open issue count: `0`
- Open PR count: `0`
- Missing intake docs/templates found during dry-run: none

## Next Action

- Continue post-release monitoring.
- If a bug report arrives, triage it with `docs/ISSUE_TRIAGE_GUIDE.md` and record confirmed candidates in `docs/V1_0_1_CANDIDATES.md`.
- Do not move the `v1.0.0` tag or replace v1.0.0 Release assets.
