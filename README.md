# ReputationBan

ReputationBan is a PaperMC moderation plugin that tracks player reputation scores from reports and staff actions. It stores data in SQLite, supports pending report review, and can trigger profile-based temporary or permanent bans when scores cross the configured threshold.

## Requirements

- Minecraft/PaperMC 26.1.2
- Java 25
- Gradle wrapper included
- SQLite JDBC loaded through `plugin.yml` libraries

## Commands

- `/reportbad <player> <category> <reason>`: report a player.
- `/rep`: show your own score.
- `/rep check <player>`: show another player's score.
- `/rep history <player> [limit]`: show recent score history.
- `/rep add <player> <points> [reason...]`: add score points.
- `/rep remove <player> <points> [reason...]`: remove score points.
- `/rep set <player> <score> [reason...]`: set an exact score.
- `/rep reload`: reload configuration.
- `/reports`: list pending reports.
- `/reports list [pending|approved|rejected|auto_accepted|cancelled|all] [limit]`: list reports.
- `/reports view <id>`: show report details.
- `/reports approve <id> [note...]`: approve a pending report and apply score deduction.
- `/reports reject <id> [note...]`: reject a pending report.

## Permissions

- `reputationban.report`: use `/reportbad`.
- `reputationban.score.self`: view your own score.
- `reputationban.score.others`: view other players' scores and histories.
- `reputationban.admin.score`: use score administration commands.
- `reputationban.admin.reports`: review reports.
- `reputationban.admin.ban`: allow manual score changes that cross into the ban threshold.
- `reputationban.notify`: receive staff notifications.
- `reputationban.bypass`: bypass reports, deductions, and automatic bans while online.
- `reputationban.admin`: grants the main admin permissions.

## Build And Test

```bash
./gradlew clean test build
./scripts/review_code.sh
```

The plugin jar is written to `build/libs/ReputationBan-0.3.0.jar`.

## Current Limitations

- Discord integration, GUI menus, permissions plugin integration, and protection plugin integrations are not implemented.
- Offline bypass detection is limited to OP status; online players with `reputationban.bypass` are protected.
- Appeal, pardon, and automatic unban workflows are deferred.
- Folia support is not included.
