package dev.modplugin.reputationban.integration.luckperms;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.integration.ExternalIntegrationType;
import dev.modplugin.reputationban.integration.IntegrationStatus;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class LuckPermsIntegration {
    private final Logger logger;
    private final LuckPermsReflectionAdapter adapter;

    public LuckPermsIntegration(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
        this.adapter = new LuckPermsReflectionAdapter(plugin);
    }

    public IntegrationStatus status(PluginConfig config) {
        PluginConfig.LuckPermsIntegrationConfig luckPermsConfig = config.luckPermsIntegration();
        boolean pluginPresent = adapter.pluginPresent();
        boolean apiAvailable = pluginPresent && adapter.apiAvailable();
        if (!luckPermsConfig.enabled()) {
            return new IntegrationStatus(
                    ExternalIntegrationType.LUCKPERMS,
                    false,
                    pluginPresent,
                    apiAvailable,
                    "",
                    false,
                    "disabled"
            );
        }
        return new IntegrationStatus(
                ExternalIntegrationType.LUCKPERMS,
                true,
                pluginPresent,
                apiAvailable,
                "",
                pluginPresent && apiAvailable,
                pluginPresent ? (apiAvailable ? "active" : "api not registered") : "plugin not found"
        );
    }

    public LuckPermsTrustService trustFor(UUID playerUuid, PluginConfig config) {
        PluginConfig.LuckPermsIntegrationConfig luckPermsConfig = config.luckPermsIntegration();
        if (!luckPermsConfig.enabled()) {
            return LuckPermsTrustService.unavailable(luckPermsConfig.defaultWeight());
        }
        try {
            Optional<String> primaryGroupLookup = adapter.primaryGroup(playerUuid);
            if (primaryGroupLookup.isEmpty()) {
                return LuckPermsTrustService.unavailable(luckPermsConfig.defaultWeight());
            }
            String primaryGroup = primaryGroupLookup.get();
            double weight = LuckPermsTrustPolicy.weightForGroup(
                    luckPermsConfig.useGroupWeight(),
                    luckPermsConfig.defaultWeight(),
                    luckPermsConfig.groupWeights(),
                    primaryGroup
            );
            boolean bypass = LuckPermsTrustPolicy.isBypassGroup(luckPermsConfig.bypassGroups(), primaryGroup);
            return new LuckPermsTrustService(primaryGroup, weight, bypass);
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "LuckPerms integration unavailable: " + exception.getMessage());
            return LuckPermsTrustService.unavailable(luckPermsConfig.defaultWeight());
        }
    }
}
