package dev.modplugin.reputationban.integration.coreprotect;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.integration.ExternalIntegrationType;
import dev.modplugin.reputationban.integration.IntegrationStatus;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class CoreProtectIntegration {
    private final JavaPlugin plugin;
    private final Logger logger;

    public CoreProtectIntegration(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public IntegrationStatus status(PluginConfig config) {
        PluginConfig.CoreProtectIntegrationConfig coreProtectConfig = config.coreProtectIntegration();
        if (!coreProtectConfig.enabled()) {
            return IntegrationStatus.disabled(ExternalIntegrationType.COREPROTECT);
        }
        Plugin coreProtectPlugin = plugin.getServer().getPluginManager().getPlugin("CoreProtect");
        boolean pluginPresent = coreProtectPlugin != null;
        Optional<CoreProtectAPI> api = api(coreProtectConfig.minimumApiVersion());
        String apiVersion = api.map(value -> Integer.toString(value.APIVersion())).orElse("");
        return new IntegrationStatus(
                ExternalIntegrationType.COREPROTECT,
                true,
                pluginPresent,
                api.isPresent(),
                apiVersion,
                pluginPresent && api.isPresent(),
                pluginPresent ? (api.isPresent() ? "active" : "api unavailable") : "plugin not found"
        );
    }

    public Optional<CoreProtectAPI> api(int minimumApiVersion) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("CoreProtect plugin lookup must run on the main thread");
        }
        try {
            Plugin candidate = Bukkit.getPluginManager().getPlugin("CoreProtect");
            if (!(candidate instanceof CoreProtect coreProtect)) {
                return Optional.empty();
            }
            CoreProtectAPI api = coreProtect.getAPI();
            if (api == null || !api.isEnabled() || api.APIVersion() < minimumApiVersion) {
                return Optional.empty();
            }
            return Optional.of(api);
        } catch (NoClassDefFoundError error) {
            return Optional.empty();
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "CoreProtect integration unavailable: " + exception.getMessage());
            return Optional.empty();
        }
    }
}
