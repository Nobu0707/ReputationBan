package dev.modplugin.reputationban.integration.coreprotect;

import dev.modplugin.reputationban.util.AuditMetadata;
import java.util.List;

public record CoreProtectEvidenceSummary(
        int resultCount,
        String summary,
        List<String> lines,
        String metadata
) {
    public static CoreProtectEvidenceSummary empty(
            String world,
            int x,
            int y,
            int z,
            int radius,
            int lookupSeconds,
            String category,
            int apiVersion
    ) {
        return new CoreProtectEvidenceSummary(
                0,
                "CoreProtect: 周辺ログ 0件 world=" + world + " x=" + x + " y=" + y + " z=" + z + " radius=" + radius,
                List.of(),
                metadata(0, lookupSeconds, radius, category, world, x, y, z, apiVersion)
        );
    }

    public static String metadata(
            int resultCount,
            int lookupSeconds,
            int radius,
            String category,
            String world,
            int x,
            int y,
            int z,
            int apiVersion
    ) {
        return AuditMetadata.create()
                .put("resultCount", resultCount)
                .put("lookupSeconds", lookupSeconds)
                .put("radius", radius)
                .put("category", category)
                .put("world", world)
                .put("x", x)
                .put("y", y)
                .put("z", z)
                .put("apiVersion", apiVersion)
                .toJson();
    }
}
