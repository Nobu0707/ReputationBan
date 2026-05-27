package dev.modplugin.reputationban.integration.worldguard;

import dev.modplugin.reputationban.util.AuditMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record WorldGuardRegionSummary(
        int regionCount,
        String world,
        int x,
        int y,
        int z,
        List<WorldGuardRegionEntry> regions,
        String summary,
        String metadata
) {
    private static final int SUMMARY_MAX_LENGTH = 1000;

    public static WorldGuardRegionSummary create(
            int regionCount,
            String world,
            int x,
            int y,
            int z,
            int maxRegions,
            List<WorldGuardRegionEntry> regions
    ) {
        String header = "WorldGuard: regions " + regionCount
                + " world=" + world
                + " x=" + x
                + " y=" + y
                + " z=" + z;
        List<String> lines = new ArrayList<>();
        int count = WorldGuardRegionPolicy.clampMaxRegions(maxRegions, regions.size());
        for (int index = 0; index < count; index++) {
            WorldGuardRegionEntry entry = regions.get(index);
            StringBuilder line = new StringBuilder("#").append(index + 1)
                    .append(" id=").append(entry.id())
                    .append(" priority=").append(entry.priority())
                    .append(" owners=").append(entry.owners())
                    .append(" members=").append(entry.members());
            if (!entry.flags().isEmpty()) {
                line.append(" flags=").append(formatFlags(entry.flags()));
            }
            lines.add(line.toString());
        }
        String summary = lines.isEmpty() ? header : header + "\n" + String.join("\n", lines);
        List<WorldGuardRegionEntry> storedRegions = List.copyOf(regions.subList(0, count));
        String metadata = AuditMetadata.create()
                .put("regionCount", regionCount)
                .put("world", world)
                .put("x", x)
                .put("y", y)
                .put("z", z)
                .put("maxRegions", maxRegions)
                .put("flags", combinedFlags(storedRegions))
                .toJson();
        return new WorldGuardRegionSummary(
                regionCount,
                world,
                x,
                y,
                z,
                storedRegions,
                WorldGuardRegionPolicy.truncateSummary(summary, SUMMARY_MAX_LENGTH),
                metadata
        );
    }

    private static String combinedFlags(List<WorldGuardRegionEntry> regions) {
        return regions.stream()
                .flatMap(entry -> entry.flags().entrySet().stream())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private static String formatFlags(Map<String, String> flags) {
        return flags.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }
}
