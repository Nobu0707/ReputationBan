package dev.modplugin.reputationban.integration.discordsrv;

public record DiscordSrvStatus(
        boolean configuredEnabled,
        boolean pluginPresent,
        boolean apiAvailable,
        boolean active,
        boolean accountLinkContextEnabled,
        boolean includeDiscordIds,
        boolean accountLinkAvailable,
        boolean notificationsEnabled,
        String notificationChannel
) {
}
