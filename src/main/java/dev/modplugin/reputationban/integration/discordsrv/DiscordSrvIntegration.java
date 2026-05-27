package dev.modplugin.reputationban.integration.discordsrv;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.integration.ExternalIntegrationType;
import dev.modplugin.reputationban.integration.IntegrationStatus;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class DiscordSrvIntegration {
    private final Logger logger;
    private final DiscordSrvReflectionAdapter adapter;

    public DiscordSrvIntegration(JavaPlugin plugin) {
        this(plugin.getLogger(), new DiscordSrvReflectionAdapter(plugin));
    }

    DiscordSrvIntegration(Logger logger, DiscordSrvReflectionAdapter adapter) {
        this.logger = logger;
        this.adapter = adapter;
    }

    public IntegrationStatus status(PluginConfig config) {
        DiscordSrvStatus detail = detail(config);
        return new IntegrationStatus(
                ExternalIntegrationType.DISCORDSRV,
                detail.configuredEnabled(),
                detail.pluginPresent(),
                detail.apiAvailable(),
                "",
                detail.active(),
                message(detail)
        );
    }

    public DiscordSrvStatus detail(PluginConfig config) {
        PluginConfig.DiscordSrvIntegrationConfig discordConfig = config.discordSrvIntegration();
        boolean pluginPresent = adapter.pluginPresent();
        boolean apiAvailable = pluginPresent && apiAvailable();
        boolean active = discordConfig.enabled() && pluginPresent && apiAvailable;
        return new DiscordSrvStatus(
                discordConfig.enabled(),
                pluginPresent,
                apiAvailable,
                active,
                discordConfig.accountLinkContextEnabled(),
                discordConfig.includeDiscordIds(),
                apiAvailable && adapter.accountLinkAvailable(),
                discordConfig.notificationsEnabled(),
                discordConfig.notificationChannel()
        );
    }

    public Optional<String> discordId(UUID playerUuid) {
        try {
            return adapter.discordId(playerUuid);
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "DiscordSRV account link lookup failed: " + exception.getMessage());
            return Optional.empty();
        }
    }

    public boolean sendMessage(String channelName, String message) {
        try {
            return adapter.sendMessage(channelName, message);
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "DiscordSRV notification failed: " + exception.getMessage());
            return false;
        }
    }

    private boolean apiAvailable() {
        try {
            return adapter.apiAvailable();
        } catch (RuntimeException exception) {
            logger.log(Level.WARNING, "DiscordSRV integration unavailable: " + exception.getMessage());
            return false;
        }
    }

    private static String message(DiscordSrvStatus detail) {
        if (!detail.configuredEnabled()) {
            return "disabled";
        }
        if (!detail.pluginPresent()) {
            return "DiscordSRV not found";
        }
        return detail.apiAvailable() ? "active" : "api unavailable";
    }
}
