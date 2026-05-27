package dev.modplugin.reputationban.integration.worldguard;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.integration.ExternalIntegrationType;
import dev.modplugin.reputationban.integration.IntegrationStatus;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldGuardIntegration {
    private final Logger logger;
    private final WorldGuardReflectionAdapter adapter;

    public WorldGuardIntegration(JavaPlugin plugin) {
        this(plugin.getLogger(), new WorldGuardReflectionAdapter(plugin));
    }

    WorldGuardIntegration(Logger logger, WorldGuardReflectionAdapter adapter) {
        this.logger = logger;
        this.adapter = adapter;
    }

    public IntegrationStatus status(PluginConfig config) {
        WorldGuardStatus detail = detail(config);
        return new IntegrationStatus(
                ExternalIntegrationType.WORLDGUARD,
                detail.configuredEnabled(),
                detail.worldGuardPresent(),
                detail.apiAvailable(),
                "",
                detail.active(),
                message(detail)
        );
    }

    public WorldGuardStatus detail(PluginConfig config) {
        PluginConfig.WorldGuardIntegrationConfig worldGuardConfig = config.worldGuardIntegration();
        boolean worldGuardPresent = adapter.worldGuardPresent();
        boolean worldEditPresent = adapter.worldEditPresent();
        boolean apiAvailable = worldGuardPresent && worldEditPresent && apiAvailable();
        boolean active = worldGuardConfig.enabled() && worldGuardPresent && worldEditPresent && apiAvailable;
        return new WorldGuardStatus(
                worldGuardConfig.enabled(),
                worldEditPresent,
                worldGuardPresent,
                apiAvailable,
                active,
                worldGuardConfig.reportContextEnabled(),
                worldGuardConfig.maxRegions()
        );
    }

    public Optional<WorldGuardRegionSummary> regionSummary(Location location, PluginConfig.WorldGuardIntegrationConfig config) {
        try {
            return adapter.regionSummary(
                    location,
                    config.maxRegions(),
                    config.includeRegionOwners(),
                    config.includeRegionMembers(),
                    config.includeFlags()
            );
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "WorldGuard region context failed: " + exception.getMessage());
            return Optional.empty();
        }
    }

    private boolean apiAvailable() {
        try {
            return adapter.apiAvailable();
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "WorldGuard integration unavailable: " + exception.getMessage());
            return false;
        }
    }

    private static String message(WorldGuardStatus detail) {
        if (!detail.configuredEnabled()) {
            return "disabled";
        }
        if (!detail.worldEditPresent()) {
            return "WorldEdit not found";
        }
        if (!detail.worldGuardPresent()) {
            return "WorldGuard not found";
        }
        return detail.apiAvailable() ? "active" : "api unavailable";
    }
}
