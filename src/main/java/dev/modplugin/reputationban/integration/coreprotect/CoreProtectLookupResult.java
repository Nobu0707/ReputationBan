package dev.modplugin.reputationban.integration.coreprotect;

import java.util.List;

public record CoreProtectLookupResult(
        int apiVersion,
        int totalResults,
        List<CoreProtectLookupEntry> entries
) {
    public static CoreProtectLookupResult empty(int apiVersion) {
        return new CoreProtectLookupResult(apiVersion, 0, List.of());
    }
}
