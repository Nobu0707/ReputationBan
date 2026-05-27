package dev.modplugin.reputationban.integration.coreprotect;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.integration.ExternalIntegrationType;
import dev.modplugin.reputationban.integration.IntegrationStatus;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoreProtectIntegration {
    private final Logger logger;
    private final CoreProtectReflectionAdapter adapter;

    public CoreProtectIntegration(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
        this.adapter = new CoreProtectReflectionAdapter(plugin);
    }

    public IntegrationStatus status(PluginConfig config) {
        PluginConfig.CoreProtectIntegrationConfig coreProtectConfig = config.coreProtectIntegration();
        boolean pluginPresent = adapter.pluginPresent();
        Optional<Integer> apiVersionLookup = pluginPresent
                ? apiVersion(coreProtectConfig.minimumApiVersion())
                : Optional.empty();
        String apiVersion = apiVersionLookup.map(Object::toString).orElse("");
        if (!coreProtectConfig.enabled()) {
            return new IntegrationStatus(
                    ExternalIntegrationType.COREPROTECT,
                    false,
                    pluginPresent,
                    apiVersionLookup.isPresent(),
                    apiVersion,
                    false,
                    "disabled"
            );
        }
        return new IntegrationStatus(
                ExternalIntegrationType.COREPROTECT,
                true,
                pluginPresent,
                apiVersionLookup.isPresent(),
                apiVersion,
                pluginPresent && apiVersionLookup.isPresent(),
                pluginPresent ? (apiVersionLookup.isPresent() ? "active" : "api unavailable") : "plugin not found"
        );
    }

    public Optional<Integer> apiVersion(int minimumApiVersion) {
        try {
            return adapter.apiVersion(minimumApiVersion);
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "CoreProtect integration unavailable: " + exception.getMessage());
            return Optional.empty();
        }
    }

    public Optional<CoreProtectReflectionAdapter.ApiHandle> api(int minimumApiVersion) {
        try {
            return adapter.api(minimumApiVersion);
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "CoreProtect integration unavailable: " + exception.getMessage());
            return Optional.empty();
        }
    }

    public CoreProtectLookupResult performLookup(
            CoreProtectReflectionAdapter.ApiHandle api,
            int lookupSeconds,
            List<String> players,
            List<Integer> actions,
            int radius,
            Location location,
            int maxResults
    ) {
        return adapter.performLookup(api, lookupSeconds, players, actions, radius, location, maxResults);
    }
}
