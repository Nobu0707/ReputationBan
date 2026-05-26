# Phase 2 / v0.2.0

## What Changed

- Added pending report listing, viewing, approval, and rejection.
- Approval now applies the configured category deduction transactionally with report status and score history updates.
- Added `/rep history`, `/rep add`, `/rep remove`, and `/rep set`.
- Centralized score mutation through `ScoreService` so report and admin score changes share threshold crossing logic.
- Updated automatic ban completion so the returned future completes after the Bukkit profile ban call succeeds or fails.
- Strengthened review archive generation with remotes, tags, build outputs, test summaries, and review script output.
- Bumped plugin version to `0.2.0`.

## Deferred To Phase 3+

- Discord webhook delivery.
- GUI moderation menus.
- LuckPerms, CoreProtect, WorldGuard, and GriefPrevention integration.
- Trust weighting, contact/proximity checks, and abuse clustering.
- Full appeal, pardon, and unban workflows.
- Folia support.
