package dev.modplugin.reputationban.integration.coreprotect;

import dev.modplugin.reputationban.util.AuditMetadata;
import java.util.List;

public record CoreProtectEvidenceSummary(
        int resultCount,
        String summary,
        List<String> lines,
        String metadata
) {
    public static CoreProtectEvidenceSummary empty(String world, int x, int y, int z, int radius, int lookupSeconds, String category) {
        return new CoreProtectEvidenceSummary(
                0,
                "0 result(s) near " + world + " " + x + " " + y + " " + z + " within " + radius + " blocks",
                List.of(),
                metadata(0, lookupSeconds, radius, category)
        );
    }

    public static String metadata(int resultCount, int lookupSeconds, int radius, String category) {
        return AuditMetadata.create()
                .put("resultCount", resultCount)
                .put("lookupSeconds", lookupSeconds)
                .put("radius", radius)
                .put("category", category)
                .toJson();
    }
}
