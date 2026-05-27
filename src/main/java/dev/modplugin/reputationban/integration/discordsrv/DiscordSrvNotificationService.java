package dev.modplugin.reputationban.integration.discordsrv;

import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.notification.NotificationEventType;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class DiscordSrvNotificationService {
    private final JavaPlugin plugin;
    private final DiscordSrvIntegration integration;

    public DiscordSrvNotificationService(JavaPlugin plugin) {
        this(plugin, new DiscordSrvIntegration(plugin));
    }

    DiscordSrvNotificationService(JavaPlugin plugin, DiscordSrvIntegration integration) {
        this.plugin = plugin;
        this.integration = integration;
    }

    public void send(NotificationEventType type, PluginConfig config, String message) {
        PluginConfig.DiscordSrvIntegrationConfig discordConfig = config.discordSrvIntegration();
        DiscordSrvStatus status = integration.detail(config);
        if (!DiscordSrvPolicy.shouldNotify(
                discordConfig.notificationsEnabled(),
                status.active(),
                discordConfig.notificationEvents(),
                type
        )) {
            return;
        }
        String sanitized = DiscordSrvPolicy.sanitizeMessage(message, discordConfig.notificationIncludeReasons());
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            boolean sent = integration.sendMessage(discordConfig.notificationChannel(), sanitized);
            if (!sent) {
                plugin.getLogger().log(Level.WARNING, "DiscordSRV notification was skipped or failed for event " + type.configKey());
            }
        });
    }
}
