package dev.modplugin.reputationban.notification;

import dev.modplugin.reputationban.ReputationBanPlugin;
import dev.modplugin.reputationban.config.PluginConfig;
import dev.modplugin.reputationban.integration.discordsrv.DiscordSrvNotificationService;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class NotificationService {
    private final ReputationBanPlugin plugin;
    private final Supplier<PluginConfig> configSupplier;
    private final DiscordWebhookClient discordWebhookClient;
    private final DiscordSrvNotificationService discordSrvNotificationService;

    public NotificationService(ReputationBanPlugin plugin, Supplier<PluginConfig> configSupplier) {
        this.plugin = plugin;
        this.configSupplier = configSupplier;
        this.discordWebhookClient = new DiscordWebhookClient(plugin.getLogger());
        this.discordSrvNotificationService = new DiscordSrvNotificationService(plugin);
    }

    public void notifyStaff(String message) {
        notifyStaff(null, message, message);
    }

    public void notifyStaff(NotificationEventType type, String message) {
        notifyStaff(type, message, message);
    }

    public void notifyStaff(NotificationEventType type, String staffMessage, String discordContent) {
        PluginConfig config = configSupplier.get();
        if (config.notifyConsole()) {
            plugin.getLogger().info(staffMessage);
        }
        if (config.notifyInGameStaff()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission(config.staffPermission())) {
                    player.sendMessage(ReputationBanPlugin.PREFIX + staffMessage);
                }
            }
        }
        notifyDiscord(type, discordContent);
    }

    public void notifyDiscord(NotificationEventType type, String discordContent) {
        if (type == null) {
            return;
        }
        PluginConfig config = configSupplier.get();
        discordWebhookClient.send(type, config.discordWebhookConfig(), discordContent);
        discordSrvNotificationService.send(type, config, discordContent);
    }
}
