package dev.modplugin.reputationban.integration;

public enum ExternalIntegrationType {
    LUCKPERMS("LuckPerms"),
    COREPROTECT("CoreProtect"),
    WORLDGUARD("WorldGuard"),
    GRIEFPREVENTION("GriefPrevention");

    private final String displayName;

    ExternalIntegrationType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
