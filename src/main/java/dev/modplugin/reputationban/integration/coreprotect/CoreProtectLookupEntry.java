package dev.modplugin.reputationban.integration.coreprotect;

public record CoreProtectLookupEntry(
        String action,
        String player,
        int x,
        int y,
        int z,
        String type
) {
}
