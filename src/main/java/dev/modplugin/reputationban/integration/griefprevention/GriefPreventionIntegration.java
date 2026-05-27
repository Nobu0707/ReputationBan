package dev.modplugin.reputationban.integration.griefprevention;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.integration.ExternalIntegrationType;
import dev.modplugin.reputationban.integration.IntegrationStatus;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public final class GriefPreventionIntegration {
    private final Logger logger;
    private final GriefPreventionReflectionAdapter adapter;

    public GriefPreventionIntegration(JavaPlugin plugin) {
        this(plugin.getLogger(), new GriefPreventionReflectionAdapter(plugin));
    }

    GriefPreventionIntegration(Logger logger, GriefPreventionReflectionAdapter adapter) {
        this.logger = logger;
        this.adapter = adapter;
    }

    public IntegrationStatus status(PluginConfig config) {
        GriefPreventionStatus detail = detail(config);
        return new IntegrationStatus(
                ExternalIntegrationType.GRIEFPREVENTION,
                detail.configuredEnabled(),
                detail.pluginPresent(),
                detail.apiAvailable(),
                "",
                detail.active(),
                message(detail)
        );
    }

    public GriefPreventionStatus detail(PluginConfig config) {
        PluginConfig.GriefPreventionIntegrationConfig griefConfig = config.griefPreventionIntegration();
        boolean pluginPresent = adapter.pluginPresent();
        boolean apiAvailable = pluginPresent && apiAvailable();
        boolean active = griefConfig.enabled() && pluginPresent && apiAvailable;
        return new GriefPreventionStatus(
                griefConfig.enabled(),
                pluginPresent,
                apiAvailable,
                active,
                griefConfig.reportContextEnabled(),
                griefConfig.includeClaimOwner(),
                griefConfig.includeTrustCounts(),
                griefConfig.includeBoundaries()
        );
    }

    public Optional<GriefPreventionClaimSummary> claimSummary(
            Location location,
            PluginConfig.GriefPreventionIntegrationConfig config
    ) {
        try {
            return adapter.claimSummary(
                    location,
                    config.includeClaimOwner(),
                    config.includeTrustCounts(),
                    config.includeBoundaries()
            );
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "GriefPrevention claim context failed: " + exception.getMessage());
            return Optional.empty();
        }
    }

    private boolean apiAvailable() {
        try {
            return adapter.apiAvailable();
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "GriefPrevention integration unavailable: " + exception.getMessage());
            return false;
        }
    }

    private static String message(GriefPreventionStatus detail) {
        if (!detail.configuredEnabled()) {
            return "disabled";
        }
        if (!detail.pluginPresent()) {
            return "GriefPrevention not found";
        }
        return detail.apiAvailable() ? "active" : "api unavailable";
    }
}
