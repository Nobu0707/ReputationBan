package dev.modplugin.reputationban.integration.luckperms;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.integration.ExternalIntegrationType;
import dev.modplugin.reputationban.integration.IntegrationStatus;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class LuckPermsIntegration {
    private final JavaPlugin plugin;
    private final Logger logger;

    public LuckPermsIntegration(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public IntegrationStatus status(PluginConfig config) {
        PluginConfig.LuckPermsIntegrationConfig luckPermsConfig = config.luckPermsIntegration();
        if (!luckPermsConfig.enabled()) {
            return IntegrationStatus.disabled(ExternalIntegrationType.LUCKPERMS);
        }
        boolean pluginPresent = plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null;
        Optional<LuckPerms> api = api();
        return new IntegrationStatus(
                ExternalIntegrationType.LUCKPERMS,
                true,
                pluginPresent,
                api.isPresent(),
                "",
                pluginPresent && api.isPresent(),
                pluginPresent ? (api.isPresent() ? "active" : "api not registered") : "plugin not found"
        );
    }

    public LuckPermsTrustService trustFor(UUID playerUuid, PluginConfig config) {
        PluginConfig.LuckPermsIntegrationConfig luckPermsConfig = config.luckPermsIntegration();
        if (!luckPermsConfig.enabled()) {
            return LuckPermsTrustService.unavailable(luckPermsConfig.defaultWeight());
        }
        try {
            Optional<LuckPerms> api = api();
            if (api.isEmpty()) {
                return LuckPermsTrustService.unavailable(luckPermsConfig.defaultWeight());
            }
            User user = api.get().getUserManager().getUser(playerUuid);
            if (user == null) {
                return LuckPermsTrustService.unavailable(luckPermsConfig.defaultWeight());
            }
            String primaryGroup = user.getPrimaryGroup();
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

    private Optional<LuckPerms> api() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("LuckPerms service lookup must run on the main thread");
        }
        try {
            RegisteredServiceProvider<LuckPerms> provider =
                    Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            return provider == null ? Optional.empty() : Optional.of(provider.getProvider());
        } catch (NoClassDefFoundError error) {
            return Optional.empty();
        }
    }
}
