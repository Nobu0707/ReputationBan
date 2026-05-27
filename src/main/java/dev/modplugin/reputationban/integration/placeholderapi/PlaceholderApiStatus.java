package dev.modplugin.reputationban.integration.placeholderapi;

public record PlaceholderApiStatus(
        boolean configuredEnabled,
        boolean pluginPresent,
        boolean apiAvailable,
        boolean active,
        String identifier,
        int cacheRefreshSeconds,
        boolean registered,
        String message
) {
    public String availabilityLabel() {
        if (!configuredEnabled) {
            return "disabled";
        }
        return active ? "active" : "unavailable";
    }
}
