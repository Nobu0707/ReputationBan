package dev.modplugin.reputationban.integration;

public record IntegrationStatus(
        ExternalIntegrationType type,
        boolean configuredEnabled,
        boolean pluginPresent,
        boolean apiAvailable,
        String apiVersion,
        boolean active,
        String message
) {
    public static IntegrationStatus disabled(ExternalIntegrationType type) {
        return new IntegrationStatus(type, false, false, false, "", false, "disabled");
    }

    public String availabilityLabel() {
        if (!configuredEnabled) {
            return "disabled";
        }
        return active ? "active" : "unavailable";
    }

    public String compactLine() {
        return type.displayName() + ": active=" + active
                + ", pluginPresent=" + pluginPresent
                + ", apiAvailable=" + apiAvailable;
    }

    public String startupLine() {
        return type.displayName() + " integration: "
                + (active ? "active" : availabilityLabel() + " (" + message + ")");
    }
}
